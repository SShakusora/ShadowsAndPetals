package com.sshakusora.shadowsandpetals.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class BlockModelTemplates {
    private BlockModelTemplates() {
    }

    public static JsonObject parentModel(Identifier parent) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent.toString());
        return json;
    }

    public static JsonObject cubeAllModel(Identifier texture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cube_all"));
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture.toString());
        json.add("textures", textures);
        return json;
    }

    public static JsonObject cubeColumnModel(Identifier sideTexture, Identifier endTexture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cube_column"));
        JsonObject textures = new JsonObject();
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);
        return json;
    }

    public static JsonObject crossModel(Identifier texture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/cross"));
        JsonObject textures = new JsonObject();
        textures.addProperty("cross", texture.toString());
        json.add("textures", textures);
        json.addProperty("render_type", "cutout");
        return json;
    }

    public static JsonObject hedgeStateModel(Identifier texture, boolean north, boolean east, boolean south, boolean west) {
        JsonObject json = hedgeBaseModel(texture);
        JsonArray elements = new JsonArray();
        elements.add(hedgeCoreModel(north, east, south, west));
        if (north) {
            elements.add(hedgeArmModel(Direction.NORTH));
        }
        if (east) {
            elements.add(hedgeArmModel(Direction.EAST));
        }
        if (south) {
            elements.add(hedgeArmModel(Direction.SOUTH));
        }
        if (west) {
            elements.add(hedgeArmModel(Direction.WEST));
        }
        json.add("elements", elements);
        return json;
    }

    public static JsonObject woodPostCoreModel(Identifier sideTexture, Identifier endTexture) {
        JsonObject json = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", sideTexture.toString());
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(BlockModelJson.cuboid(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D, "#side", "#end"));
        json.add("elements", elements);
        json.add("display", BlockModelJson.tallItemDisplay());
        return json;
    }

    public static JsonObject woodPostLinkModel(Identifier sideTexture, Identifier endTexture, boolean upperHalf) {
        JsonObject json = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", sideTexture.toString());
        textures.addProperty("side", sideTexture.toString());
        textures.addProperty("end", endTexture.toString());
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        double fromY = upperHalf ? 10.0D : 0.0D;
        double toY = upperHalf ? 16.0D : 6.0D;
        elements.add(BlockModelJson.cuboid(6.0D, fromY, 6.0D, 10.0D, toY, 10.0D, "#side", "#end"));
        json.add("elements", elements);
        return json;
    }

    public static JsonObject woodPostChainModel(boolean upperHalf, Identifier chainTexture) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Identifier.withDefaultNamespace("block/block").toString());
        JsonObject textures = new JsonObject();
        textures.addProperty("all", chainTexture.toString());
        textures.addProperty("particle", chainTexture.toString());
        json.add("textures", textures);
        json.addProperty("render_type", "cutout");

        JsonArray elements = new JsonArray();
        double fromY = upperHalf ? 10.0D : 0.0D;
        double toY = upperHalf ? 16.0D : 6.0D;
        double originY = upperHalf ? 18.0D : 8.0D;
        elements.add(BlockModelJson.chainPlane(6.5D, fromY, 8.0D, 9.5D, toY, 8.0D, originY, true));
        elements.add(BlockModelJson.chainPlane(8.0D, fromY, 6.5D, 8.0D, toY, 9.5D, originY, false));
        json.add("elements", elements);
        return json;
    }

    private static JsonObject hedgeCoreModel(boolean north, boolean east, boolean south, boolean west) {
        return BlockModelJson.cuboidAllSelective(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D, "#all", true, true, !north, !south, !west, !east, false, false, false, false);
    }

    private static JsonObject hedgeArmModel(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockModelJson.cuboidAllSelective(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 4.0D, "#all", true, true, true, false, true, true, true, false, false, false);
            case EAST -> BlockModelJson.cuboidAllSelective(12.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D, "#all", true, true, true, true, false, true, false, false, false, true);
            case SOUTH -> BlockModelJson.cuboidAllSelective(4.0D, 0.0D, 12.0D, 12.0D, 16.0D, 16.0D, "#all", true, true, false, true, true, true, false, true, false, false);
            case WEST -> BlockModelJson.cuboidAllSelective(0.0D, 0.0D, 4.0D, 4.0D, 16.0D, 12.0D, "#all", true, true, true, true, true, false, false, false, true, false);
            default -> throw new IllegalArgumentException("Unsupported hedge arm direction: " + direction);
        };
    }

    private static JsonObject hedgeBaseModel(Identifier texture) {
        JsonObject json = parentModel(Identifier.withDefaultNamespace("block/block"));
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture.toString());
        textures.addProperty("particle", texture.toString());
        json.add("textures", textures);
        json.addProperty("render_type", "cutout_mipped");
        return json;
    }
}
