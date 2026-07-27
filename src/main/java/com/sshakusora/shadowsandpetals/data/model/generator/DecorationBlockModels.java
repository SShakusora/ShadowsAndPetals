package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class DecorationBlockModels {
    private DecorationBlockModels() {
    }

    public static void ingotPile(
            BlockModelContext<? extends IngotPileBlock> context,
            SAPBlockModelGenerator generator
    ) {
        IngotPileBlock block = context.get();
        String metal = context.name().endsWith("_ingot_pile")
                ? context.name().substring(0, context.name().length() - "_ingot_pile".length())
                : context.name();
        Identifier bottom = generator.modLoc("block/ingot_pile/" + metal + "_bottom");
        Identifier full = generator.modLoc("block/ingot_pile/" + metal + "_double");
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(IngotPileBlock.TYPE)
                        .select(SlabType.BOTTOM, BlockModelGenerators.plainVariant(bottom))
                        .select(SlabType.TOP, BlockModelGenerators.plainVariant(bottom))
                        .select(SlabType.DOUBLE, BlockModelGenerators.plainVariant(full)))
                .with(PropertyDispatch.modify(IngotPileBlock.HORIZONTAL_AXIS)
                        .select(Direction.Axis.X, BlockModelGenerators.NOP)
                        .select(Direction.Axis.Z, BlockModelGenerators.Y_ROT_90)));
        StandardBlockModels.parentBlockItem(block, generator, bottom);
    }

    public static void vanity(
            BlockModelContext<? extends VanityBlock> context,
            SAPBlockModelGenerator generator
    ) {
        VanityBlock block = context.get();
        String wood = context.name().endsWith("_vanity")
                ? context.name().substring(0, context.name().length() - "_vanity".length())
                : context.name();
        Identifier lower = generator.modLoc("block/vanity/" + wood + "_lower");
        Identifier upper = generator.modLoc("block/vanity/" + wood + "_upper");
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(VanityBlock.HALF)
                        .select(DoubleBlockHalf.LOWER, BlockModelGenerators.plainVariant(lower))
                        .select(DoubleBlockHalf.UPPER, BlockModelGenerators.plainVariant(upper)))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(VanityBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
    }

    public static void irori(
            BlockModelContext<? extends IroriBlock> context,
            SAPBlockModelGenerator generator
    ) {
        IroriBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(
                                IroriBlock.NORTH,
                                IroriBlock.EAST,
                                IroriBlock.SOUTH,
                                IroriBlock.WEST,
                                IroriBlock.WATERLOGGED
                        )
                        .generate((north, east, south, west, waterlogged) ->
                                iroriVariant(generator, north, east, south, west)))
                .with(PropertyDispatch.modify(IroriBlock.HAS_GRILL)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
        StandardBlockModels.parentBlockItem(block, generator, generator.modLoc("block/irori/block"));
    }

    public static void copperTeapot(
            BlockModelContext<? extends CopperTeapotBlock> context,
            SAPBlockModelGenerator generator
    ) {
        CopperTeapotBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(CopperTeapotBlock.ON_IRORI)
                        .select(false, BlockModelGenerators.plainVariant(generator.modLoc("block/teapot/copper/main")))
                        .select(true, BlockModelGenerators.plainVariant(generator.modLoc("block/teapot/copper/main_on_irori"))))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(CopperTeapotBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
    }

    public static void bedroomLamp(
            BlockModelContext<? extends BedroomLampBlock> context,
            SAPBlockModelGenerator generator
    ) {
        BedroomLampBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BedroomLampBlock.LIT)
                        .select(false, BlockModelGenerators.plainVariant(generator.modLoc("block/bedroom_lamp/off")))
                        .select(true, BlockModelGenerators.plainVariant(generator.modLoc("block/bedroom_lamp/on")))));
    }

    public static void wallLamp(
            BlockModelContext<? extends WallLampBlock> context,
            SAPBlockModelGenerator generator
    ) {
        WallLampBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(WallLampBlock.LIT)
                        .select(false, BlockModelGenerators.plainVariant(generator.modLoc("block/wall_lamp/off")))
                        .select(true, BlockModelGenerators.plainVariant(generator.modLoc("block/wall_lamp/on"))))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    public static void emergencyLamp(
            BlockModelContext<? extends EmergencyLampBlock> context,
            SAPBlockModelGenerator generator
    ) {
        EmergencyLampBlock block = context.get();
        Identifier off = generator.modLoc("block/emergency_lamp/off");
        Identifier on = generator.modLoc("block/emergency_lamp/on");
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(EmergencyLampBlock.LIT)
                        .select(false, BlockModelGenerators.plainVariant(off))
                        .select(true, BlockModelGenerators.plainVariant(on)))
                .with(PropertyDispatch.modify(EmergencyLampBlock.FACING)
                        .select(Direction.UP, BlockModelGenerators.NOP)
                        .select(Direction.DOWN, BlockModelGenerators.X_ROT_180)
                        .select(Direction.NORTH, BlockModelGenerators.X_ROT_90)
                        .select(Direction.EAST, BlockModelGenerators.X_ROT_90.then(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.SOUTH, BlockModelGenerators.X_ROT_90.then(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.WEST, BlockModelGenerators.X_ROT_90.then(BlockModelGenerators.Y_ROT_270))));
    }

    public static void recessedLamp(
            BlockModelContext<? extends RecessedLampBlock> context,
            SAPBlockModelGenerator generator
    ) {
        RecessedLampBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(RecessedLampBlock.MOUNT, RecessedLampBlock.LIT)
                        .select(RecessedLampBlock.Mount.FLOOR, false, model(generator, "block/recessed_lamp/up_off"))
                        .select(RecessedLampBlock.Mount.FLOOR, true, model(generator, "block/recessed_lamp/up_on"))
                        .select(RecessedLampBlock.Mount.FLOOR_SLAB, false, model(generator, "block/recessed_lamp/up_slab_off"))
                        .select(RecessedLampBlock.Mount.FLOOR_SLAB, true, model(generator, "block/recessed_lamp/up_slab_on"))
                        .select(RecessedLampBlock.Mount.CEILING, false, model(generator, "block/recessed_lamp/down_off"))
                        .select(RecessedLampBlock.Mount.CEILING, true, model(generator, "block/recessed_lamp/down_on"))
                        .select(RecessedLampBlock.Mount.CEILING_SLAB, false, model(generator, "block/recessed_lamp/down_slab_off"))
                        .select(RecessedLampBlock.Mount.CEILING_SLAB, true, model(generator, "block/recessed_lamp/down_slab_on")))
                .with(PropertyDispatch.modify(RecessedLampBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
    }

    public static void deskLamp(
            BlockModelContext<? extends DeskLampBlock> context,
            SAPBlockModelGenerator generator
    ) {
        DeskLampBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(DeskLampBlock.LIT)
                        .select(false, model(generator, "block/desk_lamp/off"))
                        .select(true, model(generator, "block/desk_lamp/on")))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    public static void samon(
            BlockModelContext<? extends SamonBlock> context,
            SAPBlockModelGenerator generator
    ) {
        SamonBlock block = context.get();
        Identifier straight = generator.modLoc("block/samon/straight");
        Identifier corner = generator.modLoc("block/samon/corner");
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(SamonBlock.FACING, SamonBlock.CORNER)
                        .generate((facing, isCorner) -> rotatedVariant(
                                isCorner ? corner : straight,
                                0,
                                (int) facing.toYRot()
                        ))));
        StandardBlockModels.parentBlockItem(block, generator, straight);
    }

    public static void shishiOdoshi(
            BlockModelContext<? extends ShishiOdoshiBlock> context,
            SAPBlockModelGenerator generator
    ) {
        ShishiOdoshiBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(
                        block,
                        model(generator, "block/shishi_odoshi/block")
                )
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(ShishiOdoshiBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
    }

    public static void shishiOdoshiPipe(
            BlockModelContext<? extends ShishiOdoshiPipeBlock> context,
            SAPBlockModelGenerator generator
    ) {
        ShishiOdoshiPipeBlock block = context.get();
        generator.blockState(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(ShishiOdoshiPipeBlock.LENGTH)
                        .generate(length -> model(
                                generator,
                                "block/shishi_odoshi/pipe/" + length.getSerializedName()
                        )))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)
                .with(PropertyDispatch.modify(ShishiOdoshiPipeBlock.WATERLOGGED)
                        .select(false, BlockModelGenerators.NOP)
                        .select(true, BlockModelGenerators.NOP)));
        StandardBlockModels.parentBlockItem(
                block,
                generator,
                generator.modLoc("block/shishi_odoshi/pipe/long")
        );
    }

    private static MultiVariant iroriVariant(
            SAPBlockModelGenerator generator,
            boolean north,
            boolean east,
            boolean south,
            boolean west
    ) {
        boolean edgeNorth = !north;
        boolean edgeEast = !east;
        boolean edgeSouth = !south;
        boolean edgeWest = !west;
        int edges = (edgeNorth ? 1 : 0) + (edgeEast ? 1 : 0) + (edgeSouth ? 1 : 0) + (edgeWest ? 1 : 0);
        Identifier model = switch (edges) {
            case 0 -> generator.modLoc("block/irori/center");
            case 1 -> generator.modLoc("block/irori/single_edge");
            case 2 -> edgeNorth == edgeSouth || edgeEast == edgeWest
                    ? generator.modLoc("block/irori/double_edge")
                    : generator.modLoc("block/irori/corner");
            case 3 -> generator.modLoc("block/irori/end");
            case 4 -> generator.modLoc("block/irori/block");
            default -> throw new IllegalStateException("Unexpected edge count: " + edges);
        };
        return rotatedVariant(model, 0, iroriRotation(edgeNorth, edgeEast, edgeSouth, edgeWest, edges));
    }

    private static int iroriRotation(boolean north, boolean east, boolean south, boolean west, int edges) {
        return switch (edges) {
            case 0, 4 -> 0;
            case 1 -> east ? 0 : south ? 90 : west ? 180 : 270;
            case 2 -> {
                if (east && west) yield 0;
                if (north && south) yield 90;
                if (north && east) yield 0;
                if (east && south) yield 90;
                if (south && west) yield 180;
                yield 270;
            }
            case 3 -> !south ? 0 : !west ? 90 : !north ? 180 : 270;
            default -> throw new IllegalStateException("Unexpected edge count: " + edges);
        };
    }

    private static MultiVariant model(SAPBlockModelGenerator generator, String path) {
        return BlockModelGenerators.plainVariant(generator.modLoc(path));
    }

    private static MultiVariant rotatedVariant(Identifier model, int x, int y) {
        MultiVariant variant = BlockModelGenerators.plainVariant(model);
        variant = switch (Math.floorMod(x, 360)) {
            case 0 -> variant;
            case 90 -> variant.with(BlockModelGenerators.X_ROT_90);
            case 180 -> variant.with(BlockModelGenerators.X_ROT_180);
            case 270 -> variant.with(BlockModelGenerators.X_ROT_270);
            default -> throw new IllegalArgumentException("Unsupported X rotation: " + x);
        };
        return switch (Math.floorMod(y, 360)) {
            case 0 -> variant;
            case 90 -> variant.with(BlockModelGenerators.Y_ROT_90);
            case 180 -> variant.with(BlockModelGenerators.Y_ROT_180);
            case 270 -> variant.with(BlockModelGenerators.Y_ROT_270);
            default -> throw new IllegalArgumentException("Unsupported Y rotation: " + y);
        };
    }
}
