package com.sshakusora.shadowsandpetals.item;

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
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HammerItem extends Item {
    private static final int USE_DURATION = 30;
    private static final String TARGET_POS_KEY = "hammer_target";
    private static final String ROOT_KEY = "hammer_root";
    private static final String TEMPLATE_KEY = "hammer_template_idx";
    private static final String FACING_KEY = "hammer_facing";

    private static final List<RockeryTemplate> ROCKERY_TEMPLATES = new ArrayList<>();

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

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
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
        storePlacementData(context.getItemInHand(), clickedPos, templateIdx, placement.root(), placement.facing());

        // Begin the hammering animation
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide()) return;
        if (!player.getOffhandItem().is(ItemRegistry.CHISEL.get())) {
            player.releaseUsingItem();
            return;
        }

        BlockPos targetPos = getTargetPos(stack);
        if (targetPos == null) return;
        if (!level.getBlockState(targetPos).is(Blocks.STONE)) return;

        // Play hammering sound and particles periodically
        int usedTicks = USE_DURATION - remainingTicks;
        if (usedTicks > 0 && usedTicks % 6 == 0) {
            ServerLevel serverLevel = (ServerLevel) level;
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
        if (!(entity instanceof Player player)) return stack;
        if (level.isClientSide()) return stack;

        PlacementData data = readPlacementData(stack);
        clearPlacementData(stack);

        if (data == null) return stack;

        RockeryTemplate template = ROCKERY_TEMPLATES.get(data.templateIndex());
        Direction facing = Direction.values()[data.facingOrdinal()];

        // Validate all parts are still STONE (world may have changed during animation)
        for (int part = 0; part < template.dimensions().partCount(); part++) {
            BlockPos pos = data.root().offset(template.dimensions().worldOffset(part, facing));
            if (!level.getBlockState(pos).is(Blocks.STONE)) {
                return stack;
            }
        }

        // Reconstruct placement and apply
        RockeryPlacement placement = new RockeryPlacement(
                template.block(), template.dimensions(), data.root(), facing);
        placement.place(level);
        if (level instanceof ServerLevel serverLevel) {
            placement.playEffects(serverLevel, data.clickedPos());
        }

        return stack;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        clearPlacementData(stack);
        return false;
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
                                           int templateIdx, BlockPos root, Direction facing) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(TARGET_POS_KEY, clickedPos.asLong());
        tag.putLong(ROOT_KEY, root.asLong());
        tag.putInt(TEMPLATE_KEY, templateIdx);
        tag.putInt(FACING_KEY, facing.ordinal());
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
                tag.getInt(FACING_KEY).orElse(0)
        );
    }

    private record PlacementData(BlockPos clickedPos, BlockPos root, int templateIndex, int facingOrdinal) {}

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
