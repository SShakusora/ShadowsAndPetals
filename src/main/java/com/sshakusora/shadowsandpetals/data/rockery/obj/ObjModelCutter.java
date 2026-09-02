package com.sshakusora.shadowsandpetals.data.rockery.obj;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static com.sshakusora.shadowsandpetals.data.rockery.obj.ObjModel.*;

/** Normalizes and clips an OBJ into the individual blocks of a rockery. */
public final class ObjModelCutter {
    private static final double EPSILON = 1.0E-7D;
    private static final double ALIGNMENT_EPSILON = 1.0E-5D;

    private ObjModelCutter() {
    }

    public static CutModel cut(ObjModel source, int width, int height, int depth) {
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Rockery dimensions must be positive");
        }

        // Blockbench's OBJ exporter places the horizontal origin at the
        // centre of the clicked block, while the OBJ itself uses block units.
        ObjModel base = source.translated(0.5D, 0.0D, 0.5D);
        Bounds boundsBeforeAlignment = base.bounds();
        ObjVector3 alignment = new ObjVector3(
                integerAlignment(boundsBeforeAlignment.minX(), boundsBeforeAlignment.maxX(), width, "X"),
                integerAlignment(boundsBeforeAlignment.minY(), boundsBeforeAlignment.maxY(), height, "Y"),
                integerAlignment(boundsBeforeAlignment.minZ(), boundsBeforeAlignment.maxZ(), depth, "Z")
        );
        ObjModel normalized = base.translated(alignment.x(), alignment.y(), alignment.z());
        Bounds normalizedBounds = normalized.bounds();
        validateBounds(normalizedBounds, width, height, depth);

        List<Part> parts = new ArrayList<>(width * height * depth);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    Bounds cell = new Bounds(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
                    List<ObjFace> faces = new ArrayList<>();
                    for (ObjFace face : normalized.faces()) {
                        if (isInternalBoundaryFace(face, x, y, z)) {
                            continue;
                        }
                        List<ObjVertex> clipped = clip(face.vertices(), cell);
                        if (clipped.size() < 3) {
                            continue;
                        }
                        int partX = x;
                        int partY = y;
                        int partZ = z;
                        List<ObjVertex> local = clipped.stream()
                                .map(vertex -> vertex.withPosition(vertex.position().add(-partX, -partY, -partZ)))
                                .toList();
                        addTriangles(faces, face, local);
                    }

                    List<Bounds> collisionBoxes = new ArrayList<>();
                    for (ObjObject object : normalized.objects()) {
                        Bounds objectBounds = objectBounds(object);
                        if (objectBounds == null) {
                            continue;
                        }
                        Bounds intersection = objectBounds.intersection(cell);
                        if (intersection != null) {
                            collisionBoxes.add(intersection.translated(-x, -y, -z));
                        }
                    }
                    collisionBoxes = simplifyCollisionBoxes(collisionBoxes);
                    parts.add(new Part(x, y, z, faces, collisionBoxes));
                }
            }
        }

        return new CutModel(alignment, boundsBeforeAlignment, normalizedBounds, normalized, parts);
    }

    private static int integerAlignment(double min, double max, int size, String axis) {
        int first = (int) Math.ceil(-min - ALIGNMENT_EPSILON);
        int last = (int) Math.floor(size - max + ALIGNMENT_EPSILON);
        if (first > last) {
            throw new IllegalArgumentException(
                    "OBJ bounds cannot fit rockery " + axis + " axis: " + min + ".." + max + " in 0.." + size
            );
        }
        return IntStream.rangeClosed(first, last)
                .boxed()
                .min(Comparator.comparingInt(value -> Math.abs(value)))
                .orElseThrow();
    }

    private static void validateBounds(Bounds bounds, int width, int height, int depth) {
        if (bounds.minX() < -ALIGNMENT_EPSILON || bounds.minY() < -ALIGNMENT_EPSILON
                || bounds.minZ() < -ALIGNMENT_EPSILON
                || bounds.maxX() > width + ALIGNMENT_EPSILON
                || bounds.maxY() > height + ALIGNMENT_EPSILON
                || bounds.maxZ() > depth + ALIGNMENT_EPSILON) {
            throw new IllegalArgumentException("Aligned OBJ bounds exceed rockery dimensions: " + bounds);
        }
    }

    private static Bounds objectBounds(ObjObject object) {
        if (object.positions().isEmpty()) {
            return null;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (ObjVector3 position : object.positions()) {
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            minZ = Math.min(minZ, position.z());
            maxX = Math.max(maxX, position.x());
            maxY = Math.max(maxY, position.y());
            maxZ = Math.max(maxZ, position.z());
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static List<Bounds> simplifyCollisionBoxes(List<Bounds> boxes) {
        List<Bounds> result = new ArrayList<>();
        for (Bounds candidate : boxes) {
            boolean duplicateOrContained = result.stream().anyMatch(existing -> contains(existing, candidate));
            if (duplicateOrContained) {
                continue;
            }
            result.removeIf(existing -> contains(candidate, existing));
            result.add(candidate);
        }
        return result;
    }

    private static boolean contains(Bounds outer, Bounds inner) {
        return outer.minX() <= inner.minX() + EPSILON
                && outer.minY() <= inner.minY() + EPSILON
                && outer.minZ() <= inner.minZ() + EPSILON
                && outer.maxX() >= inner.maxX() - EPSILON
                && outer.maxY() >= inner.maxY() - EPSILON
                && outer.maxZ() >= inner.maxZ() - EPSILON;
    }

    private static boolean isInternalBoundaryFace(
            ObjFace face,
            int x,
            int y,
            int z
    ) {
        return x > 0 && allAt(face, Axis.X, x)
                || y > 0 && allAt(face, Axis.Y, y)
                || z > 0 && allAt(face, Axis.Z, z);
    }

    private static boolean allAt(ObjFace face, Axis axis, double value) {
        return face.vertices().stream().allMatch(vertex -> Math.abs(axis.value(vertex.position()) - value) <= EPSILON);
    }

    private static List<ObjVertex> clip(List<ObjVertex> source, Bounds cell) {
        List<ObjVertex> result = new ArrayList<>(source);
        for (Plane plane : Plane.values()) {
            if (result.isEmpty()) {
                return List.of();
            }
            result = clipAgainst(result, plane, cell);
        }
        removeConsecutiveDuplicates(result);
        if (result.size() > 1 && samePosition(result.getFirst(), result.getLast())) {
            result.removeLast();
        }
        return result;
    }

    private static List<ObjVertex> clipAgainst(List<ObjVertex> source, Plane plane, Bounds cell) {
        List<ObjVertex> result = new ArrayList<>();
        ObjVertex previous = source.getLast();
        boolean previousInside = plane.inside(previous.position(), cell);
        for (ObjVertex current : source) {
            boolean currentInside = plane.inside(current.position(), cell);
            if (currentInside != previousInside) {
                result.add(intersection(previous, current, plane, cell));
            }
            if (currentInside) {
                result.add(current);
            }
            previous = current;
            previousInside = currentInside;
        }
        removeConsecutiveDuplicates(result);
        return result;
    }

    private static ObjVertex intersection(ObjVertex first, ObjVertex second, Plane plane, Bounds cell) {
        double boundary = plane.boundary(cell);
        double firstValue = plane.axis.value(first.position());
        double secondValue = plane.axis.value(second.position());
        double denominator = secondValue - firstValue;
        double delta = Math.abs(denominator) <= EPSILON
                ? 0.0D
                : (boundary - firstValue) / denominator;
        delta = Math.clamp(delta, 0.0D, 1.0D);
        ObjVector2 uv = first.uv() == null && second.uv() == null
                ? null
                : first.uv() == null
                        ? second.uv()
                        : second.uv() == null
                                ? first.uv()
                                : first.uv().lerp(second.uv(), delta);
        ObjVector3 normal = first.normal() == null && second.normal() == null
                ? null
                : first.normal() == null
                        ? second.normal()
                        : second.normal() == null
                                ? first.normal()
                                : first.normal().lerp(second.normal(), delta).normalized();
        return new ObjVertex(
                first.position().lerp(second.position(), delta),
                uv,
                normal
        );
    }

    private static void addTriangles(List<ObjFace> output, ObjFace source, List<ObjVertex> polygon) {
        for (int i = 1; i < polygon.size() - 1; i++) {
            ObjVertex first = polygon.getFirst();
            ObjVertex second = polygon.get(i);
            ObjVertex third = polygon.get(i + 1);
            ObjVector3 cross = second.position().subtract(first.position())
                    .cross(third.position().subtract(first.position()));
            if (cross.x() * cross.x() + cross.y() * cross.y() + cross.z() * cross.z() <= EPSILON * EPSILON) {
                continue;
            }
            output.add(new ObjFace(
                    source.objectIndex(),
                    source.objectName(),
                    source.material(),
                    List.of(first, second, third)
            ));
        }
    }

    private static void removeConsecutiveDuplicates(List<ObjVertex> vertices) {
        for (int i = vertices.size() - 1; i > 0; i--) {
            if (samePosition(vertices.get(i), vertices.get(i - 1))) {
                vertices.remove(i);
            }
        }
    }

    private static boolean samePosition(ObjVertex first, ObjVertex second) {
        return Math.abs(first.position().x() - second.position().x()) <= EPSILON
                && Math.abs(first.position().y() - second.position().y()) <= EPSILON
                && Math.abs(first.position().z() - second.position().z()) <= EPSILON;
    }

    public record CutModel(
            ObjVector3 alignment,
            Bounds boundsBeforeAlignment,
            Bounds normalizedBounds,
            ObjModel normalizedModel,
            List<Part> parts
    ) {
        public CutModel {
            parts = List.copyOf(parts);
        }
    }

    public record Part(
            int x,
            int y,
            int z,
            List<ObjFace> faces,
            List<Bounds> collisionBoxes
    ) {
        public Part {
            faces = List.copyOf(faces);
            collisionBoxes = List.copyOf(collisionBoxes);
        }
    }

    private enum Axis {
        X {
            @Override
            double value(ObjVector3 vector) {
                return vector.x();
            }
        },
        Y {
            @Override
            double value(ObjVector3 vector) {
                return vector.y();
            }
        },
        Z {
            @Override
            double value(ObjVector3 vector) {
                return vector.z();
            }
        };

        abstract double value(ObjVector3 vector);
    }

    private enum Plane {
        MIN_X(Axis.X, false),
        MAX_X(Axis.X, true),
        MIN_Y(Axis.Y, false),
        MAX_Y(Axis.Y, true),
        MIN_Z(Axis.Z, false),
        MAX_Z(Axis.Z, true);

        private final Axis axis;
        private final boolean maximum;

        Plane(Axis axis, boolean maximum) {
            this.axis = axis;
            this.maximum = maximum;
        }

        private boolean inside(ObjVector3 point, Bounds cell) {
            double value = axis.value(point);
            double boundary = boundary(cell);
            return maximum ? value <= boundary + EPSILON : value >= boundary - EPSILON;
        }

        private double boundary(Bounds cell) {
            return switch (axis) {
                case X -> maximum ? cell.maxX() : cell.minX();
                case Y -> maximum ? cell.maxY() : cell.minY();
                case Z -> maximum ? cell.maxZ() : cell.minZ();
            };
        }

    }
}
