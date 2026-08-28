package com.sshakusora.shadowsandpetals.data.rockery.obj;

import java.util.*;

import static com.sshakusora.shadowsandpetals.data.rockery.obj.ObjModel.*;

/** Extracts the cuboid topology retained by Blockbench's OBJ exporter. */
public final class ObjModelOutline {
    private static final double EPSILON = 1.0E-5D;
    private static final int CUBOID_VERTEX_COUNT = 8;
    private static final int CUBOID_FACE_MIN = 4;
    private static final int CUBOID_FACE_MAX = 6;
    private static final int CUBOID_EDGE_COUNT = 12;

    private ObjModelOutline() {
    }

    public static List<Cuboid> extract(ObjModel model) {
        Map<Integer, List<ObjFace>> facesByObject = new HashMap<>();
        for (ObjFace face : model.faces()) {
            facesByObject.computeIfAbsent(face.objectIndex(), ignored -> new ArrayList<>()).add(face);
        }

        List<Cuboid> cuboids = new ArrayList<>();
        for (ObjObject object : model.objects()) {
            if (object.positions().isEmpty()) {
                continue;
            }
            List<ObjFace> faces = facesByObject.get(object.index());
            if (faces == null || faces.isEmpty()) {
                throw invalid(object, "has vertices but no faces");
            }
            cuboids.add(extractCuboid(object, faces));
        }
        if (cuboids.isEmpty()) {
            throw new IllegalArgumentException("OBJ model contains no cuboid objects");
        }
        return List.copyOf(cuboids);
    }

    private static Cuboid extractCuboid(ObjObject object, List<ObjFace> sourceFaces) {
        List<ObjVector3> vertices = object.positions();
        if (vertices.size() != CUBOID_VERTEX_COUNT) {
            throw invalid(object, "has " + vertices.size() + " vertices; expected 8");
        }
        ensureUniqueVertices(object, vertices);

        List<Face> faces = new ArrayList<>(sourceFaces.size());
        Set<String> uniqueFaces = new HashSet<>();
        if (sourceFaces.size() < CUBOID_FACE_MIN || sourceFaces.size() > CUBOID_FACE_MAX) {
            throw invalid(object, "has " + sourceFaces.size() + " faces; expected 4 to 6");
        }
        for (ObjFace sourceFace : sourceFaces) {
            if (sourceFace.vertices().size() != 4) {
                throw invalid(object, "contains a non-quad face");
            }
            int[] indices = new int[4];
            for (int index = 0; index < indices.length; index++) {
                indices[index] = findVertex(vertices, sourceFace.vertices().get(index).position());
            }
            ensureDistinctFace(indices, object);
            String faceKey = canonicalFaceKey(indices);
            if (!uniqueFaces.add(faceKey)) {
                throw invalid(object, "contains a duplicate face");
            }
            faces.add(new Face(indices[0], indices[1], indices[2], indices[3]));
        }

        List<DirectionLength> directions = collectDirections(vertices, faces, object);
        if (directions.size() != 3) {
            throw invalid(object, "does not have three orthogonal edge directions");
        }
        List<Edge> edges = collectEdges(vertices, directions, object);
        if (edges.size() != CUBOID_EDGE_COUNT) {
            throw invalid(object, "has " + edges.size() + " inferred edges; expected 12");
        }
        int[] degrees = new int[vertices.size()];
        for (Edge edge : edges) {
            degrees[edge.first()]++;
            degrees[edge.second()]++;
        }
        for (int degree : degrees) {
            if (degree != 3) {
                throw invalid(object, "does not form a cuboid graph");
            }
        }
        return new Cuboid(vertices, faces, edges);
    }

    private static void ensureUniqueVertices(ObjObject object, List<ObjVector3> vertices) {
        for (int first = 0; first < vertices.size(); first++) {
            for (int second = first + 1; second < vertices.size(); second++) {
                if (distanceSquared(vertices.get(first), vertices.get(second)) <= EPSILON * EPSILON) {
                    throw invalid(object, "contains duplicate vertices");
                }
            }
        }
    }

    private static List<DirectionLength> collectDirections(
            List<ObjVector3> vertices,
            List<Face> faces,
            ObjObject object
    ) {
        List<DirectionLength> directions = new ArrayList<>(3);
        for (Face face : faces) {
            for (int corner = 0; corner < 4; corner++) {
                ObjVector3 first = vertices.get(face.corners()[corner]);
                ObjVector3 second = vertices.get(face.corners()[(corner + 1) % 4]);
                double length = Math.sqrt(distanceSquared(first, second));
                if (length <= EPSILON) {
                    throw invalid(object, "contains a zero-length edge");
                }
                ObjVector3 direction = second.subtract(first).normalized();
                if (directions.stream().noneMatch(existing -> existing.matches(direction, length))) {
                    if (directions.size() == 3) {
                        throw invalid(object, "has more than three edge directions");
                    }
                    directions.add(new DirectionLength(direction, length));
                }
            }
        }
        return directions;
    }

    private static List<Edge> collectEdges(
            List<ObjVector3> vertices,
            List<DirectionLength> directions,
            ObjObject object
    ) {
        List<Edge> edges = new ArrayList<>(CUBOID_EDGE_COUNT);
        for (int first = 0; first < vertices.size(); first++) {
            for (int second = first + 1; second < vertices.size(); second++) {
                ObjVector3 delta = vertices.get(second).subtract(vertices.get(first));
                double length = Math.sqrt(distanceSquared(vertices.get(first), vertices.get(second)));
                if (length <= EPSILON) {
                    continue;
                }
                ObjVector3 direction = delta.normalized();
                if (directions.stream().anyMatch(candidate -> candidate.matches(direction, length))) {
                    edges.add(new Edge(first, second));
                }
            }
        }
        if (edges.size() > CUBOID_EDGE_COUNT) {
            throw invalid(object, "has ambiguous cuboid edges");
        }
        return edges;
    }

    private static int findVertex(List<ObjVector3> vertices, ObjVector3 target) {
        for (int index = 0; index < vertices.size(); index++) {
            if (distanceSquared(vertices.get(index), target) <= EPSILON * EPSILON) {
                return index;
            }
        }
        throw new IllegalArgumentException("OBJ face references a vertex outside its object");
    }

    private static void ensureDistinctFace(int[] indices, ObjObject object) {
        for (int first = 0; first < indices.length; first++) {
            for (int second = first + 1; second < indices.length; second++) {
                if (indices[first] == indices[second]) {
                    throw invalid(object, "contains a degenerate face");
                }
            }
        }
    }

    private static String canonicalFaceKey(int[] indices) {
        int[] sorted = indices.clone();
        java.util.Arrays.sort(sorted);
        return java.util.Arrays.toString(sorted);
    }

    private static double distanceSquared(ObjVector3 first, ObjVector3 second) {
        double x = first.x() - second.x();
        double y = first.y() - second.y();
        double z = first.z() - second.z();
        return x * x + y * y + z * z;
    }

    private static IllegalArgumentException invalid(ObjObject object, String reason) {
        return new IllegalArgumentException("OBJ object '" + object.name() + "' " + reason);
    }

    public record Cuboid(List<ObjVector3> vertices, List<Face> faces, List<Edge> edges) {
        public Cuboid {
            vertices = List.copyOf(vertices);
            faces = List.copyOf(faces);
            edges = List.copyOf(edges);
        }
    }

    public record Face(int first, int second, int third, int fourth) {
        public int[] corners() {
            return new int[]{first, second, third, fourth};
        }
    }

    public record Edge(int first, int second) {
    }

    private record DirectionLength(ObjVector3 direction, double length) {
        private boolean matches(ObjVector3 otherDirection, double otherLength) {
            return Math.abs(length - otherLength) <= EPSILON * Math.max(1.0D, Math.max(length, otherLength))
                    && Math.abs(Math.abs(direction.x() * otherDirection.x()
                    + direction.y() * otherDirection.y()
                    + direction.z() * otherDirection.z()) - 1.0D) <= 1.0E-4D;
        }
    }
}
