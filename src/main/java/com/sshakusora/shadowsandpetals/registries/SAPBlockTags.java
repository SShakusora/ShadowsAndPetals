package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class SAPBlockTags {
    public static final TagKey<Block> SUPPORTS_IRORI_GRILL = TagKey.create(
            Registries.BLOCK,
            ShadowsAndPetals.asResource("supports_irori_grill")
    );

    private SAPBlockTags() {
    }
}
