package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.blockentity.*;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BlockEntityRegistry {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SandExcavationBlockEntity>> SAND_EXCAVATION =
            SAPRegistries.<SandExcavationBlockEntity>blockEntity("sand_excavation")
                    .factory(SandExcavationBlockEntity::new)
                    .validBlocks(BlockRegistry.SAND_EXCAVATION)
                    .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IroriBlockEntity>> IRORI = SAPRegistries
            .<IroriBlockEntity>blockEntity("irori")
            .factory(IroriBlockEntity::new)
            .validBlocks(BlockRegistry.IRORI)
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VanityBlockEntity>> VANITY = SAPRegistries
            .<VanityBlockEntity>blockEntity("vanity")
            .factory(VanityBlockEntity::new)
            .validBlocks(BlockRegistry.VANITIES.toArray())
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShishiOdoshiBlockEntity>> SHISHI_ODOSHI = SAPRegistries
            .<ShishiOdoshiBlockEntity>blockEntity("shishi_odoshi")
            .factory(ShishiOdoshiBlockEntity::new)
            .validBlocks(BlockRegistry.SHISHI_ODOSHI)
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShishiOdoshiPipeBlockEntity>> SHISHI_ODOSHI_PIPE = SAPRegistries
            .<ShishiOdoshiPipeBlockEntity>blockEntity("shishi_odoshi_pipe")
            .factory(ShishiOdoshiPipeBlockEntity::new)
            .validBlocks(BlockRegistry.SHISHI_ODOSHI_PIPE)
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindChimeBlockEntity>> WIND_CHIME = SAPRegistries
            .<WindChimeBlockEntity>blockEntity("wind_chime")
            .factory(WindChimeBlockEntity::new)
            .validBlocks(BlockRegistry.WIND_CHIME)
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperTeapotBlockEntity>> COPPER_TEAPOT = SAPRegistries
            .<CopperTeapotBlockEntity>blockEntity("copper_teapot")
            .factory(CopperTeapotBlockEntity::new)
            .validBlocks(BlockRegistry.COPPER_TEAPOT)
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecessedLampBlockEntity>> RECESSED_LAMP = SAPRegistries
            .<RecessedLampBlockEntity>blockEntity("recessed_lamp")
            .factory(RecessedLampBlockEntity::new)
            .validBlocks(BlockRegistry.RECESSED_LAMP_COMPOSITE)
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BonsaiBlockEntity>> BONSAI = SAPRegistries
            .<BonsaiBlockEntity>blockEntity("bonsai")
            .factory(BonsaiBlockEntity::new)
            .validBlocks(BlockRegistry.BONSAI)
            .register();

    public static void init() {}
}
