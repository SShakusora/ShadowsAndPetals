package com.sshakusora.shadowsandpetals.data;

import com.google.common.hash.Hashing;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.ct.CTRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ModConnectedTextureProvider implements DataProvider {
    private static final ResourceLocation ISOLATED_RAW_CONCRETE_ITEM_TEXTURE =
            ShadowsAndPetals.asResource("item/isolated_raw_concrete");
    private static final ResourceLocation ISOLATED_RAW_CONCRETE_SOURCE_TEXTURE =
            ShadowsAndPetals.asResource("block/raw_concrete/connected_dense_hole");
    private static final int OMNIDIRECTIONAL_SHEET_SIZE = 8;

    private final PackOutput.PathProvider texturePathProvider;
    private final Path sourceTextureRoot;

    public ModConnectedTextureProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        this.texturePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "textures");
        this.sourceTextureRoot = findSourceTextureRoot(output.getOutputFolder());
        trackGeneratedTextures(existingFileHelper);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Set<ResourceLocation> scheduledOutputs = new HashSet<>();
        Set<ResourceLocation> scheduledBaseOutputs = new HashSet<>();
        Set<ResourceLocation> scheduledCopycatOutputs = new HashSet<>();
        for (CTRegistry.CTEntry entry : CTRegistry.entries().values()) {
            if (entry.padding() <= 0) {
                continue;
            }
            scheduleCopycatTextures(cache, tasks, scheduledCopycatOutputs, entry);

            ResourceLocation baseTexture = entry.baseTexture();
            if (scheduledBaseOutputs.add(baseTexture)) {
                ResourceLocation sourceTexture = sourceTexture(entry.connectedTextures().getFirst());
                Path source = sourcePath(sourceTexture);
                Path output = this.texturePathProvider.file(baseTexture, "png");
                int sheetSize = entry.type().getSheetSize();
                tasks.add(CompletableFuture.runAsync(
                        () -> generateBase(cache, source, output, sourceTexture, sheetSize)));
            }

            for (ResourceLocation outputTexture : entry.connectedTextures()) {
                if (!scheduledOutputs.add(outputTexture)) {
                    continue;
                }

                ResourceLocation sourceTexture = sourceTexture(outputTexture);
                Path source = sourcePath(sourceTexture);
                Path output = this.texturePathProvider.file(outputTexture, "png");
                int sheetSize = entry.type().getSheetSize();
                int padding = entry.padding();

                tasks.add(CompletableFuture.runAsync(
                        () -> generate(cache, source, output, sourceTexture, sheetSize, padding)));
            }
        }

        scheduleIsolatedRawConcreteItemTexture(cache, tasks);
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Connected Textures";
    }

    private static void trackGeneratedTextures(ExistingFileHelper existingFileHelper) {
        ExistingFileHelper.IResourceType textureType =
                new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");
        Set<ResourceLocation> tracked = new HashSet<>();
        for (CTRegistry.CTEntry entry : CTRegistry.entries().values()) {
            if (entry.padding() <= 0) {
                continue;
            }
            if (tracked.add(entry.baseTexture())) {
                existingFileHelper.trackGenerated(entry.baseTexture(), textureType);
            }
            for (ResourceLocation texture : entry.connectedTextures()) {
                if (tracked.add(texture)) {
                    existingFileHelper.trackGenerated(texture, textureType);
                }
            }
            for (int connectedTextureIndex = 0;
                 connectedTextureIndex < entry.connectedTextures().size();
                 connectedTextureIndex++) {
                for (int tileIndex = 0;
                     tileIndex < entry.type().getSheetSize() * entry.type().getSheetSize();
                     tileIndex++) {
                    ResourceLocation copycatTexture = entry.copycatTexture(connectedTextureIndex, tileIndex);
                    if (tracked.add(copycatTexture)) {
                        existingFileHelper.trackGenerated(copycatTexture, textureType);
                    }
                }
            }
        }
        if (tracked.add(ISOLATED_RAW_CONCRETE_ITEM_TEXTURE)) {
            existingFileHelper.trackGenerated(ISOLATED_RAW_CONCRETE_ITEM_TEXTURE, textureType);
        }
    }

    private void scheduleIsolatedRawConcreteItemTexture(
            CachedOutput cache,
            List<CompletableFuture<?>> tasks
    ) {
        Path source = sourcePath(ISOLATED_RAW_CONCRETE_SOURCE_TEXTURE);
        Path output = texturePathProvider.file(ISOLATED_RAW_CONCRETE_ITEM_TEXTURE, "png");
        tasks.add(CompletableFuture.runAsync(
                () -> generateBase(
                        cache,
                        source,
                        output,
                        ISOLATED_RAW_CONCRETE_SOURCE_TEXTURE,
                        OMNIDIRECTIONAL_SHEET_SIZE)));
    }

    private static void generateBase(CachedOutput cache, Path source, Path output,
                                     ResourceLocation sourceTexture, int sheetSize) {
        try {
            if (!Files.isRegularFile(source)) {
                throw new IOException("Missing connected texture source: " + source);
            }

            BufferedImage sourceImage = ImageIO.read(source.toFile());
            if (sourceImage == null) {
                throw new IOException("Unsupported image: " + source);
            }

            BufferedImage baseImage = cropFirstTile(sourceImage, sourceTexture, sheetSize);
            byte[] png = encodePng(baseImage);
            cache.writeIfNeeded(output, png, Hashing.sha256().hashBytes(png));
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private static void generate(CachedOutput cache, Path source, Path output, ResourceLocation sourceTexture,
                                 int sheetSize, int padding) {
        try {
            if (!Files.isRegularFile(source)) {
                throw new IOException("Missing connected texture source: " + source);
            }

            BufferedImage sourceImage = ImageIO.read(source.toFile());
            if (sourceImage == null) {
                throw new IOException("Unsupported image: " + source);
            }

            BufferedImage outputImage = addBleed(sourceImage, sourceTexture, sheetSize, padding);
            byte[] png = encodePng(outputImage);
            cache.writeIfNeeded(output, png, Hashing.sha256().hashBytes(png));
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private static BufferedImage addBleed(BufferedImage source, ResourceLocation sourceTexture, int sheetSize, int padding) {
        if (source.getWidth() != source.getHeight()) {
            throw new IllegalArgumentException(sourceTexture + " must be square, got "
                    + source.getWidth() + "x" + source.getHeight());
        }
        if (source.getWidth() % sheetSize != 0) {
            throw new IllegalArgumentException(sourceTexture + " width must be divisible by sheet size " + sheetSize);
        }

        int tileSize = source.getWidth() / sheetSize;
        int stride = tileSize + padding * 2;
        BufferedImage result = new BufferedImage(stride * sheetSize, stride * sheetSize, BufferedImage.TYPE_INT_ARGB);

        for (int tileY = 0; tileY < sheetSize; tileY++) {
            for (int tileX = 0; tileX < sheetSize; tileX++) {
                copyTileWithBleed(source, result, tileX, tileY, tileSize, stride, padding);
            }
        }

        return result;
    }

    static BufferedImage cropFirstTile(BufferedImage source, ResourceLocation sourceTexture, int sheetSize) {
        if (source.getWidth() != source.getHeight()) {
            throw new IllegalArgumentException(sourceTexture + " must be square, got "
                    + source.getWidth() + "x" + source.getHeight());
        }
        if (source.getWidth() % sheetSize != 0) {
            throw new IllegalArgumentException(sourceTexture + " width must be divisible by sheet size " + sheetSize);
        }

        int tileSize = source.getWidth() / sheetSize;
        BufferedImage result = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                result.setRGB(x, y, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static BufferedImage cropTile(BufferedImage source, ResourceLocation sourceTexture,
                                          int sheetSize, int tileIndex) {
        if (source.getWidth() != source.getHeight()) {
            throw new IllegalArgumentException(sourceTexture + " must be square, got "
                    + source.getWidth() + "x" + source.getHeight());
        }
        if (source.getWidth() % sheetSize != 0) {
            throw new IllegalArgumentException(sourceTexture + " width must be divisible by sheet size " + sheetSize);
        }

        int tileSize = source.getWidth() / sheetSize;
        int tileX = Math.floorMod(tileIndex, sheetSize);
        int tileY = Math.floorDiv(tileIndex, sheetSize);
        BufferedImage result = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                result.setRGB(x, y, source.getRGB(tileX * tileSize + x, tileY * tileSize + y));
            }
        }
        return result;
    }

    private void scheduleCopycatTextures(CachedOutput cache, List<CompletableFuture<?>> tasks,
                                          Set<ResourceLocation> scheduledOutputs,
                                          CTRegistry.CTEntry entry) {
        int sheetSize = entry.type().getSheetSize();
        int tileCount = sheetSize * sheetSize;
        for (int connectedTextureIndex = 0;
             connectedTextureIndex < entry.connectedTextures().size();
             connectedTextureIndex++) {
            ResourceLocation sourceTexture = sourceTexture(entry.connectedTextures().get(connectedTextureIndex));
            Path source = sourcePath(sourceTexture);
            for (int tileIndex = 0; tileIndex < tileCount; tileIndex++) {
                ResourceLocation outputTexture = entry.copycatTexture(connectedTextureIndex, tileIndex);
                if (!scheduledOutputs.add(outputTexture)) {
                    continue;
                }
                Path output = texturePathProvider.file(outputTexture, "png");
                int currentTileIndex = tileIndex;
                tasks.add(CompletableFuture.runAsync(
                        () -> generateCopycatTile(cache, source, output, sourceTexture, sheetSize, currentTileIndex)));
            }
        }
    }

    private static void generateCopycatTile(CachedOutput cache, Path source, Path output,
                                            ResourceLocation sourceTexture, int sheetSize, int tileIndex) {
        try {
            if (!Files.isRegularFile(source)) {
                throw new IOException("Missing connected texture source: " + source);
            }

            BufferedImage sourceImage = ImageIO.read(source.toFile());
            if (sourceImage == null) {
                throw new IOException("Unsupported image: " + source);
            }

            BufferedImage tile = cropTile(sourceImage, sourceTexture, sheetSize, tileIndex);
            byte[] png = encodePng(tile);
            cache.writeIfNeeded(output, png, Hashing.sha256().hashBytes(png));
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private static void copyTileWithBleed(BufferedImage source, BufferedImage result, int tileX, int tileY,
                                          int tileSize, int stride, int padding) {
        for (int y = 0; y < stride; y++) {
            int innerY = Math.clamp(y - padding, 0, tileSize - 1);
            for (int x = 0; x < stride; x++) {
                int innerX = Math.clamp(x - padding, 0, tileSize - 1);
                int color = source.getRGB(tileX * tileSize + innerX, tileY * tileSize + innerY);
                result.setRGB(tileX * stride + x, tileY * stride + y, color);
            }
        }
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static ResourceLocation sourceTexture(ResourceLocation outputTexture) {
        String path = outputTexture.getPath();
        if (!path.endsWith("_bleed")) {
            throw new IllegalArgumentException("Cannot infer connected texture source for " + outputTexture
                    + "; generated bleed textures must end with '_bleed'");
        }
        return ResourceLocation.fromNamespaceAndPath(outputTexture.getNamespace(), path.substring(0, path.length() - "_bleed".length()));
    }

    private Path sourcePath(ResourceLocation texture) {
        return this.sourceTextureRoot
                .resolve(texture.getNamespace())
                .resolve("textures")
                .resolve(texture.getPath() + ".png");
    }

    private static Path findSourceTextureRoot(Path outputFolder) {
        Path cursor = outputFolder.toAbsolutePath().normalize();
        while (cursor != null) {
            Path candidate = cursor.resolve("src/main/resources/assets");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Cannot locate src/main/resources/assets from " + outputFolder);
    }
}
