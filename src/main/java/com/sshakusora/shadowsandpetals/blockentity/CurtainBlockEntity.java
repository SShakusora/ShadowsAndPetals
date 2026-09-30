package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Animation clock for the one-block curtain. */
public final class CurtainBlockEntity extends AbstractCurtainBlockEntity {
    public CurtainBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CURTAIN.get(), pos, blockState);
    }
}
