package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
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

    private String name(Block block) {
        return block.builtInRegistryHolder().key().location().getPath();
    }
}
