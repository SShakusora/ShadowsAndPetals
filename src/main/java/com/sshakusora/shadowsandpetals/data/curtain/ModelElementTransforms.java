package com.sshakusora.shadowsandpetals.data.curtain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

final class ModelElementTransforms {
    private ModelElementTransforms() {
    }

    record Pose(double tx, double ty, double tz, double ry) {
        static Pose zero() {
            return new Pose(0.0D, 0.0D, 0.0D, 0.0D);
        }

        Pose plus(Pose other) {
            return new Pose(tx + other.tx, ty + other.ty, tz + other.tz, ry + other.ry);
        }

        boolean isZero() {
            return tx == 0.0D && ty == 0.0D && tz == 0.0D && ry == 0.0D;
        }
    }

    static JsonObject rotateElement(JsonObject element, Pose bonePose, JsonArray pivot) {
        double authoredY = 0.0D;
        if (element.has("rotation") && element.getAsJsonObject("rotation").has("axis")
                && "y".equals(element.getAsJsonObject("rotation").get("axis").getAsString())) {
            authoredY = element.getAsJsonObject("rotation").get("angle").getAsDouble();
        }
        double degrees = bonePose.ry() + authoredY;

        double[][] corners = new double[8][];
        int cornerIndex = 0;
        double[] from = vector(element.getAsJsonArray("from"));
        double[] to = vector(element.getAsJsonArray("to"));
        for (double x : new double[]{from[0], to[0]}) {
            for (double y : new double[]{from[1], to[1]}) {
                for (double z : new double[]{from[2], to[2]}) {
                    corners[cornerIndex++] = rotateYAbout(
                            new double[]{x, y, z}, pivot, degrees);
                }
            }
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (double[] corner : corners) {
            minX = Math.min(minX, corner[0]);
            minY = Math.min(minY, corner[1]);
            minZ = Math.min(minZ, corner[2]);
            maxX = Math.max(maxX, corner[0]);
            maxY = Math.max(maxY, corner[1]);
            maxZ = Math.max(maxZ, corner[2]);
        }

        JsonObject output = new JsonObject();
        if (element.has("name")) {
            output.add("name", element.get("name").deepCopy());
        }
        output.add("from", vector(minX + bonePose.tx(), minY + bonePose.ty(), minZ + bonePose.tz()));
        output.add("to", vector(maxX + bonePose.tx(), maxY + bonePose.ty(), maxZ + bonePose.tz()));

        JsonObject rotation = element.has("rotation") ? element.getAsJsonObject("rotation") : null;
        if (rotation != null && (!"y".equals(rotation.get("axis").getAsString()) || degrees == 0.0D)) {
            output.add("rotation", rotation.deepCopy());
        }

        JsonObject faces = new JsonObject();
        if (degrees % 360.0D != 0.0D) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
                faces.add(rotateDirection(entry.getKey(), degrees), entry.getValue().deepCopy());
            }
        } else {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
                faces.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        output.add("faces", faces);
        return output;
    }

    static JsonArray vector(double[] values) {
        return vector(values[0], values[1], values[2]);
    }

    static JsonArray vector(double x, double y, double z) {
        JsonArray result = new JsonArray();
        result.add(x);
        result.add(y);
        result.add(z);
        return result;
    }

    static double[] vector(JsonArray array) {
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    static double[] rotateYAbout(double[] point, JsonArray pivot, double degrees) {
        double[] pivotVector = vector(pivot);
        double radians = degrees * Math.PI / 180.0D;
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double dx = point[0] - pivotVector[0];
        double dz = point[2] - pivotVector[2];
        return new double[]{
                pivotVector[0] + dx * cos + dz * sin,
                point[1],
                pivotVector[2] - dx * sin + dz * cos
        };
    }

    static String rotateDirection(String direction, double degrees) {
        if ("up".equals(direction) || "down".equals(direction)) {
            return direction;
        }
        Map<String, String> clockwise = new LinkedHashMap<>();
        clockwise.put("north", "east");
        clockwise.put("east", "south");
        clockwise.put("south", "west");
        clockwise.put("west", "north");

        int steps;
        if (degrees > 0.0D) {
            steps = (int) Math.round(degrees / 90.0D) % 4;
        } else {
            steps = (4 - (int) Math.round(-degrees / 90.0D) % 4) % 4;
        }
        String result = direction;
        for (int index = 0; index < steps; index++) {
            result = clockwise.get(result);
        }
        return result;
    }
}
