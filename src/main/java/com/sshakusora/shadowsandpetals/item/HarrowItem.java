package com.sshakusora.shadowsandpetals.item;

import com.sshakusora.shadowsandpetals.block.decoration.SamonBlock;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class HarrowItem extends Item {
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public HarrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // Right-click gravel -> convert to samon
        if (state.is(Blocks.GRAVEL)) {
            if (!level.isClientSide()) {
                SamonBlock samon = BlockRegistry.SAMON.get();
                level.setBlock(pos, samon.getStateForConnections(level, pos), 3);
                level.playSound(null, pos, SoundEvents.GRAVEL_BREAK,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                ((ServerLevel) level).sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        12, 0.3, 0.3, 0.3, 0.1);
            }
            return InteractionResult.SUCCESS;
        }

        // Right-click samon top -> cycle state
        if (state.getBlock() instanceof SamonBlock && context.getClickedFace() == Direction.UP) {
            int facingIdx = indexOf(state.getValue(SamonBlock.FACING));
            int cornerVal = state.getValue(SamonBlock.CORNER) ? 4 : 0;
            int next = (cornerVal + facingIdx + 1) % 8;

            Direction nextFacing = FACINGS[next % 4];
            boolean nextCorner = next >= 4;

            level.setBlock(pos, state
                    .setValue(SamonBlock.FACING, nextFacing)
                    .setValue(SamonBlock.CORNER, nextCorner), 3);
            level.playSound(context.getPlayer(), pos, SoundEvents.WOOD_PLACE,
                    SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static int indexOf(Direction dir) {
        for (int i = 0; i < FACINGS.length; i++) {
            if (FACINGS[i] == dir) return i;
        }
        return 0;
    }
}
