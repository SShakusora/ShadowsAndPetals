package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModularDeskBlock extends AbstractConnectingTableBlock {
    public static final MapCodec<ModularDeskBlock> CODEC = simpleCodec(ModularDeskBlock::new);

    public ModularDeskBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ModularDeskBlock> codec() {
        return CODEC;
    }
}
