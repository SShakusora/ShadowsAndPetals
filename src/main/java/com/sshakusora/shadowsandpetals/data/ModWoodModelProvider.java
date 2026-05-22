package com.sshakusora.shadowsandpetals.data;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.stream.Stream;

public class ModWoodModelProvider extends ModelProvider {
    private static final Block[] WOOD_BLOCKS = new Block[]{
            BlockRegistry.SAKURA_LOG.get(),
            BlockRegistry.STRIPPED_SAKURA_LOG.get(),
            BlockRegistry.SAKURA_WOOD.get(),
            BlockRegistry.STRIPPED_SAKURA_WOOD.get(),
            BlockRegistry.SAKURA_PLANKS.get(),
            BlockRegistry.SAKURA_SLAB.get(),
            BlockRegistry.SAKURA_STAIRS.get(),
            BlockRegistry.SAKURA_FENCE.get(),
            BlockRegistry.SAKURA_FENCE_GATE.get(),
            BlockRegistry.SAKURA_PRESSURE_PLATE.get(),
            BlockRegistry.SAKURA_BUTTON.get(),
            BlockRegistry.MAPLE_LOG.get(),
            BlockRegistry.STRIPPED_MAPLE_LOG.get(),
            BlockRegistry.MAPLE_WOOD.get(),
            BlockRegistry.STRIPPED_MAPLE_WOOD.get(),
            BlockRegistry.MAPLE_PLANKS.get(),
            BlockRegistry.MAPLE_SLAB.get(),
            BlockRegistry.MAPLE_STAIRS.get(),
            BlockRegistry.MAPLE_FENCE.get(),
            BlockRegistry.MAPLE_FENCE_GATE.get(),
            BlockRegistry.MAPLE_PRESSURE_PLATE.get(),
            BlockRegistry.MAPLE_BUTTON.get(),
            BlockRegistry.GINKGO_LOG.get(),
            BlockRegistry.STRIPPED_GINKGO_LOG.get(),
            BlockRegistry.GINKGO_WOOD.get(),
            BlockRegistry.STRIPPED_GINKGO_WOOD.get(),
            BlockRegistry.GINKGO_PLANKS.get(),
            BlockRegistry.GINKGO_SLAB.get(),
            BlockRegistry.GINKGO_STAIRS.get(),
            BlockRegistry.GINKGO_FENCE.get(),
            BlockRegistry.GINKGO_FENCE_GATE.get(),
            BlockRegistry.GINKGO_PRESSURE_PLATE.get(),
            BlockRegistry.GINKGO_BUTTON.get()
    };

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
        registerWoodSet(
                blockModels,
                BlockRegistry.SAKURA_LOG.get(),
                BlockRegistry.STRIPPED_SAKURA_LOG.get(),
                BlockRegistry.SAKURA_WOOD.get(),
                BlockRegistry.STRIPPED_SAKURA_WOOD.get(),
                BlockRegistry.SAKURA_PLANKS.get(),
                BlockRegistry.SAKURA_SLAB.get(),
                BlockRegistry.SAKURA_STAIRS.get(),
                BlockRegistry.SAKURA_FENCE.get(),
                BlockRegistry.SAKURA_FENCE_GATE.get(),
                BlockRegistry.SAKURA_PRESSURE_PLATE.get(),
                BlockRegistry.SAKURA_BUTTON.get()
        );

        registerWoodSet(
                blockModels,
                BlockRegistry.MAPLE_LOG.get(),
                BlockRegistry.STRIPPED_MAPLE_LOG.get(),
                BlockRegistry.MAPLE_WOOD.get(),
                BlockRegistry.STRIPPED_MAPLE_WOOD.get(),
                BlockRegistry.MAPLE_PLANKS.get(),
                BlockRegistry.MAPLE_SLAB.get(),
                BlockRegistry.MAPLE_STAIRS.get(),
                BlockRegistry.MAPLE_FENCE.get(),
                BlockRegistry.MAPLE_FENCE_GATE.get(),
                BlockRegistry.MAPLE_PRESSURE_PLATE.get(),
                BlockRegistry.MAPLE_BUTTON.get()
        );

        registerWoodSet(
                blockModels,
                BlockRegistry.GINKGO_LOG.get(),
                BlockRegistry.STRIPPED_GINKGO_LOG.get(),
                BlockRegistry.GINKGO_WOOD.get(),
                BlockRegistry.STRIPPED_GINKGO_WOOD.get(),
                BlockRegistry.GINKGO_PLANKS.get(),
                BlockRegistry.GINKGO_SLAB.get(),
                BlockRegistry.GINKGO_STAIRS.get(),
                BlockRegistry.GINKGO_FENCE.get(),
                BlockRegistry.GINKGO_FENCE_GATE.get(),
                BlockRegistry.GINKGO_PRESSURE_PLATE.get(),
                BlockRegistry.GINKGO_BUTTON.get()
        );
    }

    private void registerWoodSet(
            BlockModelGenerators blockModels,
            Block log,
            Block strippedLog,
            Block wood,
            Block strippedWood,
            Block planks,
            Block slab,
            Block stairs,
            Block fence,
            Block fenceGate,
            Block pressurePlate,
            Block button
    ) {
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
