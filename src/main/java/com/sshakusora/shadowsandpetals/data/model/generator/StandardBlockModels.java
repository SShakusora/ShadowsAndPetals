package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** Legacy 1.21.1 model callback names retained as no-op hooks. */
public final class StandardBlockModels {
    private StandardBlockModels() {}
    public static void cubeAll(BlockModelContext context, SAPBlockModelGenerator generator) {
        cubeAll(context, generator, generator.modLoc("block/" + context.name()));
    }
    public static void cubeAll(
            BlockModelContext context,
            SAPBlockModelGenerator generator,
            ResourceLocation texture
    ) {
        generator.cubeAllWithItem(context.get(), context.name(), texture);
    }
    public static void cubeAll(Object... ignored) {}
    public static void simpleBlock(Object... ignored) {}
    public static void simpleBlockWithItem(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            ResourceLocation model
    ) {
        generator.simpleBlockWithItem(context.get(), model);
    }
    public static void simpleBlockWithItem(Object... ignored) {}
    public static void simpleWaterloggedBlockWithItem(Object... ignored) {}
    public static void horizontalFacingCubeAll(Object... ignored) {}
    public static void fluid(Object... ignored) {}
    public static void verticalSlab(Object... ignored) {}
    public static void slab(Object... ignored) {}
    public static void stairs(Object... ignored) {}
    public static void parentBlockItem(Object... ignored) {}
}
