package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Animation clock for the two-cell long curtain. */
public final class LongCurtainBlockEntity extends AbstractCurtainBlockEntity {
    public LongCurtainBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.LONG_CURTAIN.get(), pos, blockState);
    }
}
