package com.sshakusora.shadowsandpetals.legacy;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

public final class LegacyCompatIds {
    public static final String LEGACY_BLOCK_PREFIX = "lcb_";
    public static final String LEGACY_BLOCK_ENTITY_PREFIX = "lcbe_";

    private LegacyCompatIds() {}

    public static String blockName(String targetName, ResourceLocation aliasId, int index) {
        return buildName(LEGACY_BLOCK_PREFIX, targetName, aliasId, index);
    }

    public static String blockEntityName(String targetName, ResourceLocation aliasId, int index) {
        return buildName(LEGACY_BLOCK_ENTITY_PREFIX, targetName, aliasId, index);
    }

    public static boolean isLegacyCompatId(ResourceLocation id) {
        String path = id.getPath();
        return path.startsWith(LEGACY_BLOCK_PREFIX) || path.startsWith(LEGACY_BLOCK_ENTITY_PREFIX);
    }

    public static boolean shouldHideFromSuggestions(ResourceLocation id) {
        return isLegacyCompatId(id);
    }

    private static String buildName(String prefix, String targetName, ResourceLocation aliasId, int index) {
        String hash = Integer.toUnsignedString(Objects.hash(targetName, aliasId, index), 36)
                .toLowerCase(Locale.ROOT);
        return prefix + hash;
    }
}
