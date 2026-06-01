package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BlockEntityRegistry {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VanityBlockEntity>> VANITY = SAPRegistries
            .<VanityBlockEntity>blockEntity("vanity")
            .factory(VanityBlockEntity::new)
            .validBlocks(BlockRegistry.VANITIES.toArray())
            .register();

    public static void init() {}
}
