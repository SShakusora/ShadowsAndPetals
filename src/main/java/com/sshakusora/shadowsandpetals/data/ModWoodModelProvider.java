package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.stream.Stream;

public class ModWoodModelProvider extends ModelProvider {
    private static final Block[] WOOD_BLOCKS = BlockRegistry.WOOD_SETS.stream()
            .flatMap(ModWoodModelProvider::blocksOf)
            .toArray(Block[]::new);

    public ModWoodModelProvider(PackOutput output) {
        super(output, ShadowsAndPetals.MOD_ID);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.of(WOOD_BLOCKS).map(Block::builtInRegistryHolder);
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.of(WOOD_BLOCKS).map(Block::asItem).map(Item::builtInRegistryHolder);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BlockRegistry.WOOD_SETS.forEach(woodSet -> registerWoodSet(blockModels, woodSet));
    }

    private static Stream<Block> blocksOf(WoodSetList.WoodSet woodSet) {
        return Arrays.stream(new Block[]{
                woodSet.log().get(),
                woodSet.strippedLog().get(),
                woodSet.wood().get(),
                woodSet.strippedWood().get(),
                woodSet.planks().get(),
                woodSet.slab().get(),
                woodSet.stairs().get(),
                woodSet.fence().get(),
                woodSet.fenceGate().get(),
                woodSet.pressurePlate().get(),
                woodSet.button().get()
        });
    }

    private void registerWoodSet(BlockModelGenerators blockModels, WoodSetList.WoodSet woodSet) {
        Block log = woodSet.log().get();
        Block strippedLog = woodSet.strippedLog().get();
        Block wood = woodSet.wood().get();
        Block strippedWood = woodSet.strippedWood().get();
        Block planks = woodSet.planks().get();
        Block slab = woodSet.slab().get();
        Block stairs = woodSet.stairs().get();
        Block fence = woodSet.fence().get();
        Block fenceGate = woodSet.fenceGate().get();
        Block pressurePlate = woodSet.pressurePlate().get();
        Block button = woodSet.button().get();

        blockModels.woodProvider(log).logWithHorizontal(log).wood(wood);
        blockModels.woodProvider(strippedLog).logWithHorizontal(strippedLog).wood(strippedWood);

        BlockFamily family = new BlockFamily.Builder(planks)
                .slab(slab)
                .stairs(stairs)
                .fence(fence)
                .fenceGate(fenceGate)
                .pressurePlate(pressurePlate)
                .button(button)
                .getFamily();
        blockModels.family(planks).generateFor(family);
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Wood Models";
    }
}
