package com.sshakusora.shadowsandpetals.dev;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Build-time bootstrap used by the {@code runStructureEditor} Gradle task.
 */
public final class StructureEditorWorldBootstrap {
    public static final String WORLD_DIRECTORY = "sap_structure_editor";
    public static final String REBUILD_MARKER = ".sap-structure-editor-rebuild";
    private static final String FINGERPRINT_FILE = ".sap-structure-editor-source.sha256";
    private static final String WORLD_NAME = "Shadows & Petals Structure Editor";
    private static final int STORAGE_VERSION = 19133;

    private StructureEditorWorldBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected arguments: <game directory> <source structure directory>");
        }

        Path gameDirectory = Path.of(args[0]).toAbsolutePath().normalize();
        Path sourceStructures = Path.of(args[1]).toAbsolutePath().normalize();
        Path worldDirectory = gameDirectory.resolve("saves").resolve(WORLD_DIRECTORY).normalize();
        verifyInside(gameDirectory, worldDirectory);

        Files.createDirectories(sourceStructures);
        Files.createDirectories(worldDirectory);
        ensureWorldFiles(worldDirectory);
        ensureStructureJunction(worldDirectory, sourceStructures);

        String fingerprint = fingerprintStructures(sourceStructures);
        Path fingerprintFile = worldDirectory.resolve(FINGERPRINT_FILE);
        String previousFingerprint = Files.exists(fingerprintFile)
                ? Files.readString(fingerprintFile, StandardCharsets.UTF_8).trim()
                : "";

        if (!fingerprint.equals(previousFingerprint)) {
            clearGeneratedLayout(worldDirectory);
            Files.writeString(worldDirectory.resolve(REBUILD_MARKER), fingerprint + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.writeString(fingerprintFile, fingerprint + System.lineSeparator(), StandardCharsets.UTF_8);
            System.out.println("Structure templates changed; the editor layout will be rebuilt.");
        } else {
            System.out.println("Structure templates are unchanged; keeping the existing editor layout.");
        }
    }

    private static void ensureWorldFiles(Path worldDirectory) throws IOException {
        Path levelDat = worldDirectory.resolve("level.dat");
        if (!Files.exists(levelDat)) {
            NbtIo.writeCompressed(createLevelData(), levelDat);
        }

        Path worldGenSettings = worldDirectory.resolve("data").resolve("minecraft").resolve("world_gen_settings.dat");
        if (!Files.exists(worldGenSettings)) {
            Files.createDirectories(worldGenSettings.getParent());
            NbtIo.writeCompressed(createWorldGenSettings(), worldGenSettings);
        }
    }

    @SuppressWarnings("deprecation")
    private static CompoundTag createLevelData() {
        int dataVersion = SharedConstants.WORLD_VERSION;
        CompoundTag root = new CompoundTag();
        CompoundTag data = new CompoundTag();
        root.put("Data", data);

        CompoundTag difficulty = new CompoundTag();
        difficulty.putString("difficulty", "peaceful");
        difficulty.putBoolean("hardcore", false);
        difficulty.putBoolean("locked", false);
        data.put("difficulty_settings", difficulty);

        data.putLong("Time", 6000L);
        data.putInt("GameType", 1);
        data.putInt("version", STORAGE_VERSION);
        data.putLong("LastPlayed", Instant.now().toEpochMilli());
        data.putString("LevelName", WORLD_NAME);
        data.putBoolean("initialized", true);
        data.putBoolean("WasModded", true);
        data.putInt("DataVersion", dataVersion);
        data.putBoolean("allowCommands", true);
        data.putBoolean("confirmedExperimentalSettings", false);
        data.putFloat("neoDayTimeFraction", 0.0F);
        data.putFloat("neoDayTimePerTick", -1.0F);
        data.putString("forgeLifecycle", "stable");

        ListTag serverBrands = new ListTag();
        serverBrands.add(StringTag.valueOf("neoforge"));
        data.put("ServerBrands", serverBrands);

        CompoundTag spawn = new CompoundTag();
        spawn.putIntArray("pos", new int[]{0, StructureEditorLayout.SPAWN_Y, 0});
        spawn.putString("dimension", "minecraft:overworld");
        spawn.putFloat("yaw", 0.0F);
        spawn.putFloat("pitch", 0.0F);
        data.put("spawn", spawn);

        CompoundTag version = new CompoundTag();
        version.putBoolean("Snapshot", false);
        version.putString("Series", "main");
        version.putInt("Id", dataVersion);
        version.putString("Name", "26.1.2");
        data.put("Version", version);

        CompoundTag dataPacks = new CompoundTag();
        dataPacks.put("Enabled", stringList("vanilla", "mod_data"));
        dataPacks.put("Disabled", stringList("minecart_improvements", "redstone_experiments", "trade_rebalance"));
        data.put("DataPacks", dataPacks);
        return root;
    }

    @SuppressWarnings("deprecation")
    private static CompoundTag createWorldGenSettings() {
        CompoundTag root = new CompoundTag();
        CompoundTag data = new CompoundTag();
        root.put("data", data);
        data.putBoolean("bonus_chest", false);
        data.putLong("seed", 0L);
        data.putBoolean("generate_structures", false);
        data.put("dimensions", createDimensions());
        root.putInt("DataVersion", SharedConstants.WORLD_VERSION);
        return root;
    }

    private static CompoundTag createDimensions() {
        CompoundTag dimensions = new CompoundTag();

        CompoundTag flatSettings = new CompoundTag();
        flatSettings.putBoolean("features", false);
        flatSettings.putString("biome", "minecraft:the_void");
        flatSettings.put("layers", new ListTag());
        flatSettings.put("structure_overrides", new ListTag());
        flatSettings.putBoolean("lakes", false);

        CompoundTag overworldGenerator = new CompoundTag();
        overworldGenerator.put("settings", flatSettings);
        overworldGenerator.putString("type", "minecraft:flat");
        CompoundTag overworld = new CompoundTag();
        overworld.put("generator", overworldGenerator);
        overworld.putString("type", "minecraft:overworld");
        dimensions.put("minecraft:overworld", overworld);

        CompoundTag netherBiomeSource = new CompoundTag();
        netherBiomeSource.putString("preset", "minecraft:nether");
        netherBiomeSource.putString("type", "minecraft:multi_noise");
        CompoundTag netherGenerator = new CompoundTag();
        netherGenerator.putString("settings", "minecraft:nether");
        netherGenerator.put("biome_source", netherBiomeSource);
        netherGenerator.putString("type", "minecraft:noise");
        CompoundTag nether = new CompoundTag();
        nether.put("generator", netherGenerator);
        nether.putString("type", "minecraft:the_nether");
        dimensions.put("minecraft:the_nether", nether);

        CompoundTag endBiomeSource = new CompoundTag();
        endBiomeSource.putString("type", "minecraft:the_end");
        CompoundTag endGenerator = new CompoundTag();
        endGenerator.putString("settings", "minecraft:end");
        endGenerator.put("biome_source", endBiomeSource);
        endGenerator.putString("type", "minecraft:noise");
        CompoundTag end = new CompoundTag();
        end.put("generator", endGenerator);
        end.putString("type", "minecraft:the_end");
        dimensions.put("minecraft:the_end", end);
        return dimensions;
    }

    private static ListTag stringList(String... values) {
        ListTag result = new ListTag();
        for (String value : values) {
            result.add(StringTag.valueOf(value));
        }
        return result;
    }

    private static void ensureStructureJunction(Path worldDirectory, Path sourceStructures) throws IOException, InterruptedException {
        Path namespaceDirectory = worldDirectory.resolve("generated").resolve("shadowsandpetals");
        Path link = namespaceDirectory.resolve("structure");
        Files.createDirectories(namespaceDirectory);

        if (Files.exists(link)) {
            if (Files.isSameFile(link, sourceStructures)) {
                return;
            }
            throw new IOException("The editor structure path already exists and does not point to the source directory: " + link);
        }
        if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("A broken structure link already exists: " + link);
        }

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "mklink", "/J", link.toString(), sourceStructures.toString()
            ).inheritIO().start();
            if (process.waitFor() != 0) {
                throw new IOException("Failed to create structure directory junction: " + link);
            }
        } else {
            Files.createSymbolicLink(link, sourceStructures);
        }
    }

    private static String fingerprintStructures(Path sourceStructures) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<Path> structures;
        try (var paths = Files.walk(sourceStructures)) {
            structures = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".nbt"))
                    .sorted(Comparator.comparing(path -> sourceStructures.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }

        for (Path structure : structures) {
            String relativePath = sourceStructures.relativize(structure).toString().replace('\\', '/');
            digest.update(relativePath.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Files.readAllBytes(structure));
            digest.update((byte) 0);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void clearGeneratedLayout(Path worldDirectory) throws IOException {
        List<Path> generatedChunkDirectories = List.of(
                worldDirectory.resolve("region"),
                worldDirectory.resolve("entities"),
                worldDirectory.resolve("poi"),
                worldDirectory.resolve("dimensions/minecraft/overworld/region"),
                worldDirectory.resolve("dimensions/minecraft/overworld/entities"),
                worldDirectory.resolve("dimensions/minecraft/overworld/poi")
        );

        for (Path directory : generatedChunkDirectories) {
            verifyInside(worldDirectory, directory);
            deleteRecursively(directory);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void verifyInside(Path parent, Path child) throws IOException {
        Path normalizedParent = parent.toAbsolutePath().normalize();
        Path normalizedChild = child.toAbsolutePath().normalize();
        if (!normalizedChild.startsWith(normalizedParent) || normalizedChild.equals(normalizedParent)) {
            throw new IOException("Refusing to operate outside the expected directory: " + normalizedChild);
        }
    }
}
