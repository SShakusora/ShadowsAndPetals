package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.decoration.IngotPileBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ShadowsAndPetals.MOD_ID, existingFileHelper);
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

    public void logBlockWithItem(RotatedPillarBlock block, Identifier sideTexture, Identifier endTexture) {
        axisBlock(block, sideTexture, endTexture);
        simpleBlockItem(block, models().getExistingFile(sideTexture));
    }

    public void leavesBlockWithItem(LeavesBlock block) {
        simpleBlockWithItem(block, models().cubeAll(name(block), blockTexture(block)));
    }

    public void leavesBlockWithItem(LeavesBlock block, Identifier texture) {
        simpleBlockWithItem(block, models().cubeAll(name(block), texture));
    }

    public void cubeAllBlockWithItem(Block block) {
        simpleBlockWithItem(block, models().cubeAll(name(block), blockTexture(block)));
    }

    public void cubeAllBlockWithItem(Block block, Identifier texture) {
        simpleBlockWithItem(block, models().cubeAll(name(block), texture));
    }

    public void saplingBlockWithItem(SaplingBlock block) {
        var model = models().cross(name(block), blockTexture(block)).renderType("cutout");
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    public void saplingBlockWithItem(SaplingBlock block, Identifier texture) {
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

    private String name(Block block) {
        return block.builtInRegistryHolder().key().location().getPath();
    }
}
