package com.sshakusora.shadowsandpetals.data.rockery.obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A small, data-generator-only representation of an OBJ mesh.
 *
 * <p>The model deliberately keeps vertex attributes on face corners instead
 * of trying to preserve OBJ's separate index streams.  That makes clipping
 * and interpolation deterministic, and the writer can emit a valid indexed
 * stream again without changing the rendered geometry.</p>
 */
public record ObjModel(
        List<ObjFace> faces,
        List<ObjObject> objects,
        Set<String> materials
) {
    public ObjModel {
        faces = List.copyOf(faces);
        objects = List.copyOf(objects);
        materials = Set.copyOf(materials);
    }

    public Bounds bounds() {
        BoundsBuilder builder = new BoundsBuilder();
        for (ObjObject object : objects) {
            for (ObjVector3 position : object.positions()) {
                builder.include(position);
            }
        }
        if (builder.isEmpty()) {
            for (ObjFace face : faces) {
                for (ObjVertex vertex : face.vertices()) {
                    builder.include(vertex.position());
                }
            }
        }
        return builder.build();
    }

    public ObjModel translated(double x, double y, double z) {
        List<ObjFace> translatedFaces = new ArrayList<>(faces.size());
        for (ObjFace face : faces) {
            List<ObjVertex> vertices = new ArrayList<>(face.vertices().size());
            for (ObjVertex vertex : face.vertices()) {
                vertices.add(vertex.withPosition(vertex.position().add(x, y, z)));
            }
            translatedFaces.add(face.withVertices(vertices));
        }

        List<ObjObject> translatedObjects = new ArrayList<>(objects.size());
        for (ObjObject object : objects) {
            List<ObjVector3> positions = object.positions().stream()
                    .map(position -> position.add(x, y, z))
                    .toList();
            translatedObjects.add(object.withPositions(positions));
        }

        return new ObjModel(translatedFaces, translatedObjects, materials);
    }

    public record ObjVertex(ObjVector3 position, ObjVector2 uv, ObjVector3 normal) {
        public ObjVertex withPosition(ObjVector3 value) {
            return new ObjVertex(value, uv, normal);
        }

    }

    public record ObjFace(
            int objectIndex,
            String objectName,
            String material,
            List<ObjVertex> vertices
    ) {
        public ObjFace {
            vertices = List.copyOf(vertices);
        }

        public ObjFace withVertices(List<ObjVertex> value) {
            return new ObjFace(objectIndex, objectName, material, value);
        }
    }

    public record ObjObject(int index, String name, List<ObjVector3> positions) {
        public ObjObject {
            positions = List.copyOf(positions);
        }

        public ObjObject withPositions(List<ObjVector3> value) {
            return new ObjObject(index, name, value);
        }
    }

    public record ObjVector2(double x, double y) {
        public ObjVector2 lerp(ObjVector2 other, double delta) {
            return new ObjVector2(
                    x + (other.x - x) * delta,
                    y + (other.y - y) * delta
            );
        }
    }

    public record ObjVector3(double x, double y, double z) {
        public ObjVector3 add(double dx, double dy, double dz) {
            return new ObjVector3(x + dx, y + dy, z + dz);
        }

        public ObjVector3 subtract(ObjVector3 other) {
            return new ObjVector3(x - other.x, y - other.y, z - other.z);
        }

        public ObjVector3 cross(ObjVector3 other) {
            return new ObjVector3(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }

        public ObjVector3 lerp(ObjVector3 other, double delta) {
            return new ObjVector3(
                    x + (other.x - x) * delta,
                    y + (other.y - y) * delta,
                    z + (other.z - z) * delta
            );
        }

        public ObjVector3 normalized() {
            double length = Math.sqrt(x * x + y * y + z * z);
            return length <= 1.0E-9D
                    ? new ObjVector3(0.0D, 1.0D, 0.0D)
                    : new ObjVector3(x / length, y / length, z / length);
        }
    }

    public record Bounds(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        public Bounds intersection(Bounds other) {
            Bounds result = new Bounds(
                    Math.max(minX, other.minX),
                    Math.max(minY, other.minY),
                    Math.max(minZ, other.minZ),
                    Math.min(maxX, other.maxX),
                    Math.min(maxY, other.maxY),
                    Math.min(maxZ, other.maxZ)
            );
            return result.isEmpty() ? null : result;
        }

        public Bounds translated(double x, double y, double z) {
            return new Bounds(
                    minX + x, minY + y, minZ + z,
                    maxX + x, maxY + y, maxZ + z
            );
        }

        public boolean isEmpty() {
            return maxX - minX <= 1.0E-9D
                    || maxY - minY <= 1.0E-9D
                    || maxZ - minZ <= 1.0E-9D;
        }
    }

    private static final class BoundsBuilder {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(ObjVector3 position) {
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            minZ = Math.min(minZ, position.z());
            maxX = Math.max(maxX, position.x());
            maxY = Math.max(maxY, position.y());
            maxZ = Math.max(maxZ, position.z());
        }

        private boolean isEmpty() {
            return minX == Double.POSITIVE_INFINITY;
        }

        private Bounds build() {
            if (isEmpty()) {
                throw new IllegalArgumentException("OBJ model does not contain any vertices");
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
