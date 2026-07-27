package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.block.decoration.WindChimeBlock;
import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.ItemModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import com.sshakusora.shadowsandpetals.data.model.SAPItemModelGenerator;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import java.util.Optional;

public final class WindChimeModels {
    private WindChimeModels() {
    }

    public static void block(
            BlockModelContext<? extends WindChimeBlock> context,
            SAPBlockModelGenerator generator
    ) {
        StandardBlockModels.simpleBlock(
                context,
                generator,
                WindChimeColors.blockBodyModelId(WindChimeColors.DEFAULT_COLOR)
        );
        for (DyeColor ribbon : DyeColor.values()) {
            TextureSlot ribbonSlot = TextureSlot.create("2");
            Identifier ribbonTexture = generator.modLoc("block/wind_chime/ribbon/" + ribbon.getName());
            generator.create(
                    new ModelTemplate(
                            Optional.of(generator.modLoc("block/wind_chimes/block")),
                            Optional.empty(),
                            ribbonSlot,
                            TextureSlot.PARTICLE
                    ),
                    WindChimeColors.blockBodyModelId(ribbon),
                    new TextureMapping()
                            .put(ribbonSlot, new Material(ribbonTexture))
                            .put(TextureSlot.PARTICLE, new Material(generator.mcLoc("block/glass")))
            );
            generator.create(
                    new ModelTemplate(
                            Optional.of(generator.modLoc("block/wind_chimes/main_ribbon")),
                            Optional.empty(),
                            ribbonSlot,
                            TextureSlot.PARTICLE
                    ),
                    WindChimeColors.blockMainRibbonModelId(ribbon),
                    new TextureMapping()
                            .put(ribbonSlot, new Material(ribbonTexture))
                            .put(TextureSlot.PARTICLE, new Material(ribbonTexture))
            );
        }
        for (DyeColor vane : DyeColor.values()) {
            TextureSlot vaneSlot = TextureSlot.create("windchime0");
            Identifier vaneTexture = generator.modLoc("block/wind_chime/vane/" + vane.getName());
            generator.create(
                    new ModelTemplate(
                            Optional.of(generator.modLoc("block/wind_chimes/vane")),
                            Optional.empty(),
                            TextureSlot.PARTICLE,
                            vaneSlot
                    ),
                    WindChimeColors.blockVaneModelId(vane),
                    new TextureMapping()
                            .put(TextureSlot.PARTICLE, new Material(vaneTexture))
                            .put(vaneSlot, new Material(vaneTexture))
            );
        }
    }

    public static void item(
            ItemModelContext<? extends Item> context,
            SAPItemModelGenerator generator
    ) {
        generator.parentModel(WindChimeColors.itemBodyModelId(), generator.modLoc("item/wind_chime_body"));
        for (DyeColor ribbon : DyeColor.values()) {
            TextureSlot ribbonSlot = TextureSlot.create("2");
            Identifier ribbonTexture = generator.modLoc("block/wind_chime/ribbon/" + ribbon.getName());
            generator.create(
                    new ModelTemplate(
                            Optional.of(generator.modLoc("item/wind_chime_ribbon")),
                            Optional.empty(),
                            ribbonSlot,
                            TextureSlot.PARTICLE
                    ),
                    WindChimeColors.itemRibbonModelId(ribbon),
                    new TextureMapping()
                            .put(ribbonSlot, new Material(ribbonTexture))
                            .put(TextureSlot.PARTICLE, new Material(ribbonTexture))
            );
        }
        for (DyeColor vane : DyeColor.values()) {
            TextureSlot vaneSlot = TextureSlot.create("windchime0");
            Identifier vaneTexture = generator.modLoc("block/wind_chime/vane/" + vane.getName());
            generator.create(
                    new ModelTemplate(
                            Optional.of(generator.modLoc("item/wind_chime_vane")),
                            Optional.empty(),
                            TextureSlot.PARTICLE,
                            vaneSlot
                    ),
                    WindChimeColors.itemVaneModelId(vane),
                    new TextureMapping()
                            .put(TextureSlot.PARTICLE, new Material(vaneTexture))
                            .put(vaneSlot, new Material(vaneTexture))
            );
        }
    }
}
