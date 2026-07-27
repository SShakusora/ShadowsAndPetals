package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.BlockModelTemplates;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public final class WoodBlockModels {
    private WoodBlockModels() {
    }

    public static void woodSet(
            BlockModelContext<? extends Block> context,
            SAPBlockModelGenerator generator,
            WoodSetList.WoodSet set
    ) {
        Block log = set.log().get();
        Block strippedLog = set.strippedLog().get();
        Block wood = set.wood().get();
        Block strippedWood = set.strippedWood().get();
        Block planks = set.planks().get();

        generator.vanilla().woodProvider(log).logWithHorizontal(log).wood(wood);
        generator.vanilla().woodProvider(strippedLog).logWithHorizontal(strippedLog).wood(strippedWood);
        BlockFamily family = new BlockFamily.Builder(planks)
                .slab(set.slab().get())
                .stairs(set.stairs().get())
                .fence(set.fence().get())
                .fenceGate(set.fenceGate().get())
                .pressurePlate(set.pressurePlate().get())
                .button(set.button().get())
                .getFamily();
        generator.vanilla().family(planks).generateFor(family);
        generator.suggestItemModel(planks.asItem(), generator.blockModelId(planks));
        generator.suggestItemModel(
                set.fenceGate().get().asItem(),
                generator.blockModelId(set.fenceGate().get())
        );
        generator.suggestItemModel(
                set.pressurePlate().get().asItem(),
                generator.blockModelId(set.pressurePlate().get())
        );
    }

    public static void post(
            BlockModelContext<? extends WoodPostBlock> context,
            SAPBlockModelGenerator generator,
            Identifier sideTexture,
            Identifier endTexture
    ) {
        WoodPostBlock block = context.get();
        Identifier core = generator.blockModelId(block);
        Identifier lowerLink = generator.modLoc(core.getPath() + "_link");
        Identifier upperLink = generator.modLoc(core.getPath() + "_link_top");
        generator.jsonModel(core, BlockModelTemplates.woodPostCoreModel(sideTexture, endTexture));
        generator.jsonModel(lowerLink, BlockModelTemplates.woodPostLinkModel(sideTexture, endTexture, false));
        generator.jsonModel(upperLink, BlockModelTemplates.woodPostLinkModel(sideTexture, endTexture, true));

        for (WoodPostBlock.ConnectionType type : WoodPostBlock.ConnectionType.values()) {
            if (!type.isChain()) {
                continue;
            }
            generator.jsonModelOnce(
                    chainModelId(generator, type, false),
                    BlockModelTemplates.woodPostChainModel(false, type.texture())
            );
            generator.jsonModelOnce(
                    chainModelId(generator, type, true),
                    BlockModelTemplates.woodPostChainModel(true, type.texture())
            );
        }

        generator.blockState(BlockModelGenerators.createAxisAlignedPillarBlock(
                block,
                BlockModelGenerators.plainVariant(core)
        ));
        StandardBlockModels.parentBlockItem(block, generator, core);
    }

    private static Identifier chainModelId(
            SAPBlockModelGenerator generator,
            WoodPostBlock.ConnectionType type,
            boolean upperHalf
    ) {
        return generator.modLoc("block/wood_post_" + type.getSerializedName()
                + (upperHalf ? "_link_top" : "_link"));
    }
}
