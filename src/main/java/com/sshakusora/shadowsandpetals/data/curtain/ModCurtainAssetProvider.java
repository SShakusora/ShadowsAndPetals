package com.sshakusora.shadowsandpetals.data.curtain;

import com.google.gson.JsonElement;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ModCurtainAssetProvider implements DataProvider {
    private final Path resourceRoot;
    private final Path projectRoot;

    public ModCurtainAssetProvider(PackOutput output) {
        this(output, projectRootFromOutput(output));
    }

    public ModCurtainAssetProvider(PackOutput output, Path projectRoot) {
        this.resourceRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(ShadowsAndPetals.MOD_ID);
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Map<String, JsonElement> assets = new CurtainAssetCompiler(projectRoot).compile();
        List<CompletableFuture<?>> writes = new ArrayList<>(assets.size());
        for (Map.Entry<String, JsonElement> entry : assets.entrySet()) {
            writes.add(DataProvider.saveStable(
                    cache,
                    entry.getValue(),
                    resourceRoot.resolve(entry.getKey())
            ));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Curtain Assets";
    }

    private static Path projectRootFromOutput(PackOutput output) {
        Path outputFolder = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .toAbsolutePath()
                .normalize();
        Path current = outputFolder;
        while (current != null) {
            if (hasCurtainSources(current)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "could not locate ShadowsAndPetals project root from DataGen output folder: " + outputFolder
        );
    }

    private static boolean hasCurtainSources(Path root) {
        Path models = root.resolve("src/main/resources/assets")
                .resolve(ShadowsAndPetals.MOD_ID)
                .resolve("models/block");
        return Files.isDirectory(models.resolve("curtain/source"))
                && Files.isDirectory(models.resolve("large_curtain/source"));
    }
}
