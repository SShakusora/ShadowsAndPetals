package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class CafeTableBlock extends AbstractConnectingTableBlock {
    public static final MapCodec<CafeTableBlock> CODEC = simpleCodec(CafeTableBlock::new);

    public CafeTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CafeTableBlock> codec() {
        return CODEC;
    }
}
