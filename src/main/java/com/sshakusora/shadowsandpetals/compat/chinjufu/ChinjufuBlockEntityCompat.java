package com.sshakusora.shadowsandpetals.compat.chinjufu;

import com.sshakusora.shadowsandpetals.block.WoodBlockList.WoodType;
import com.sshakusora.shadowsandpetals.blockentity.VanityBlockEntity;
import com.sshakusora.shadowsandpetals.legacy.LegacyBlockAliasRegistry;
import com.sshakusora.shadowsandpetals.registries.builder.RegBlockEntityBuilder;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** Chinjufu-specific block-entity alias wiring. */
public final class ChinjufuBlockEntityCompat {
    private static final WoodType[] TANSU_WOODS = {
            WoodType.ACACIA,
            WoodType.BIRCH,
            WoodType.DARK_OAK,
            WoodType.GINKGO,
            WoodType.JUNGLE,
            WoodType.MAPLE,
            WoodType.OAK,
            WoodType.SAKURA,
            WoodType.SPRUCE
    };

    private ChinjufuBlockEntityCompat() {}

    public static RegBlockEntityBuilder<VanityBlockEntity> vanityDataAliases(
            RegBlockEntityBuilder<VanityBlockEntity> builder
    ) {
        @SuppressWarnings("unchecked")
        Supplier<? extends Block>[] legacyBlocks = new Supplier[TANSU_WOODS.length];
        for (int index = 0; index < TANSU_WOODS.length; index++) {
            legacyBlocks[index] = legacyBlock(TANSU_WOODS[index]);
        }

        return builder.dataAlias(
                ChinjufuIds.MOD_ID,
                "tansu",
                ChinjufuDataConverters::tansuToVanity,
                legacyBlocks
        );
    }

    private static Supplier<? extends Block> legacyBlock(WoodType woodType) {
        String path = ChinjufuIds.tansuBlockPath(woodType);
        if (path == null) {
            throw new IllegalArgumentException("No Chinjufu Tansu alias for " + woodType);
        }
        return LegacyBlockAliasRegistry.require(ChinjufuIds.id(path));
    }
}
