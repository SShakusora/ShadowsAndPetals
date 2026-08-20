package com.sshakusora.shadowsandpetals.item.hammer;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.RawConcreteBlock;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import com.sshakusora.shadowsandpetals.registries.TriggerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jspecify.annotations.Nullable;

import java.util.*;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public class HammerItem extends Item {
    public static final int USE_DURATION = 30;
    private static final int MAX_USE_DURATION = 72000;
    private static final int BASE_USE_DURATION = 15;
    private static final int TICKS_PER_PART = 10;
    private static final int LOOK_REFRESH_INTERVAL_TICKS = 5;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final String TARGET_POS_KEY = "hammer_target";
    private static final String ROOT_KEY = "hammer_root";
    private static final String TEMPLATE_KEY = "hammer_template_idx";
    private static final String FACING_KEY = "hammer_facing";
    private static final String START_TICK_KEY = "hammer_start_tick";

    private static final List<RockeryTemplate> ROCKERY_TEMPLATES = new ArrayList<>();
    private static final Map<UUID, HammerSession> HAMMER_SESSIONS = new HashMap<>();
    private static final Set<Integer> ACTIVE_BREAKER_IDS = new HashSet<>();
    private static int nextBreakerId = -1;

    /**
     * Registers a rockery multi-block for hammer placement.
     * Called automatically by {@code BlockRegistry.registerRockery()}
     * during static initialization.
     */
    public static void registerRockery(DeferredBlock<RockeryBlock> block, RockeryDimensions dims) {
        ROCKERY_TEMPLATES.add(new RockeryTemplate(block, dims));
        ROCKERY_TEMPLATES.sort(Comparator.comparingInt(RockeryTemplate::partCount).reversed());
    }

    public static List<RockeryTemplate> rockeryTemplates() {
        return List.copyOf(ROCKERY_TEMPLATES);
    }

    public HammerItem(Properties properties) {
        super(properties);
    }

    public static float getHammeringProgress(Player player, Level level, BlockPos pos) {
        HammerSession session = HAMMER_SESSIONS.get(player.getUUID());
        if (session == null || !session.dimension().equals(level.dimension())) {
            return -1.0F;
        }

        PlacementData placement = session.placement();
        RockeryTemplate template = getTemplate(placement);
        if (template == null) {
            return -1.0F;
        }

        Direction facing = DIRECTIONS[placement.facingOrdinal()];
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos partPos = placement.root().offset(template.dimensions().worldOffset(part, facing));
            if (partPos.equals(pos)) {
                return session.progress();
            }
        }
        return -1.0F;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        stopHammeringForLifecycleEvent(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        stopHammeringForLifecycleEvent(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        stopHammeringForLifecycleEvent(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        stopHammeringForLifecycleEvent(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HAMMER_SESSIONS.clear();
        ACTIVE_BREAKER_IDS.clear();
        nextBreakerId = -1;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return HammerUseAnimationEnumExtensions.getHammerAndChisel();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    public static int getEffectiveUseDuration(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(TEMPLATE_KEY)) {
            var tag = data.copyTag();
            int templateIdx = tag.getInt(TEMPLATE_KEY).orElse(-1);
            if (templateIdx >= 0 && templateIdx < ROCKERY_TEMPLATES.size()) {
                return getEffectiveUseDuration(ROCKERY_TEMPLATES.get(templateIdx).dimensions());
            }
        }
        return USE_DURATION;
    }

    public static int getEffectiveUseDuration(RockeryDimensions dimensions) {
        return BASE_USE_DURATION + dimensions.partCount() * TICKS_PER_PART;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.getBlock() instanceof RawConcreteBlock
                && RawConcreteBlock.isHolePosition(clickedPos, context.getClickedFace())) {
            if (!level.isClientSide()
                    && level.setBlock(
                    clickedPos,
                    clickedState.cycle(RawConcreteBlock.DENSE),
                    Block.UPDATE_CLIENTS)) {
                level.playSound(
                        null,
                        clickedPos,
                        SoundEvents.STONE_HIT,
                        SoundSource.BLOCKS,
                        0.7F,
                        clickedState.getValue(RawConcreteBlock.DENSE) ? 0.65F : 0.8F);
            }
            return InteractionResult.SUCCESS;
        }

        if (!player.getOffhandItem().is(ItemRegistry.CHISEL.get())) {
            return InteractionResult.PASS;
        }

        if (!level.getBlockState(clickedPos).is(Blocks.STONE)) {
            return InteractionResult.PASS;
        }

        RockeryPlacement placement = findPlacement(level, clickedPos, player.getDirection().getOpposite());
        if (placement == null) {
            return InteractionResult.PASS;
        }

        int templateIdx = templateIndex(placement);
        storePlacementData(context.getItemInHand(), clickedPos, templateIdx, placement.root(), placement.facing(), level.getGameTime());
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            PlacementData data = readPlacementData(context.getItemInHand());
            if (data != null) {
                beginHammerSession(serverPlayer, serverLevel, data);
            }
        }

        // Begin the hammering animation
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return;
        PlacementData data = readPlacementData(stack);
        if (data == null) {
            cancelHammering(player, stack);
            return;
        }
        if (!player.getOffhandItem().is(ItemRegistry.CHISEL.get())) {
            cancelHammering(player, stack);
            return;
        }

        data = refreshPlacementFromLook(player, serverLevel, stack, data);
        if (data == null) {
            cancelHammering(player, stack);
            return;
        }
        BlockPos targetPos = data.clickedPos();

        int usedTicks = getUsedTicks(serverLevel, data);
        if (usedTicks >= getEffectiveUseDuration(data)) {
            completeHammering(stack, serverLevel, player);
            player.releaseUsingItem();
            return;
        }

        // Update destruction crack overlay on all rockery parts
        updateCrackProgress(player, data, serverLevel, usedTicks);

        boolean hammerImpact = AnimationTiming.isImpactTick(
                player.getTicksUsingItem());
        if (hammerImpact) {
            serverLevel.playSound(null, targetPos,
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS,
                    0.55F, 1.45F + level.getRandom().nextFloat() * 0.25F);
        }

        if (hammerImpact) {
            serverLevel.playSound(null, targetPos,
                    SoundEvents.STONE_HIT, SoundSource.PLAYERS,
                    0.7F, 0.8F + level.getRandom().nextFloat() * 0.3F);

            BlockParticleOption stoneDust = new BlockParticleOption(
                    ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            serverLevel.sendParticles(stoneDust,
                    targetPos.getX() + 0.5D,
                    targetPos.getY() + 0.6D,
                    targetPos.getZ() + 0.5D,
                    5, 0.2D, 0.1D, 0.2D, 0.05D);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player)) return stack;
        if (level.isClientSide()) return stack;

        completeHammering(stack, level, (Player) entity);
        return stack;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            endHammerSession(player);
        }
        clearPlacementData(stack);
        return false;
    }

    private static void completeHammering(ItemStack stack, Level level, Player player) {
        PlacementData data = readPlacementData(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            endHammerSession(serverPlayer);
        }
        clearPlacementData(stack);
        if (data == null) return;

        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = DIRECTIONS[data.facingOrdinal()];

        // Validate all parts are still STONE (world may have changed during animation)
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos pos = data.root().offset(template.dimensions().worldOffset(part, facing));
            if (!level.getBlockState(pos).is(Blocks.STONE)) {
                return;
            }
        }

        // Reconstruct placement and apply
        RockeryPlacement placement = new RockeryPlacement(
                template.block(), template.dimensions(), data.root(), facing);
        placement.place(level);
        if (level instanceof ServerLevel serverLevel) {
            placement.playEffects(serverLevel, data.clickedPos());
            if (player instanceof ServerPlayer serverPlayer) {
                TriggerRegistry.ROCKERY_CARVED.get().trigger(serverPlayer);
            }
        }
    }

    private static void cancelHammering(ServerPlayer player, ItemStack stack) {
        endHammerSession(player);
        clearPlacementData(stack);
        player.releaseUsingItem();
    }

    private static int getUsedTicks(ServerLevel level, PlacementData data) {
        long elapsed = level.getGameTime() - data.startTick() + 1L;
        if (elapsed <= 0L) {
            return 1;
        }
        return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
    }

    private static void updateCrackProgress(Player player, PlacementData data, ServerLevel level, int usedTicks) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        HammerSession session = getOrCreateHammerSession(serverPlayer, level, data);
        int duration = getEffectiveUseDuration(data);
        int progress = Math.clamp(usedTicks * 10L / Math.max(1, duration), 0, 9);
        float normalizedProgress = Math.clamp((float) usedTicks / Math.max(1, duration), 0.0F, 1.0F);
        session.setProgress(normalizedProgress);
        if (session.lastSentCrackProgress() == progress) {
            return;
        }

        session.setLastSentCrackProgress(progress);
        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = DIRECTIONS[data.facingOrdinal()];
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos pos = data.root().offset(template.dimensions().worldOffset(part, facing));
            // Current templates contain at most four parts. If that grows substantially,
            // batch root/template/facing/progress and reconstruct positions in a client payload handler.
            level.destroyBlockProgress(session.breakerIds()[part], pos, progress);
        }
    }

    private static int getEffectiveUseDuration(PlacementData data) {
        if (data.templateIndex() >= 0 && data.templateIndex() < ROCKERY_TEMPLATES.size()) {
            return getEffectiveUseDuration(ROCKERY_TEMPLATES.get(data.templateIndex()).dimensions());
        }
        return USE_DURATION;
    }

    private static HammerSession getOrCreateHammerSession(ServerPlayer player, ServerLevel level, PlacementData data) {
        HammerSession session = HAMMER_SESSIONS.get(player.getUUID());
        if (session == null || !session.dimension().equals(level.dimension()) || !session.placement().equals(data)) {
            beginHammerSession(player, level, data);
            session = Objects.requireNonNull(HAMMER_SESSIONS.get(player.getUUID()));
        }
        return session;
    }

    private static void beginHammerSession(ServerPlayer player, ServerLevel level, PlacementData data) {
        endHammerSession(player);
        RockeryTemplate template = getTemplate(data);
        if (template == null) {
            return;
        }

        int[] breakerIds = new int[template.dimensions().partCount()];
        for (int part = 0; part < breakerIds.length; part++) {
            breakerIds[part] = allocateBreakerId();
        }
        HAMMER_SESSIONS.put(player.getUUID(), new HammerSession(level.dimension(), data, breakerIds));
    }

    private static void endHammerSession(ServerPlayer player) {
        HammerSession session = HAMMER_SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }

        ServerLevel level = player.level().getServer().getLevel(session.dimension());
        RockeryTemplate template = getTemplate(session.placement());
        if (level != null && template != null) {
            Direction facing = DIRECTIONS[session.placement().facingOrdinal()];
            for (int part = 0; part < session.breakerIds().length; part++) {
                BlockPos pos = session.placement().root().offset(template.dimensions().worldOffset(part, facing));
                level.destroyBlockProgress(session.breakerIds()[part], pos, -1);
            }
        }
        for (int breakerId : session.breakerIds()) {
            ACTIVE_BREAKER_IDS.remove(breakerId);
        }
    }

    private static int allocateBreakerId() {
        // Vanilla mining uses positive entity IDs. A separately allocated negative namespace
        // prevents hammer overlays from replacing or clearing another player's mining overlay.
        int candidate = nextBreakerId;
        while (ACTIVE_BREAKER_IDS.contains(candidate)) {
            candidate = decrementBreakerId(candidate);
        }
        ACTIVE_BREAKER_IDS.add(candidate);
        nextBreakerId = decrementBreakerId(candidate);
        return candidate;
    }

    private static int decrementBreakerId(int breakerId) {
        return breakerId == Integer.MIN_VALUE ? -1 : breakerId - 1;
    }

    private static void stopHammeringForLifecycleEvent(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        ItemStack useItem = player.getUseItem();
        boolean isUsingHammer = useItem.getItem() instanceof HammerItem;
        endHammerSession(player);
        if (isUsingHammer && hasPlacementData(useItem)) {
            clearPlacementData(useItem);
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof HammerItem && hasPlacementData(mainHandItem)) {
            clearPlacementData(mainHandItem);
        }
        if (player.isUsingItem() && isUsingHammer) {
            player.releaseUsingItem();
        }
    }

    private static @Nullable RockeryTemplate getTemplate(PlacementData data) {
        return data.templateIndex() >= 0 && data.templateIndex() < ROCKERY_TEMPLATES.size()
                && data.facingOrdinal() >= 0 && data.facingOrdinal() < DIRECTIONS.length
                ? ROCKERY_TEMPLATES.get(data.templateIndex())
                : null;
    }

    private static @Nullable PlacementData refreshPlacementFromLook(ServerPlayer player, ServerLevel level, ItemStack stack, PlacementData data) {
        BlockPos lookedPos = getLookedAtStone(player, level);
        if (lookedPos == null) {
            return null;
        }

        if (!data.clickedPos().equals(lookedPos)) {
            RockeryPlacement placement = findPlacement(level, lookedPos, player.getDirection().getOpposite());
            return replaceHammerPlacement(player, level, stack, lookedPos, placement);
        }

        if (getUsedTicks(level, data) % LOOK_REFRESH_INTERVAL_TICKS != 0) {
            return data;
        }

        if (placementStillMatches(level, data)) {
            return data;
        }

        RockeryPlacement placement = findPlacement(level, lookedPos, player.getDirection().getOpposite());
        return replaceHammerPlacement(player, level, stack, lookedPos, placement);
    }

    private static @Nullable PlacementData replaceHammerPlacement(
            ServerPlayer player,
            ServerLevel level,
            ItemStack stack,
            BlockPos lookedPos,
            @Nullable RockeryPlacement placement) {
        endHammerSession(player);
        if (placement == null) {
            clearPlacementData(stack);
            return null;
        }

        int templateIdx = templateIndex(placement);
        storePlacementData(stack, lookedPos, templateIdx, placement.root(), placement.facing(), level.getGameTime());
        PlacementData refreshed = readPlacementData(stack);
        if (refreshed != null) {
            beginHammerSession(player, level, refreshed);
        }
        return refreshed;
    }

    private static @Nullable BlockPos getLookedAtStone(Player player, Level level) {
        HitResult hitResult = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)
                || blockHitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        return level.getBlockState(pos).is(Blocks.STONE) ? pos : null;
    }

    private static boolean placementStillMatches(Level level, PlacementData data) {
        if (data.templateIndex() < 0 || data.templateIndex() >= ROCKERY_TEMPLATES.size()
                || data.facingOrdinal() < 0 || data.facingOrdinal() >= DIRECTIONS.length) {
            return false;
        }

        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = DIRECTIONS[data.facingOrdinal()];
        return matches(level, data.root(), template.dimensions(), facing);
    }

    private static @Nullable RockeryPlacement findPlacement(Level level, BlockPos clickedPos, Direction preferredFacing) {
        for (RockeryTemplate template : ROCKERY_TEMPLATES) {
            RockeryPlacement preferred = findPlacement(level, clickedPos, template, preferredFacing);
            if (preferred != null) {
                return preferred;
            }

            for (Direction facing : new Direction[] {
                    preferredFacing.getClockWise(),
                    preferredFacing.getCounterClockWise(),
                    preferredFacing.getOpposite()
            }) {
                RockeryPlacement placement = findPlacement(level, clickedPos, template, facing);
                if (placement != null) {
                    return placement;
                }
            }
        }
        return null;
    }

    private static @Nullable RockeryPlacement findPlacement(Level level, BlockPos clickedPos, RockeryTemplate template, Direction facing) {
        for (int clickedPart = 0; clickedPart < template.dimensions().partCount(); clickedPart++) {
            Vec3i clickedOffset = template.dimensions().worldOffset(clickedPart, facing);
            BlockPos root = clickedPos.subtract(clickedOffset);
            if (matches(level, root, template.dimensions(), facing)) {
                return new RockeryPlacement(template.block(), template.dimensions(), root, facing);
            }
        }
        return null;
    }

    private static boolean matches(Level level, BlockPos root, RockeryDimensions dimensions, Direction facing) {
        for (int part = 0; part < dimensions.partCount(); part++) {
            BlockPos pos = root.offset(dimensions.worldOffset(part, facing));
            if (!level.getBlockState(pos).is(Blocks.STONE)) {
                return false;
            }
        }
        return true;
    }

    private static int templateIndex(RockeryPlacement placement) {
        for (int i = 0; i < ROCKERY_TEMPLATES.size(); i++) {
            RockeryTemplate t = ROCKERY_TEMPLATES.get(i);
            if (t.block() == placement.block() && t.dimensions().equals(placement.dimensions())) {
                return i;
            }
        }
        return 0;
    }

    private static void storePlacementData(ItemStack stack, BlockPos clickedPos,
                                           int templateIdx, BlockPos root, Direction facing, long startTick) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(TARGET_POS_KEY, clickedPos.asLong());
        tag.putLong(ROOT_KEY, root.asLong());
        tag.putInt(TEMPLATE_KEY, templateIdx);
        tag.putInt(FACING_KEY, facing.ordinal());
        tag.putLong(START_TICK_KEY, startTick);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearPlacementData(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    private static boolean hasPlacementData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TARGET_POS_KEY);
    }

    private static @Nullable PlacementData readPlacementData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(TARGET_POS_KEY)) return null;
        var tag = data.copyTag();
        PlacementData placement = new PlacementData(
                BlockPos.of(tag.getLong(TARGET_POS_KEY).orElse(0L)),
                BlockPos.of(tag.getLong(ROOT_KEY).orElse(0L)),
                tag.getInt(TEMPLATE_KEY).orElse(0),
                tag.getInt(FACING_KEY).orElse(0),
                tag.getLong(START_TICK_KEY).orElse(0L)
        );
        return getTemplate(placement) == null ? null : placement;
    }

    private record PlacementData(BlockPos clickedPos, BlockPos root, int templateIndex, int facingOrdinal, long startTick) {}

    private static final class HammerSession {
        private final ResourceKey<Level> dimension;
        private final PlacementData placement;
        private final int[] breakerIds;
        private int lastSentCrackProgress = -1;
        private float progress;

        private HammerSession(ResourceKey<Level> dimension, PlacementData placement, int[] breakerIds) {
            this.dimension = dimension;
            this.placement = placement;
            this.breakerIds = breakerIds;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private PlacementData placement() {
            return placement;
        }

        private int[] breakerIds() {
            return breakerIds;
        }

        private int lastSentCrackProgress() {
            return lastSentCrackProgress;
        }

        private void setLastSentCrackProgress(int lastSentCrackProgress) {
            this.lastSentCrackProgress = lastSentCrackProgress;
        }

        private float progress() {
            return progress;
        }

        private void setProgress(float progress) {
            this.progress = progress;
        }
    }

    static final class AnimationTiming {
        private static final double TICKS_PER_SECOND = 20.0D;
        private static final double INTRO_TICKS = 0.45833D * TICKS_PER_SECOND;
        private static final double LOOP_LENGTH_TICKS = 0.75D * TICKS_PER_SECOND;
        private static final double FIRST_IMPACT_TICKS =
                INTRO_TICKS + 0.625D * TICKS_PER_SECOND;

        private AnimationTiming() {
        }

        static boolean isImpactTick(int usedTicks) {
            if (usedTicks < FIRST_IMPACT_TICKS) {
                return false;
            }
            double previousImpactCycle = Math.floor(
                    (usedTicks - 1.0D - FIRST_IMPACT_TICKS) / LOOP_LENGTH_TICKS);
            double currentImpactCycle = Math.floor(
                    (usedTicks - FIRST_IMPACT_TICKS) / LOOP_LENGTH_TICKS);
            return currentImpactCycle > previousImpactCycle;
        }
    }

    public record RockeryTemplate(DeferredBlock<RockeryBlock> block, RockeryDimensions dimensions) {
        private int partCount() {
            return dimensions.partCount();
        }
    }

    private record RockeryPlacement(DeferredBlock<RockeryBlock> block, RockeryDimensions dimensions, BlockPos root, Direction facing) {
        private void place(Level level) {
            Block rockery = block.get();
            for (int part = 0; part < dimensions.partCount(); part++) {
                BlockPos pos = root.offset(dimensions.worldOffset(part, facing));
                BlockState state = rockery.defaultBlockState()
                        .setValue(RockeryBlock.FACING, facing)
                        .setValue(RockeryBlock.PART, part);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
            for (int part = 0; part < dimensions.partCount(); part++) {
                BlockPos pos = root.offset(dimensions.worldOffset(part, facing));
                level.updateNeighborsAt(pos, rockery);
            }
        }

        private void playEffects(ServerLevel level, BlockPos clickedPos) {
            level.playSound(null, clickedPos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.9F, 0.65F);
            level.playSound(null, clickedPos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.25F);
            level.playSound(null, clickedPos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.55F, 1.15F);
            level.playSound(null, clickedPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);

            BlockParticleOption stoneDust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            for (int part = 0; part < dimensions.partCount(); part++) {
                BlockPos pos = root.offset(dimensions.worldOffset(part, facing));
                level.sendParticles(
                        stoneDust,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.65D,
                        pos.getZ() + 0.5D,
                        18,
                        0.38D,
                        0.45D,
                        0.38D,
                        0.08D
                );
            }
        }
    }
}
