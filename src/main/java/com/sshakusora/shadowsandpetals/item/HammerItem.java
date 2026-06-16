package com.sshakusora.shadowsandpetals.item;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jspecify.annotations.Nullable;

import java.util.*;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.DEDICATED_SERVER)
public class HammerItem extends Item {
    public static final int USE_DURATION = 30;
    private static final int MAX_USE_DURATION = 72000;
    private static final int BASE_USE_DURATION = 15;
    private static final int TICKS_PER_PART = 10;
    private static final int LOOK_REFRESH_INTERVAL_TICKS = 5;
    public static final float HAMMER_STRIKE_PERIOD_TICKS = 10.0F;
    public static final float HAMMER_IMPACT_PHASE = 0.16F;
    private static final int HAMMER_STRIKE_PERIOD_TICKS_INT = Math.round(HAMMER_STRIKE_PERIOD_TICKS);
    private static final int HAMMER_IMPACT_TICK = Math.round(HAMMER_STRIKE_PERIOD_TICKS * HAMMER_IMPACT_PHASE);
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final String TARGET_POS_KEY = "hammer_target";
    private static final String ROOT_KEY = "hammer_root";
    private static final String TEMPLATE_KEY = "hammer_template_idx";
    private static final String FACING_KEY = "hammer_facing";
    private static final String START_TICK_KEY = "hammer_start_tick";

    private static final List<RockeryTemplate> ROCKERY_TEMPLATES = new ArrayList<>();
    private static final Map<BlockPos, Float> HAMMERING_PROGRESS = new HashMap<>();
    private static final Map<Integer, Integer> LAST_SENT_CRACK_PROGRESS = new HashMap<>();

    /**
     * Registers a rockery multi-block for hammer placement.
     * Called automatically by {@code BlockRegistry.registerRockery()}
     * during static initialization.
     */
    public static void registerRockery(DeferredBlock<RockeryBlock> block, RockeryDimensions dims) {
        ROCKERY_TEMPLATES.add(new RockeryTemplate(block, dims));
        ROCKERY_TEMPLATES.sort(Comparator.comparingInt(RockeryTemplate::partCount).reversed());
    }

    public HammerItem(Properties properties) {
        super(properties);
    }

    public static float getHammeringProgress(BlockPos pos) {
        return HAMMERING_PROGRESS.getOrDefault(pos, -1.0F);
    }

    private static void updateHammeringProgress(PlacementData data, float progress) {
        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = DIRECTIONS[data.facingOrdinal()];
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos pos = data.root().offset(template.dimensions().worldOffset(part, facing));
            HAMMERING_PROGRESS.put(pos, progress);
        }
    }

    private static void clearHammeringProgress(PlacementData data) {
        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = DIRECTIONS[data.facingOrdinal()];
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos pos = data.root().offset(template.dimensions().worldOffset(part, facing));
            HAMMERING_PROGRESS.remove(pos);
        }
    }

    /**
     * Clean up leaked static map entries when a player disconnects during hammering.
     * {@code LAST_SENT_CRACK_PROGRESS} is keyed by player ID — remove the leaving player's entry.
     * {@code HAMMERING_PROGRESS} is keyed by {@link BlockPos} (not per-player), so we clear all
     * entries; active hammering players will repopulate the map on their next {@code onUseTick}.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT_CRACK_PROGRESS.remove(event.getEntity().getId());
        HAMMERING_PROGRESS.clear();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
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
                return BASE_USE_DURATION + ROCKERY_TEMPLATES.get(templateIdx).dimensions().partCount() * TICKS_PER_PART;
            }
        }
        return USE_DURATION;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null
                || context.getHand() != InteractionHand.MAIN_HAND
                || !player.getOffhandItem().is(ItemRegistry.CHISEL.get())) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (!level.getBlockState(clickedPos).is(Blocks.STONE)) {
            return InteractionResult.PASS;
        }

        RockeryPlacement placement = findPlacement(level, clickedPos, player.getDirection().getOpposite());
        if (placement == null) {
            return InteractionResult.PASS;
        }

        int templateIdx = templateIndex(placement);
        storePlacementData(context.getItemInHand(), clickedPos, templateIdx, placement.root(), placement.facing(), level.getGameTime());

        // Begin the hammering animation
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) level;
        PlacementData data = readPlacementData(stack);
        if (data == null) {
            return;
        }
        if (!player.getOffhandItem().is(ItemRegistry.CHISEL.get())) {
            cancelHammering(player, stack, serverLevel, data);
            return;
        }

        BlockPos targetPos = refreshPlacementFromLook(player, serverLevel, stack, data);
        if (targetPos == null) {
            cancelHammering(player, stack, serverLevel, data);
            return;
        }

        int usedTicks = getUsedTicks(serverLevel, data);
        if (usedTicks >= getEffectiveUseDuration(data)) {
            completeHammering(stack, serverLevel, player);
            player.releaseUsingItem();
            return;
        }

        // Update destruction crack overlay on all rockery parts
        updateCrackProgress(player, data, serverLevel, usedTicks);

        if (isHammerImpactTick(usedTicks)) {
            serverLevel.playSound(null, targetPos,
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS,
                    0.55F, 1.45F + level.getRandom().nextFloat() * 0.25F);
        }

        if (usedTicks > 0 && usedTicks % 6 == 0) {
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
        if (!level.isClientSide() && entity instanceof Player player) {
            PlacementData data = readPlacementData(stack);
            if (data != null) {
                clearHammeringVisuals(player, data, (ServerLevel) level);
            }
        }
        clearPlacementData(stack);
        return false;
    }

    private static void completeHammering(ItemStack stack, Level level, Player player) {
        PlacementData data = readPlacementData(stack);
        if (data == null) return;

        if (level instanceof ServerLevel serverLevel) {
            clearHammeringVisuals(player, data, serverLevel);
        } else {
            clearHammeringProgress(data);
        }
        clearPlacementData(stack);

        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = Direction.values()[data.facingOrdinal()];

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
        }
    }

    private static void cancelHammering(Player player, ItemStack stack, ServerLevel level) {
        PlacementData data = readPlacementData(stack);
        if (data != null) {
            clearHammeringVisuals(player, data, level);
        }
        clearPlacementData(stack);
        player.releaseUsingItem();
    }

    private static void cancelHammering(Player player, ItemStack stack, ServerLevel level, PlacementData data) {
        clearHammeringVisuals(player, data, level);
        clearPlacementData(stack);
        player.releaseUsingItem();
    }

    private static boolean isHammerImpactTick(int usedTicks) {
        return usedTicks > 0 && usedTicks % HAMMER_STRIKE_PERIOD_TICKS_INT == HAMMER_IMPACT_TICK;
    }

    private static int getUsedTicks(ServerLevel level, PlacementData data) {
        long elapsed = level.getGameTime() - data.startTick() + 1L;
        if (elapsed <= 0L) {
            return 1;
        }
        return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
    }

    private static void updateCrackProgress(Player player, PlacementData data, ServerLevel level, int usedTicks) {
        int duration = getEffectiveUseDuration(data);
        int progress = Math.clamp(usedTicks * 10L / Math.max(1, duration), 0, 9);
        int playerId = player.getId();
        Integer lastProgress = LAST_SENT_CRACK_PROGRESS.get(playerId);
        if (lastProgress != null && lastProgress == progress) {
            float normalizedProgress = (float) usedTicks / Math.max(1, duration);
            updateHammeringProgress(data, normalizedProgress);
            return;
        }
        LAST_SENT_CRACK_PROGRESS.put(playerId, progress);
        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = DIRECTIONS[data.facingOrdinal()];
        int baseId = playerId;
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos pos = data.root().offset(template.dimensions().worldOffset(part, facing));
            // Use unique breaker ID per part: LevelRenderer.destroyingBlocks is keyed by ID only,
            // so multiple positions for the same ID would overwrite each other.
            int uniqueId = baseId * 100 + part;
            sendDestructionPacket(level, uniqueId, pos, progress);
        }

        // Sync progress to Jade via static map
        float normalizedProgress = (float) usedTicks / Math.max(1, duration);
        updateHammeringProgress(data, normalizedProgress);
    }

    private static int getEffectiveUseDuration(PlacementData data) {
        if (data.templateIndex() >= 0 && data.templateIndex() < ROCKERY_TEMPLATES.size()) {
            return BASE_USE_DURATION + ROCKERY_TEMPLATES.get(data.templateIndex()).dimensions().partCount() * TICKS_PER_PART;
        }
        return USE_DURATION;
    }

    private static void clearHammeringVisuals(Player player, PlacementData data, ServerLevel level) {
        clearCrackProgressForData(player, data, level);
        clearHammeringProgress(data);
    }

    private static void clearCrackProgressForData(Player player, PlacementData data, ServerLevel level) {
        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = DIRECTIONS[data.facingOrdinal()];
        int baseId = player.getId();
        LAST_SENT_CRACK_PROGRESS.remove(baseId);
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos pos = data.root().offset(template.dimensions().worldOffset(part, facing));
            int uniqueId = baseId * 100 + part;
            sendDestructionPacket(level, uniqueId, pos, -1);
        }
    }

    private static void sendDestructionPacket(ServerLevel level, int breakerId, BlockPos pos, int progress) {
        var packet = new ClientboundBlockDestructionPacket(breakerId, pos, progress);
        for (ServerPlayer serverPlayer : level.players()) {
            double dx = pos.getX() - serverPlayer.getX();
            double dy = pos.getY() - serverPlayer.getY();
            double dz = pos.getZ() - serverPlayer.getZ();
            if (dx * dx + dy * dy + dz * dz < 1024.0D) {
                serverPlayer.connection.send(packet);
            }
        }
    }

    private static @Nullable BlockPos refreshPlacementFromLook(Player player, ServerLevel level, ItemStack stack, PlacementData data) {
        BlockPos lookedPos = getLookedAtStone(player, level);
        if (lookedPos == null) {
            return null;
        }

        if (!data.clickedPos().equals(lookedPos)) {
            clearHammeringVisuals(player, data, level);
            RockeryPlacement placement = findPlacement(level, lookedPos, player.getDirection().getOpposite());
            if (placement == null) {
                clearPlacementData(stack);
                return null;
            }

            int templateIdx = templateIndex(placement);
            storePlacementData(stack, lookedPos, templateIdx, placement.root(), placement.facing(), level.getGameTime());
            return lookedPos;
        }

        if (getUsedTicks(level, data) % LOOK_REFRESH_INTERVAL_TICKS != 0) {
            return lookedPos;
        }

        if (placementStillMatches(level, data)) {
            return lookedPos;
        }

        RockeryPlacement placement = findPlacement(level, lookedPos, player.getDirection().getOpposite());
        clearHammeringVisuals(player, data, level);
        if (placement == null) {
            clearPlacementData(stack);
            return null;
        }

        int templateIdx = templateIndex(placement);
        storePlacementData(stack, lookedPos, templateIdx, placement.root(), placement.facing(), level.getGameTime());
        return lookedPos;
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

    private static @Nullable BlockPos getTargetPos(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(TARGET_POS_KEY)) {
            return BlockPos.of(data.copyTag().getLong(TARGET_POS_KEY).orElse(0L));
        }
        return null;
    }

    private static void clearPlacementData(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    private static @Nullable PlacementData readPlacementData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(TARGET_POS_KEY)) return null;
        var tag = data.copyTag();
        return new PlacementData(
                BlockPos.of(tag.getLong(TARGET_POS_KEY).orElse(0L)),
                BlockPos.of(tag.getLong(ROOT_KEY).orElse(0L)),
                tag.getInt(TEMPLATE_KEY).orElse(0),
                tag.getInt(FACING_KEY).orElse(0),
                tag.getLong(START_TICK_KEY).orElse(0L)
        );
    }

    private record PlacementData(BlockPos clickedPos, BlockPos root, int templateIndex, int facingOrdinal, long startTick) {}

    private record RockeryTemplate(DeferredBlock<RockeryBlock> block, RockeryDimensions dimensions) {
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
