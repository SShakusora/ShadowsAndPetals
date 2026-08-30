package com.sshakusora.shadowsandpetals.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodSetList;
import com.sshakusora.shadowsandpetals.block.decoration.bonsai.BonsaiTreeResolver;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Generates the data-pack mappings consumed by {@link BonsaiTreeResolver}. */
public final class ModBonsaiTreeProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public ModBonsaiTreeProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "bonsai_trees");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();

        add(tasks, cache, "oak", Blocks.OAK_SAPLING, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        add(tasks, cache, "spruce", Blocks.SPRUCE_SAPLING, Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES);
        add(tasks, cache, "birch", Blocks.BIRCH_SAPLING, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
        add(tasks, cache, "jungle", Blocks.JUNGLE_SAPLING, Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
        add(tasks, cache, "acacia", Blocks.ACACIA_SAPLING, Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES);
        add(tasks, cache, "dark_oak", Blocks.DARK_OAK_SAPLING, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
        add(tasks, cache, "cherry", Blocks.CHERRY_SAPLING, Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
        add(tasks, cache, "mangrove", Blocks.MANGROVE_PROPAGULE, Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES);
        add(tasks, cache, "pale_oak", Blocks.PALE_OAK_SAPLING, Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_LEAVES);

        add(tasks, cache, "sakura", BlockRegistry.SAKURA_SET);
        add(tasks, cache, "maple", BlockRegistry.MAPLE_SET);
        add(tasks, cache, "ginkgo", BlockRegistry.GINKGO_SET);
        add(tasks, cache, "autumn_oak", BlockRegistry.AUTUMN_OAK_SAPLING.get(), Blocks.OAK_LOG,
                BlockRegistry.AUTUMN_OAK_LEAVES.get());

        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Bonsai Tree Mappings";
    }

    private void add(
            List<CompletableFuture<?>> tasks,
            CachedOutput cache,
            String name,
            Block sapling,
            Block trunk,
            Block leaves
    ) {
        BonsaiTreeResolver.Definition definition = new BonsaiTreeResolver.Definition(
                key(sapling),
                key(trunk),
                Optional.of(key(leaves))
        );
        JsonElement json = BonsaiTreeResolver.DEFINITION_CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .getOrThrow(error -> new IllegalStateException("Failed to encode bonsai mapping " + name + ": " + error));
        tasks.add(DataProvider.saveStable(cache, json, pathProvider.json(ShadowsAndPetals.asResource(name))));
    }

    private void add(
            List<CompletableFuture<?>> tasks,
            CachedOutput cache,
            String name,
            WoodSetList.WoodSet woodSet
    ) {
        add(tasks, cache, name, woodSet.sapling().get(), woodSet.log().get(), woodSet.leaves().get());
    }

    private static Identifier key(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }
}
