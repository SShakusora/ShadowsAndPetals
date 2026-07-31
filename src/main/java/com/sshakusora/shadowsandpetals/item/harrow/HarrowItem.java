package com.sshakusora.shadowsandpetals.item.harrow;

import com.sshakusora.shadowsandpetals.block.decoration.SamonBlock;
import com.sshakusora.shadowsandpetals.block.nature.SandExcavationBlock;
import com.sshakusora.shadowsandpetals.blockentity.SandExcavationBlockEntity;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.world.excavation.SandExcavationCooldownData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class HarrowItem extends Item {
    private static final int USE_DURATION = 200;
    private static final Direction[] FACINGS = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    public HarrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.GRAVEL)) {
            if (!level.isClientSide()) {
                SamonBlock samon = BlockRegistry.SAMON.get();
                level.setBlock(pos, samon.getStateForConnections(level, pos), 3);
                level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                ((ServerLevel) level).sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        12,
                        0.3,
                        0.3,
                        0.3,
                        0.1
                );
            }
            return InteractionResult.SUCCESS;
        }

        if (state.getBlock() instanceof SamonBlock && context.getClickedFace() == Direction.UP) {
            if (!level.isClientSide()) {
                int facingIdx = indexOf(state.getValue(SamonBlock.FACING));
                int cornerVal = state.getValue(SamonBlock.CORNER) ? 4 : 0;
                int next = (cornerVal + facingIdx + 1) % 8;

                level.setBlock(pos, state
                        .setValue(SamonBlock.FACING, FACINGS[next % 4])
                        .setValue(SamonBlock.CORNER, next >= 4), 3);
                level.playSound(context.getPlayer(), pos, SoundEvents.WOOD_PLACE,
                        SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }

        if (isNotDiggableSand(state)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (context.getClickedFace() != Direction.UP || !isDiggingEnvironment(level, pos)) {
            return failDigging(
                    level,
                    player,
                    Component.translatable(BuiltinLanguageKeys.HARROW_DIGGING_ENVIRONMENT_REQUIRED.key())
            );
        }

        if (!level.isClientSide() && state.is(Blocks.SAND)) {
            ServerLevel serverLevel = (ServerLevel) level;
            long remainingTicks = SandExcavationCooldownData.getRemainingCooldownTicks(serverLevel, pos);
            if (remainingTicks > 0L) {
                long remainingSeconds = Math.ceilDiv(remainingTicks, 20L);
                return failDigging(
                        level,
                        player,
                        Component.translatable(BuiltinLanguageKeys.HARROW_DIGGING_COOLDOWN.key(), remainingSeconds)
                );
            }
            level.setBlock(pos, BlockRegistry.SAND_EXCAVATION.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof SandExcavationBlockEntity excavation) {
                excavation.begin(level.getGameTime());
            }
        }

        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return HarrowUseAnimationEnumExtensions.getHarrowDigging();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_DURATION;
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_BRUSH_ACTIONS.contains(itemAbility);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int ticksRemaining) {
        if (ticksRemaining < 0 || !(livingEntity instanceof Player player)) {
            livingEntity.releaseUsingItem();
            return;
        }

        HitResult hitResult = calculateHitResult(player);
        if (!(hitResult instanceof BlockHitResult blockHit)
                || hitResult.getType() != HitResult.Type.BLOCK
                || blockHit.getDirection() != Direction.UP) {
            livingEntity.releaseUsingItem();
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SandExcavationBlock) || !isDiggingEnvironment(level, pos)) {
            livingEntity.releaseUsingItem();
            return;
        }

        int timeElapsed = getUseDuration(stack, livingEntity) - ticksRemaining + 1;
        if (timeElapsed % SandExcavationBlockEntity.BRUSH_COOLDOWN_TICKS != 5) {
            return;
        }

        HumanoidArm arm = livingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        if (state.shouldSpawnTerrainParticles() && state.getRenderShape() != RenderShape.INVISIBLE) {
            spawnDustParticles(level, blockHit, state, livingEntity.getViewVector(0.0F), arm);
        }
        level.playSound(player, pos, SoundEvents.BRUSH_SAND, SoundSource.BLOCKS);

        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SandExcavationBlockEntity excavation
                && excavation.brush(level.getGameTime(), serverLevel, blockHit.getDirection())) {
            EquipmentSlot slot = livingEntity.getUsedItemHand() == InteractionHand.OFF_HAND
                    ? EquipmentSlot.OFFHAND
                    : EquipmentSlot.MAINHAND;
            stack.hurtAndBreak(1, player, slot);
        }
    }

    private static HitResult calculateHitResult(Player player) {
        return ProjectileUtil.getHitResultOnViewVector(
                player,
                EntitySelector.CAN_BE_PICKED,
                player.blockInteractionRange()
        );
    }

    private static boolean isDiggingEnvironment(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isNotDiggableSand(state)) {
            return false;
        }
        if (!level.getBlockState(pos.above()).canBeReplaced()) {
            return false;
        }
        return level.getBiome(pos).is(BiomeTags.IS_BEACH) && hasNearbyWater(level, pos);
    }

    private static boolean hasNearbyWater(Level level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if ((x != 0 || z != 0) && isWater(level, pos.offset(x, 0, z))) {
                    return true;
                }
            }
        }

        return isWater(level, pos.above())
                || isWater(level, pos.north(2))
                || isWater(level, pos.south(2))
                || isWater(level, pos.west(2))
                || isWater(level, pos.east(2));
    }

    private static boolean isNotDiggableSand(BlockState state) {
        return !state.is(Blocks.SAND) && !(state.getBlock() instanceof SandExcavationBlock);
    }

    private static InteractionResult failDigging(Level level, Player player, Component message) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(message);
        }
        return InteractionResult.FAIL;
    }

    private static boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static void spawnDustParticles(
            Level level,
            BlockHitResult hitResult,
            BlockState state,
            Vec3 viewVector,
            HumanoidArm arm
    ) {
        int flip = arm == HumanoidArm.RIGHT ? 1 : -1;
        Vec3 hit = hitResult.getLocation();
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        int count = level.getRandom().nextInt(7, 12);

        for (int i = 0; i < count; i++) {
            level.addParticle(
                    particle,
                    hit.x,
                    hit.y,
                    hit.z,
                    viewVector.z() * flip * 3.0 * level.getRandom().nextDouble(),
                    0.0,
                    -viewVector.x() * flip * 3.0 * level.getRandom().nextDouble()
            );
        }
    }

    private static int indexOf(Direction direction) {
        for (int i = 0; i < FACINGS.length; i++) {
            if (FACINGS[i] == direction) {
                return i;
            }
        }
        return 0;
    }
}
