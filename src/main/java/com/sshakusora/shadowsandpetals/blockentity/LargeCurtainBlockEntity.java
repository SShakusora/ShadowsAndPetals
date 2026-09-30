package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Animation clock for the four-block large curtain. */
public final class LargeCurtainBlockEntity extends AbstractCurtainBlockEntity {
    public LargeCurtainBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.LARGE_CURTAIN.get(), pos, blockState);
    }
}
