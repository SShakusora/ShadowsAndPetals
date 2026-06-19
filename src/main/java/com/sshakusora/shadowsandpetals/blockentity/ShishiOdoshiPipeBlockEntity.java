package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShishiOdoshiPipeBlockEntity extends BlockEntity {
    public ShishiOdoshiPipeBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get(), pos, blockState);
    }
}
