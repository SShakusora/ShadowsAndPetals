package com.sshakusora.shadowsandpetals.data.model.generator;

import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPillarBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public final class WoodPillarBlockModels {
    private static final Identifier MODEL = ShadowsAndPetals.asResource("models/block/wood_pillar/stripped_wood_pillar.obj");
    private static final Identifier PARENT = ShadowsAndPetals.asResource("block/template/wood_pillar");

    private WoodPillarBlockModels() {
    }

    public static void strippedWoodPillar(
            BlockModelContext<? extends WoodPillarBlock> context,
            SAPBlockModelGenerator generator,
            WoodBlockList.WoodType woodType
    ) {
        Block block = context.get();
        Identifier modelId = generator.blockModelId(block);
        Identifier strippedLogId = BuiltInRegistries.BLOCK.getKey(woodType.getStrippedLog());
        Identifier sideTexture = texture(strippedLogId, "");
        Identifier topTexture = texture(strippedLogId, "_top");

        JsonObject model = new JsonObject();
        model.addProperty("parent", PARENT.toString());
        model.addProperty("loader", "neoforge:obj");
        model.addProperty("model", MODEL.toString());
        model.addProperty("flip_v", true);

        JsonObject textures = new JsonObject();
        textures.addProperty("top", topTexture.toString());
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("particle", sideTexture.toString());
        model.add("textures", textures);

        generator.jsonModel(modelId, model);
        generator.blockState(BlockModelGenerators.createAxisAlignedPillarBlock(
                block,
                BlockModelGenerators.plainVariant(modelId)
        ));
        StandardBlockModels.parentBlockItem(block, generator, modelId);
    }

    private static Identifier texture(Identifier strippedLogId, String suffix) {
        return Identifier.fromNamespaceAndPath(
                strippedLogId.getNamespace(),
                "block/" + strippedLogId.getPath() + suffix
        );
    }
}
