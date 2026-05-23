package com.sshakusora.shadowsandpetals.data;


import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import com.sshakusora.shadowsandpetals.block.decoration.VanityBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.sshakusora.shadowsandpetals.ShadowsAndPetals.MOD_ID;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (var generator : DatagenBlockStateRegistry.generators()) {
            generator.accept(this);
        }
    }

    public void logBlockWithItem(RotatedPillarBlock block) {
        logBlock(block);
        simpleBlockItem(block, models().getExistingFile(blockTexture(block)));
    }

    public void logBlockWithItem(RotatedPillarBlock block, ResourceLocation sideTexture, ResourceLocation endTexture) {
        axisBlock(block, sideTexture, endTexture);
        simpleBlockItem(block, models().getExistingFile(sideTexture));
    }

    public void leavesBlockWithItem(LeavesBlock block) {
        simpleBlockWithItem(block, models().cubeAll(name(block), blockTexture(block)));
    }

    public void leavesBlockWithItem(LeavesBlock block, ResourceLocation texture) {
        simpleBlockWithItem(block, models().cubeAll(name(block), texture));
    }

    public void cubeAllBlockWithItem(Block block) {
        simpleBlockWithItem(block, models().cubeAll(name(block), blockTexture(block)));
    }

    public void cubeAllBlockWithItem(Block block, ResourceLocation texture) {
        simpleBlockWithItem(block, models().cubeAll(name(block), texture));
    }

    public void saplingBlockWithItem(SaplingBlock block) {
        var model = models().cross(name(block), blockTexture(block)).renderType("cutout");
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    public void saplingBlockWithItem(SaplingBlock block, ResourceLocation texture) {
        var model = models().cross(name(block), texture).renderType("cutout");
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    public void ingotPileBlock(IngotPileBlock block) {
        String blockName = name(block);
        String metalName = blockName.endsWith("_ingot_pile")
                ? blockName.substring(0, blockName.length() - "_ingot_pile".length())
                : blockName;

        getVariantBuilder(block).forAllStates(state -> {
            boolean isDouble = state.getValue(IngotPileBlock.TYPE) == SlabType.DOUBLE;
            boolean isZ = state.getValue(IngotPileBlock.HORIZONTAL_AXIS) == Direction.Axis.Z;
            String modelPath = "block/ingot_pile/" + metalName + (isDouble ? "_double" : "_bottom");
            return ConfiguredModel.builder()
                    .modelFile(models().getExistingFile(modLoc(modelPath)))
                    .rotationY(isZ ? 90 : 0)
                    .build();
        });
        simpleBlockItem(block, models().getExistingFile(modLoc("block/ingot_pile/" + metalName + "_bottom")));
    }

    private boolean modelExists(String modelPath) {
        Path path = Paths.get(
                "src/main/resources/assets/" +
                        MOD_ID +
                        "/models/" +
                        modelPath +
                        ".json"
        );

        return Files.exists(path);
    }

    public void vanityBlock(VanityBlock block) {
        String blockName = name(block);
        String woodName = blockName.endsWith("_vanity")
                ? blockName.substring(0, blockName.length() - "_vanity".length())
                : blockName;

        // TODO: Need To Replace Oak Upper
        ResourceLocation lowerModel = modelExists("block/vanity/" + woodName + "_lower") ? modLoc("block/vanity/" + woodName + "_lower") : modLoc("block/vanity/oak_lower");
        ResourceLocation upperModel = modelExists("block/vanity/" + woodName + "_upper") ? modLoc("block/vanity/" + woodName + "_upper") : modLoc("block/vanity/oak_upper");
        getVariantBuilder(block).forAllStates(state -> {
            ResourceLocation model = state.getValue(VanityBlock.HALF) == DoubleBlockHalf.LOWER ? lowerModel : upperModel;
            int rotationY = switch (state.getValue(VanityBlock.FACING)) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            return ConfiguredModel.builder()
                    .modelFile(models().getExistingFile(model))
                    .rotationY(rotationY)
                    .build();
        });
        ResourceLocation itemModel = modelExists("item/vanity/" + woodName)
                ? modLoc("item/vanity/" + woodName)
                : modLoc("item/vanity/oak");
        simpleBlockItem(block, models().getExistingFile(itemModel));
    }

    private String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }
}
