package com.sshakusora.shadowsandpetals.dev;

import com.mojang.logging.LogUtils;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class StructureEditorLayout {
    public static final int SPAWN_Y = 65;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EDITOR_PROPERTY = "shadowsandpetals.structureEditor";
    private static final BlockPos SPAWN = new BlockPos(0, SPAWN_Y, 0);
    private static final int CELL_PADDING = 10;
    private static final int LAYOUT_START = 10;
    private static final int FLOOR_Y = SPAWN_Y - 1;

    private StructureEditorLayout() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!isStructureEditor()) {
            return;
        }

        MinecraftServer server = event.getServer();
        Path marker = server.getWorldPath(LevelResource.ROOT).resolve(StructureEditorWorldBootstrap.REBUILD_MARKER);
        if (!Files.exists(marker)) {
            return;
        }

        ServerLevel level = server.overworld();
        try {
            rebuild(level, server);
            Files.deleteIfExists(marker);
        } catch (Exception exception) {
            LOGGER.error("Failed to build the structure editor layout", exception);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!isStructureEditor() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.teleportTo((ServerLevel)player.level(), SPAWN.getX() + 0.5, SPAWN.getY(), SPAWN.getZ() + 0.5, java.util.Set.of(), 0.0F, 0.0F, true);
    }

    private static boolean isStructureEditor() {
        return Boolean.getBoolean(EDITOR_PROPERTY);
    }

    private static void rebuild(ServerLevel level, MinecraftServer server) throws IOException {
        List<TemplateEntry> templates = loadTemplates(server);
        if (templates.isEmpty()) {
            LOGGER.warn("No {} structure templates were found for the structure editor", ShadowsAndPetals.MOD_ID);
            return;
        }

        int maxWidth = templates.stream().mapToInt(entry -> entry.template().getSize().getX()).max().orElse(1);
        int maxDepth = templates.stream().mapToInt(entry -> entry.template().getSize().getZ()).max().orElse(1);
        int columns = Math.max(1, (int)Math.ceil(Math.sqrt(templates.size())));
        int rows = (templates.size() + columns - 1) / columns;
        int cellWidth = maxWidth + CELL_PADDING;
        int cellDepth = maxDepth + CELL_PADDING;

        int maxX = LAYOUT_START + columns * cellWidth;
        int maxZ = LAYOUT_START + rows * cellDepth;
        fillFloor(level, -4, maxX, -4, maxZ);

        for (int index = 0; index < templates.size(); index++) {
            TemplateEntry entry = templates.get(index);
            Vec3i size = entry.template().getSize();
            if (size.getX() > StructureBlockEntity.MAX_SIZE_PER_AXIS
                    || size.getY() > StructureBlockEntity.MAX_SIZE_PER_AXIS
                    || size.getZ() > StructureBlockEntity.MAX_SIZE_PER_AXIS) {
                LOGGER.warn("Skipping structure {} because its size {} exceeds the structure block limit", entry.id(), size);
                continue;
            }

            int column = index % columns;
            int row = index / columns;
            BlockPos templateOrigin = new BlockPos(
                    LAYOUT_START + column * cellWidth + (maxWidth - size.getX()) / 2,
                    SPAWN_Y,
                    LAYOUT_START + row * cellDepth + (maxDepth - size.getZ()) / 2
            );

            entry.template().placeInWorld(
                    level,
                    templateOrigin,
                    templateOrigin,
                    new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(false),
                    level.getRandom(),
                    Block.UPDATE_ALL
            );
            placeSaveBlock(level, entry.id(), templateOrigin, size);
        }

        level.setRespawnData(LevelData.RespawnData.of(level.dimension(), SPAWN, 0.0F, 0.0F));
        LOGGER.info("Placed {} structure templates in the structure editor world", templates.size());
    }

    private static List<TemplateEntry> loadTemplates(MinecraftServer server) {
        List<TemplateEntry> result = new ArrayList<>();
        server.getStructureManager().listTemplates()
                .filter(id -> id.getNamespace().equals(ShadowsAndPetals.MOD_ID))
                .sorted(Comparator.comparing(Identifier::toString))
                .forEach(id -> server.getStructureManager().get(id)
                        .ifPresent(template -> result.add(new TemplateEntry(id, template))));
        return result;
    }

    private static void fillFloor(ServerLevel level, int minX, int maxX, int minZ, int maxZ) {
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(new BlockPos(x, FLOOR_Y, z), floor, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void placeSaveBlock(ServerLevel level, Identifier id, BlockPos templateOrigin, Vec3i templateSize) {
        BlockPos saveBlockPos = templateOrigin.offset(-2, 0, 0);
        BlockState saveBlockState = Blocks.STRUCTURE_BLOCK.defaultBlockState()
                .setValue(StructureBlock.MODE, StructureMode.SAVE);
        level.setBlock(saveBlockPos, saveBlockState, Block.UPDATE_ALL);

        if (!(level.getBlockEntity(saveBlockPos) instanceof StructureBlockEntity structureBlock)) {
            LOGGER.warn("Failed to create save block for structure {}", id);
            return;
        }

        structureBlock.setStructureName(id);
        structureBlock.setStructurePos(templateOrigin.subtract(saveBlockPos));
        structureBlock.setStructureSize(templateSize);
        structureBlock.setIgnoreEntities(true);
        structureBlock.setMode(StructureMode.SAVE);
        structureBlock.setChanged();
        level.sendBlockUpdated(saveBlockPos, saveBlockState, saveBlockState, Block.UPDATE_ALL);
    }

    private record TemplateEntry(Identifier id, StructureTemplate template) {
    }
}
