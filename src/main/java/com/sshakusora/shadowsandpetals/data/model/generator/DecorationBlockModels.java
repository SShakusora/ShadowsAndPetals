package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.*;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillCopperTeapotBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

public final class DecorationBlockModels {
    private DecorationBlockModels() {}

    public static void ingotPile(BlockModelContext<? extends IngotPileBlock> context,
                                 SAPBlockModelGenerator generator) {
        IngotPileBlock block = context.get();
        String metal = suffix(context.name(), "_ingot_pile");
        ResourceLocation bottom = generator.modLoc("block/ingot_pile/" + metal + "_bottom");
        ResourceLocation full = generator.modLoc("block/ingot_pile/" + metal + "_double");
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(state.getValue(IngotPileBlock.TYPE) == SlabType.DOUBLE ? full : bottom))
                .rotationY(state.getValue(IngotPileBlock.HORIZONTAL_AXIS) == Direction.Axis.Z ? 90 : 0)
                .build());
        StandardBlockModels.parentBlockItem(block, generator, bottom);
    }

    public static void woodenBarrel(BlockModelContext<? extends WoodenBarrelBlock> context,
                                    SAPBlockModelGenerator generator) {
        WoodenBarrelBlock block = context.get();
        ResourceLocation model = generator.modLoc("block/wooden_barrel/wooden_barrel");
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(model))
                .rotationY(state.getValue(WoodenBarrelBlock.AXIS) == Direction.Axis.X ? 90 : 0)
                .build());
        StandardBlockModels.parentBlockItem(block, generator, model);
    }

    public static void vanity(BlockModelContext<? extends VanityBlock> context,
                              SAPBlockModelGenerator generator) {
        VanityBlock block = context.get();
        String wood = suffix(context.name(), "_vanity");
        ResourceLocation lower = generator.modLoc("block/vanity/" + wood + "_lower");
        ResourceLocation upper = generator.modLoc("block/vanity/" + wood + "_upper");
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(state.getValue(VanityBlock.HALF) == DoubleBlockHalf.LOWER ? lower : upper))
                .rotationY(horizontalRotation(state.getValue(VanityBlock.FACING)))
                .build());
        StandardBlockModels.parentBlockItem(block, generator, generator.modLoc("item/vanity/" + wood));
    }

    public static void irori(BlockModelContext<? extends IroriBlock> context,
                             SAPBlockModelGenerator generator) {
        IroriBlock block = context.get();
        generator.provider().getVariantBuilder(block).forAllStates(state -> {
            boolean north = state.getValue(IroriBlock.NORTH);
            boolean east = state.getValue(IroriBlock.EAST);
            boolean south = state.getValue(IroriBlock.SOUTH);
            boolean west = state.getValue(IroriBlock.WEST);
            int edges = (north ? 0 : 1) + (east ? 0 : 1) + (south ? 0 : 1) + (west ? 0 : 1);
            ResourceLocation model = switch (edges) {
                case 0 -> generator.modLoc("block/irori/center");
                case 1 -> generator.modLoc("block/irori/single_edge");
                case 2 -> north == south || east == west
                        ? generator.modLoc("block/irori/double_edge")
                        : generator.modLoc("block/irori/corner");
                case 3 -> generator.modLoc("block/irori/end");
                case 4 -> generator.modLoc("block/irori/block");
                default -> throw new IllegalStateException("Unexpected irori edge count: " + edges);
            };
            return ConfiguredModel.builder().modelFile(generator.uncheckedModel(model))
                    .rotationY(iroriRotation(!north, !east, !south, !west, edges)).build();
        });
        StandardBlockModels.parentBlockItem(block, generator, generator.modLoc("block/irori/block"));
    }

    public static void iroriGrill(BlockModelContext<? extends IroriGrillBlock> context,
                                  SAPBlockModelGenerator generator) {
        IroriGrillBlock block = context.get();
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> {
            IroriGrillPart part = state.getValue(IroriGrillBlock.GRILL_PART);
            return ConfiguredModel.builder()
                    .modelFile(generator.uncheckedModel(generator.modLoc(
                            "block/grill/double/" + part.modelName() + "_upper")))
                    .rotationY(part == IroriGrillPart.STRIP_WEST || part == IroriGrillPart.STRIP_EAST ? 90 : 0)
                    .build();
        }, IroriGrillBlock.WATERLOGGED);
    }

    public static void copperTeapot(BlockModelContext<? extends CopperTeapotBlock> context,
                                    SAPBlockModelGenerator generator) {
        CopperTeapotBlock block = context.get();
        ResourceLocation model = generator.modLoc("block/teapot/copper/main");
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(model))
                .rotationY(horizontalRotation(state.getValue(CopperTeapotBlock.FACING)))
                .build(), CopperTeapotBlock.WATERLOGGED);
    }

    public static void iroriGrillCopperTeapot(
            BlockModelContext<? extends IroriGrillCopperTeapotBlock> context,
            SAPBlockModelGenerator generator) {
        IroriGrillCopperTeapotBlock block = context.get();
        ResourceLocation parent = generator.modLoc("block/teapot/copper/main");
        ResourceLocation model = generator.createTranslatedParentModel(
                "block/teapot/copper/main_on_grill", parent, 0.0F, 0.3125F, 0.0F);
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(model))
                .rotationY(horizontalRotation(state.getValue(CopperTeapotBlock.FACING)))
                .build(), CopperTeapotBlock.WATERLOGGED, IroriGrillBlock.GRILL_PART);
    }

    public static void bedroomLamp(BlockModelContext<? extends BedroomLampBlock> context,
                                   SAPBlockModelGenerator generator) {
        BedroomLampBlock block = context.get();
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(generator.modLoc(
                        "block/bedroom_lamp/" + (state.getValue(BedroomLampBlock.LIT) ? "on" : "off"))))
                .build());
    }

    public static void wallLamp(BlockModelContext<? extends WallLampBlock> context,
                                SAPBlockModelGenerator generator) {
        WallLampBlock block = context.get();
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(generator.modLoc(
                        "block/wall_lamp/" + (state.getValue(WallLampBlock.LIT) ? "on" : "off"))))
                .rotationY(horizontalRotation(state.getValue(WallLampBlock.FACING)))
                .build());
    }

    public static void emergencyLamp(BlockModelContext<? extends EmergencyLampBlock> context,
                                     SAPBlockModelGenerator generator) {
        EmergencyLampBlock block = context.get();
        generator.provider().getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(EmergencyLampBlock.FACING);
            ConfiguredModel.Builder builder = ConfiguredModel.builder()
                    .modelFile(generator.uncheckedModel(generator.modLoc(
                            "block/emergency_lamp/" + (state.getValue(EmergencyLampBlock.LIT) ? "on" : "off"))));
            switch (facing) {
                case DOWN -> builder.rotationX(180);
                case NORTH -> builder.rotationX(90);
                case EAST -> builder.rotationX(90).rotationY(90);
                case SOUTH -> builder.rotationX(90).rotationY(180);
                case WEST -> builder.rotationX(90).rotationY(270);
                default -> { }
            }
            return builder.build();
        });
    }

    public static void recessedLamp(BlockModelContext<? extends RecessedLampBlock> context,
                                    SAPBlockModelGenerator generator) {
        RecessedLampBlock block = context.get();
        ResourceLocation upOff = generator.modLoc("block/recessed_lamp/up_off");
        ResourceLocation upOn = generator.modLoc("block/recessed_lamp/up_on");
        ResourceLocation downOff = generator.modLoc("block/recessed_lamp/down_off");
        ResourceLocation downOn = generator.modLoc("block/recessed_lamp/down_on");
        ResourceLocation upSlabOff = generator.createTranslatedParentModel(
                "block/recessed_lamp/up_slab_off", upOff, 0.0F, -0.5F, 0.0F);
        ResourceLocation upSlabOn = generator.createTranslatedParentModel(
                "block/recessed_lamp/up_slab_on", upOn, 0.0F, -0.5F, 0.0F);
        ResourceLocation downSlabOff = generator.createTranslatedParentModel(
                "block/recessed_lamp/down_slab_off", downOff, 0.0F, 0.5F, 0.0F);
        ResourceLocation downSlabOn = generator.createTranslatedParentModel(
                "block/recessed_lamp/down_slab_on", downOn, 0.0F, 0.5F, 0.0F);
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(recessedModel(state.getValue(RecessedLampBlock.MOUNT),
                        state.getValue(RecessedLampBlock.LIT), upOff, upOn, downOff, downOn,
                        upSlabOff, upSlabOn, downSlabOff, downSlabOn)))
                .build());
    }

    public static void recessedLampComposite(
            BlockModelContext<? extends RecessedLampCompositeBlock> context,
            SAPBlockModelGenerator generator) {
        RecessedLampCompositeBlock block = context.get();
        ResourceLocation upOff = generator.modLoc("block/recessed_lamp/up_off");
        ResourceLocation upOn = generator.modLoc("block/recessed_lamp/up_on");
        ResourceLocation downOff = generator.modLoc("block/recessed_lamp/down_off");
        ResourceLocation downOn = generator.modLoc("block/recessed_lamp/down_on");
        ResourceLocation upSlabOff = generator.createTranslatedParentModel(
                "block/recessed_lamp/up_composite_off", upOff, 0.0F, 0.5F, 0.0F);
        ResourceLocation upSlabOn = generator.createTranslatedParentModel(
                "block/recessed_lamp/up_composite_on", upOn, 0.0F, 0.5F, 0.0F);
        ResourceLocation downSlabOff = generator.createTranslatedParentModel(
                "block/recessed_lamp/down_composite_off", downOff, 0.0F, -0.5F, 0.0F);
        ResourceLocation downSlabOn = generator.createTranslatedParentModel(
                "block/recessed_lamp/down_composite_on", downOn, 0.0F, -0.5F, 0.0F);
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(recessedModel(state.getValue(RecessedLampBlock.MOUNT),
                        state.getValue(RecessedLampBlock.LIT), upOff, upOn, downOff, downOn,
                        upSlabOff, upSlabOn, downSlabOff, downSlabOn)))
                .build());
    }

    public static void deskLamp(BlockModelContext<? extends DeskLampBlock> context,
                                SAPBlockModelGenerator generator) {
        DeskLampBlock block = context.get();
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(generator.modLoc(
                        "block/desk_lamp/" + (state.getValue(DeskLampBlock.LIT) ? "on" : "off"))))
                .rotationY(horizontalRotation(state.getValue(DeskLampBlock.FACING)))
                .build());
    }

    public static void samon(BlockModelContext<? extends SamonBlock> context,
                             SAPBlockModelGenerator generator) {
        SamonBlock block = context.get();
        ResourceLocation straight = generator.modLoc("block/samon/straight");
        ResourceLocation corner = generator.modLoc("block/samon/corner");
        generator.provider().getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(state.getValue(SamonBlock.CORNER) ? corner : straight))
                .rotationY(horizontalRotation(state.getValue(SamonBlock.FACING)))
                .build());
        StandardBlockModels.parentBlockItem(block, generator, straight);
    }

    public static void shishiOdoshi(BlockModelContext<? extends ShishiOdoshiBlock> context,
                                    SAPBlockModelGenerator generator) {
        ShishiOdoshiBlock block = context.get();
        ResourceLocation model = generator.modLoc("block/shishi_odoshi/block");
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(model))
                .rotationY(horizontalRotation(state.getValue(ShishiOdoshiBlock.FACING)))
                .build(), ShishiOdoshiBlock.WATERLOGGED);
    }

    public static void shishiOdoshiPipe(BlockModelContext<? extends ShishiOdoshiPipeBlock> context,
                                        SAPBlockModelGenerator generator) {
        ShishiOdoshiPipeBlock block = context.get();
        generator.provider().getVariantBuilder(block).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(generator.uncheckedModel(generator.modLoc(
                        "block/shishi_odoshi/pipe/" + state.getValue(ShishiOdoshiPipeBlock.LENGTH).getSerializedName())))
                .rotationY(horizontalRotation(state.getValue(ShishiOdoshiPipeBlock.FACING)))
                .build(), ShishiOdoshiPipeBlock.WATERLOGGED);
        StandardBlockModels.parentBlockItem(block, generator,
                generator.modLoc("block/shishi_odoshi/pipe/long"));
    }

    private static ResourceLocation recessedModel(RecessedLampBlock.Mount mount, boolean lit,
                                                  ResourceLocation upOff, ResourceLocation upOn,
                                                  ResourceLocation downOff, ResourceLocation downOn,
                                                  ResourceLocation upSlabOff, ResourceLocation upSlabOn,
                                                  ResourceLocation downSlabOff, ResourceLocation downSlabOn) {
        return switch (mount) {
            case FLOOR -> lit ? upOn : upOff;
            case FLOOR_SLAB -> lit ? upSlabOn : upSlabOff;
            case CEILING -> lit ? downOn : downOff;
            case CEILING_SLAB -> lit ? downSlabOn : downSlabOff;
        };
    }

    private static int horizontalRotation(Direction direction) {
        return switch (direction) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
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
            default -> throw new IllegalStateException("Unexpected irori edge count: " + edges);
        };
    }

    private static String suffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

}
