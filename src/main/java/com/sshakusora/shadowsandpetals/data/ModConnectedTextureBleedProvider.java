package com.sshakusora.shadowsandpetals.data;

import com.google.common.hash.Hashing;
import com.sshakusora.shadowsandpetals.client.ct.CTRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

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

public class ModConnectedTextureBleedProvider implements DataProvider {
    private final PackOutput.PathProvider texturePathProvider;
    private final Path sourceTextureRoot;

    public ModConnectedTextureBleedProvider(PackOutput output) {
        this.texturePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "textures");
        Path projectRoot = output.getOutputFolder().toAbsolutePath().normalize()
                .getParent()
                .getParent()
                .getParent();
        this.sourceTextureRoot = projectRoot.resolve("src/main/resources/assets");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Set<Identifier> scheduledOutputs = new HashSet<>();
        for (CTRegistry.CTEntry entry : CTRegistry.entries().values()) {
            if (entry.padding() <= 0) {
                continue;
            }

            for (Identifier outputTexture : entry.connectedTextures()) {
                if (!scheduledOutputs.add(outputTexture)) {
                    continue;
                }

                Identifier sourceTexture = sourceTexture(outputTexture);
                Path source = sourcePath(sourceTexture);
                Path output = this.texturePathProvider.file(outputTexture, "png");
                int sheetSize = entry.type().getSheetSize();
                int padding = entry.padding();

                tasks.add(CompletableFuture.runAsync(
                        () -> generate(cache, source, output, sourceTexture, sheetSize, padding)));
            }
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "ShadowsAndPetals Connected Texture Bleed";
    }

    private static void generate(CachedOutput cache, Path source, Path output, Identifier sourceTexture,
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

    private static BufferedImage addBleed(BufferedImage source, Identifier sourceTexture, int sheetSize, int padding) {
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

    private static Identifier sourceTexture(Identifier outputTexture) {
        String path = outputTexture.getPath();
        if (!path.endsWith("_bleed")) {
            throw new IllegalArgumentException("Cannot infer connected texture source for " + outputTexture
                    + "; generated bleed textures must end with '_bleed'");
        }
        return Identifier.fromNamespaceAndPath(outputTexture.getNamespace(), path.substring(0, path.length() - "_bleed".length()));
    }

    private Path sourcePath(Identifier texture) {
        return this.sourceTextureRoot
                .resolve(texture.getNamespace())
                .resolve("textures")
                .resolve(texture.getPath() + ".png");
    }
}
