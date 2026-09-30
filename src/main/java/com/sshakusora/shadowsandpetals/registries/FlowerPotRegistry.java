package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.data.model.generator.NatureBlockModels;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/** Registers mod plants that can be placed in the vanilla flower pot. */
public final class FlowerPotRegistry {
    public static final DeferredBlock<FlowerPotBlock> POTTED_SAKURA_SAPLING = registerPotted(
            "potted_sakura_sapling",
            BlockRegistry.SAKURA_SET.sapling(),
            ShadowsAndPetals.asResource("block/sakura_sapling"),
            "盆栽樱花树苗"
    );
    public static final DeferredBlock<FlowerPotBlock> POTTED_MAPLE_SAPLING = registerPotted(
            "potted_maple_sapling",
            BlockRegistry.MAPLE_SET.sapling(),
            ShadowsAndPetals.asResource("block/maple_sapling"),
            "盆栽枫树苗"
    );
    public static final DeferredBlock<FlowerPotBlock> POTTED_GINKGO_SAPLING = registerPotted(
            "potted_ginkgo_sapling",
            BlockRegistry.GINKGO_SET.sapling(),
            ShadowsAndPetals.asResource("block/ginkgo_sapling"),
            "盆栽银杏树苗"
    );
    public static final DeferredBlock<FlowerPotBlock> POTTED_AUTUMN_OAK_SAPLING = registerPotted(
            "potted_autumn_oak_sapling",
            BlockRegistry.AUTUMN_OAK_SAPLING,
            ShadowsAndPetals.asResource("block/autumn_oak_sapling"),
            "盆栽秋橡树树苗"
    );

    private FlowerPotRegistry() {
    }

    private static DeferredBlock<FlowerPotBlock> registerPotted(
            String id,
            Supplier<? extends Block> plant,
            Identifier texture,
            String zhName
    ) {
        return SAPRegistries
                .block(id, properties -> new FlowerPotBlock(
                        () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                        plant,
                        properties
                ))
                .properties(properties -> BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT))
                .lang(DatagenLangRegistry.ZH_CN, zhName)
                .blockstate(() -> (context, generator) ->
                        NatureBlockModels.pottedPlant(context, generator, texture))
                .loot((provider, pot) -> provider.dropPottedPlant(pot.get(), plant.get()))
                .register();
    }

    /** Adds the registered plants to the vanilla empty flower pot's content map. */
    public static void registerPlants() {
        FlowerPotBlock emptyPot = (FlowerPotBlock) Blocks.FLOWER_POT;
        emptyPot.addPlant(BlockRegistry.SAKURA_SET.sapling().getId(), POTTED_SAKURA_SAPLING);
        emptyPot.addPlant(BlockRegistry.MAPLE_SET.sapling().getId(), POTTED_MAPLE_SAPLING);
        emptyPot.addPlant(BlockRegistry.GINKGO_SET.sapling().getId(), POTTED_GINKGO_SAPLING);
        emptyPot.addPlant(BlockRegistry.AUTUMN_OAK_SAPLING.getId(), POTTED_AUTUMN_OAK_SAPLING);
    }

    public static void init() {
    }
}
