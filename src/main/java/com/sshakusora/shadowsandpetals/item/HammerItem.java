package com.sshakusora.shadowsandpetals.item;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import com.sshakusora.shadowsandpetals.block.nature.RockeryBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class HammerItem extends Item {
    private static List<RockeryTemplate> rockeries() {
        return Stream.of(
            RockeryTemplate.of(BlockRegistry.ROCKERY_1x1x1, 1, 1, 1),
            RockeryTemplate.of(BlockRegistry.ROCKERY_1x1x2, 1, 1, 2),
            RockeryTemplate.of(BlockRegistry.ROCKERY_1x2x1, 1, 2, 1),
            RockeryTemplate.of(BlockRegistry.ROCKERY_1x2x2, 1, 2, 2),
            RockeryTemplate.of(BlockRegistry.ROCKERY_1x3x1, 1, 3, 1)
        )
            .sorted(Comparator.comparingInt(RockeryTemplate::partCount).reversed())
            .toList();
    }

    public HammerItem(Properties properties) {
        super(properties);
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

        if (!level.isClientSide()) {
            placement.place(level);
            if (level instanceof ServerLevel serverLevel) {
                placement.playEffects(serverLevel, clickedPos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static @Nullable RockeryPlacement findPlacement(Level level, BlockPos clickedPos, Direction preferredFacing) {
        for (RockeryTemplate template : rockeries()) {
            RockeryPlacement preferred = findPlacement(level, clickedPos, template, preferredFacing);
            if (preferred != null) {
                return preferred;
            }

            for (Direction facing : Direction.Plane.HORIZONTAL) {
                if (facing == preferredFacing) {
                    continue;
                }

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

    private record RockeryTemplate(DeferredBlock<RockeryBlock> block, RockeryDimensions dimensions) {
        private static RockeryTemplate of(DeferredBlock<RockeryBlock> block, int width, int height, int depth) {
            return new RockeryTemplate(block, new RockeryDimensions(width, height, depth));
        }

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
