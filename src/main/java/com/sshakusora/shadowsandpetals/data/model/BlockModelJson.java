package com.sshakusora.shadowsandpetals.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

public final class BlockModelJson {
    private BlockModelJson() {
    }

    public static JsonObject cuboid(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String sideTexture, String endTexture) {
        JsonObject json = new JsonObject();
        json.add("from", vector(fromX, fromY, fromZ));
        json.add("to", vector(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        faces.add("down", face(endTexture));
        faces.add("up", face(endTexture));
        faces.add("north", face(sideTexture));
        faces.add("south", face(sideTexture));
        faces.add("west", face(sideTexture));
        faces.add("east", face(sideTexture));
        json.add("faces", faces);
        return json;
    }

    public static JsonObject cuboidAll(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String texture) {
        return cuboidAllSelective(fromX, fromY, fromZ, toX, toY, toZ, texture, true, true, true, true, true, true, false, false, false, false);
    }

    public static JsonObject cuboidAllSelective(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            String texture,
            boolean includeDown,
            boolean includeUp,
            boolean includeNorth,
            boolean includeSouth,
            boolean includeWest,
            boolean includeEast,
            boolean cullNorth,
            boolean cullSouth,
            boolean cullWest,
            boolean cullEast
    ) {
        JsonObject json = new JsonObject();
        json.add("from", vector(fromX, fromY, fromZ));
        json.add("to", vector(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        addFace(faces, "down", texture, includeDown, null);
        addFace(faces, "up", texture, includeUp, null);
        addFace(faces, "north", texture, includeNorth, cullNorth ? "north" : null);
        addFace(faces, "south", texture, includeSouth, cullSouth ? "south" : null);
        addFace(faces, "west", texture, includeWest, cullWest ? "west" : null);
        addFace(faces, "east", texture, includeEast, cullEast ? "east" : null);
        json.add("faces", faces);
        return json;
    }

    public static JsonObject chainPlane(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double originY, boolean northSouthFaces) {
        JsonObject json = new JsonObject();
        json.add("from", vector(fromX, fromY, fromZ));
        json.add("to", vector(toX, toY, toZ));
        json.addProperty("shade", false);

        JsonObject rotation = new JsonObject();
        rotation.addProperty("angle", 45);
        rotation.addProperty("axis", "y");
        rotation.add("origin", vector(8.0D, originY, 8.0D));
        json.add("rotation", rotation);

        JsonObject faces = new JsonObject();
        if (northSouthFaces) {
            faces.add("north", faceWithUv("#all", 0.0D, upperLowerUvMin(fromY), 3.0D, upperLowerUvMax(fromY, toY)));
            faces.add("south", faceWithUv("#all", 0.0D, upperLowerUvMin(fromY), 3.0D, upperLowerUvMax(fromY, toY)));
        } else {
            faces.add("east", faceWithUv("#all", 3.0D, upperLowerUvMin(fromY), 6.0D, upperLowerUvMax(fromY, toY)));
            faces.add("west", faceWithUv("#all", 3.0D, upperLowerUvMin(fromY), 6.0D, upperLowerUvMax(fromY, toY)));
        }
        json.add("faces", faces);
        return json;
    }

    public static JsonObject tallItemDisplay() {
        JsonObject display = new JsonObject();
        display.add("thirdperson_righthand", transform(new double[]{75.0D, 45.0D, 0.0D}, new double[]{0.0D, 1.5D, 0.0D}, new double[]{0.375D, 0.375D, 0.375D}));
        display.add("thirdperson_lefthand", transform(new double[]{75.0D, 45.0D, 0.0D}, new double[]{0.0D, 1.5D, 0.0D}, new double[]{0.375D, 0.375D, 0.375D}));
        display.add("firstperson_righthand", transform(new double[]{0.0D, 135.0D, 0.0D}, new double[]{0.0D, 1.0D, 0.0D}, new double[]{0.4D, 0.4D, 0.4D}));
        display.add("firstperson_lefthand", transform(new double[]{0.0D, 135.0D, 0.0D}, new double[]{0.0D, 1.0D, 0.0D}, new double[]{0.4D, 0.4D, 0.4D}));
        display.add("ground", transform(null, new double[]{0.0D, 3.0D, 0.0D}, new double[]{0.25D, 0.25D, 0.25D}));
        display.add("gui", transform(new double[]{30.0D, -135.0D, 0.0D}, new double[]{0.0D, 0.0D, 0.0D}, new double[]{0.65D, 0.65D, 0.65D}));
        display.add("fixed", transform(null, new double[]{0.0D, 0.0D, 0.0D}, new double[]{0.5D, 0.5D, 0.5D}));
        return display;
    }

    public static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    public static JsonArray vector(double a, double b, double c, double d) {
        JsonArray array = new JsonArray();
        array.add(a);
        array.add(b);
        array.add(c);
        array.add(d);
        return array;
    }

    public static JsonObject face(String texture) {
        return face(texture, null);
    }

    public static JsonObject face(String texture, @Nullable String cullface) {
        JsonObject json = new JsonObject();
        json.addProperty("texture", texture);
        if (cullface != null) {
            json.addProperty("cullface", cullface);
        }
        return json;
    }

    public static JsonObject faceWithUv(String texture, double u1, double v1, double u2, double v2) {
        JsonObject json = face(texture);
        json.add("uv", vector(u1, v1, u2, v2));
        return json;
    }

    private static void addFace(JsonObject faces, String name, String texture, boolean include, @Nullable String cullface) {
        if (include) {
            faces.add(name, face(texture, cullface));
        }
    }

    private static JsonObject transform(double[] rotation, double[] translation, double[] scale) {
        JsonObject json = new JsonObject();
        if (rotation != null) {
            json.add("rotation", vector(rotation[0], rotation[1], rotation[2]));
        }
        if (translation != null) {
            json.add("translation", vector(translation[0], translation[1], translation[2]));
        }
        if (scale != null) {
            json.add("scale", vector(scale[0], scale[1], scale[2]));
        }
        return json;
    }

    private static double upperLowerUvMin(double fromY) {
        return fromY <= 0.0D ? 10.0D : 0.0D;
    }

    private static double upperLowerUvMax(double fromY, double toY) {
        return fromY <= 0.0D ? 10.0D + (toY - fromY) : toY - fromY;
    }
}
