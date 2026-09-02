package com.sshakusora.shadowsandpetals.client.outline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Extracts the boundary of the union of cuboids declared by a Blockbench
 * model. The single-model entry point is also used by custom block outlines;
 * the multipart entry point additionally supports rockery structure offsets.
 *
 * <p>The model is not an axis-aligned voxel shape: its elements can be
 * rotated, overlap one another, and be split between several PART models.
 * Consequently, collecting cuboid edges and trying to hide some of them is
 * insufficient. This implementation starts with every declared cuboid face,
 * clips that face against the interior of every other cuboid, and finally
 * collects the boundaries of the remaining surface polygons. Intersections
 * between two rotated faces are therefore produced naturally by the clipping
 * operation, even when no source cuboid has an edge at that location.</p>
 */
public final class RockeryOutlineGeometry {
    private static final String OBJ_COLLISION_KEY = "shadowsandpetals:collision";
    private static final String OBJ_OUTLINE_KEY = "shadowsandpetals:outline";
    private static final double EDGE_EPSILON = 1.0E-6D;
    private static final double PLANE_EPSILON = 1.0E-5D;
    private static final double CLIP_EPSILON = 1.0E-7D;
    private static final double PARAMETER_EPSILON = 1.0E-9D;
    private static final double AREA_EPSILON = 1.0E-7D;
    private static final double NORMAL_EPSILON = 1.0E-7D;
    private static final double MIN_EDGE_LENGTH = PLANE_EPSILON * 2.0D;

    private RockeryOutlineGeometry() {
    }

    /**
     * Extracts the visible boundary of a single Blockbench/Minecraft model.
     *
     * <p>This entry point is intentionally public because the same rotated
     * cuboid clipping is useful for custom block outlines outside rockery
     * models. Multipart rockery callers should continue to use
     * {@link #fromModels(List)}.</p>
     */
    public static @Nullable OutlineGeometry fromModel(JsonObject model) {
        return fromModels(List.of(new ModelPart(model, Vec3.ZERO)));
    }

    /**
     * Parses all generated PART models in the common structure coordinate
     * system. Each offset is expressed in model units (16 units per block).
     */
    public static @Nullable OutlineGeometry fromModels(List<ModelPart> modelParts) {
        Objects.requireNonNull(modelParts, "modelParts");
        if (modelParts.isEmpty()) {
            return null;
        }

        boolean hasObjOutline = modelParts.stream()
                .map(ModelPart::model)
                .anyMatch(RockeryOutlineGeometry::hasExactObjOutline);
        List<Cuboid> cuboids = new ArrayList<>();
        for (ModelPart modelPart : modelParts) {
            Objects.requireNonNull(modelPart, "modelPart");
            JsonObject model = modelPart.model();
            if (hasObjOutline && hasExactObjOutline(model)) {
                JsonObject outline = model.getAsJsonObject(OBJ_OUTLINE_KEY);
                JsonArray outlineCuboids = outline.getAsJsonArray("cuboids");
                for (JsonElement cuboidValue : outlineCuboids) {
                    if (!cuboidValue.isJsonObject()) {
                        continue;
                    }
                    Cuboid cuboid = Cuboid.fromOutline(cuboidValue.getAsJsonObject(), modelPart.offset());
                    if (cuboid != null) {
                        cuboids.add(cuboid);
                    }
                }
            } else if (!hasObjOutline && model.has("elements") && model.get("elements").isJsonArray()) {
                for (JsonElement elementValue : model.getAsJsonArray("elements")) {
                    if (!elementValue.isJsonObject()) {
                        continue;
                    }
                    Cuboid cuboid = Cuboid.fromElement(elementValue.getAsJsonObject(), modelPart.offset());
                    if (cuboid != null) {
                        cuboids.add(cuboid);
                    }
                }
            } else if (!hasObjOutline && model.has(OBJ_COLLISION_KEY)
                    && model.get(OBJ_COLLISION_KEY).isJsonArray()) {
                for (JsonElement collisionValue : model.getAsJsonArray(OBJ_COLLISION_KEY)) {
                    if (!collisionValue.isJsonObject()) {
                        continue;
                    }
                    Cuboid cuboid = Cuboid.fromElement(
                            collisionElement(collisionValue.getAsJsonObject()),
                            modelPart.offset()
                    );
                    if (cuboid != null) {
                        cuboids.add(cuboid);
                    }
                }
            }
        }

        if (cuboids.isEmpty()) {
            return null;
        }

        List<Surface> surfaces = new ArrayList<>();
        for (Cuboid cuboid : cuboids) {
            for (CuboidFace face : cuboid.declaredFaces) {
                surfaces.add(new Surface(cuboid, face, PlaneFrame.of(cuboid, face)));
            }
        }
        // A larger coplanar face is accepted first. Later faces then only add
        // the portions which are not already represented by that surface,
        // turning overlapping PART/model faces into one surface union.
        surfaces.sort(Comparator.comparingDouble(Surface::area).reversed());

        BoundaryCollector boundaries = new BoundaryCollector();
        List<VisibleFragment> accepted = new ArrayList<>();
        for (Surface surface : surfaces) {
            Cuboid cuboid = surface.cuboid();
            PlaneFrame frame = surface.frame();
            List<Polygon2> fragments = new ArrayList<>();
            fragments.add(frame.project(cuboid.faceVertices(surface.face())));

            for (Cuboid occluder : cuboids) {
                if (occluder == cuboid || fragments.isEmpty()) {
                    continue;
                }

                Polygon2 section = frame.intersection(occluder);
                if (section == null || !frame.occludes(occluder)) {
                    continue;
                }

                List<Polygon2> remaining = new ArrayList<>();
                for (Polygon2 fragment : fragments) {
                    remaining.addAll(subtractConvex(fragment, section));
                }
                fragments = remaining;
            }

            // Remove portions which are already represented by an accepted
            // coplanar face with the same outward normal. This is a surface
            // union operation, distinct from volume occlusion above.
            for (VisibleFragment previous : accepted) {
                if (fragments.isEmpty() || !sameSurface(frame, previous)) {
                    continue;
                }
                Polygon2 cover = frame.project(previous.vertices().toArray(Vec3[]::new));
                List<Polygon2> remaining = new ArrayList<>();
                for (Polygon2 fragment : fragments) {
                    remaining.addAll(subtractConvex(fragment, cover));
                }
                fragments = remaining;
            }

            for (Polygon2 fragment : fragments) {
                List<Vec3> vertices = frame.unproject(fragment);
                boundaries.add(vertices, frame.normal());
                accepted.add(new VisibleFragment(vertices, frame.normal()));
            }
        }

        List<OutlineGeometry.Line> lines = boundaries.lines();
        return lines.isEmpty() ? null : OutlineGeometry.of(lines);
    }

    private static boolean hasExactObjOutline(JsonObject model) {
        if (!model.has(OBJ_OUTLINE_KEY) || !model.get(OBJ_OUTLINE_KEY).isJsonObject()) {
            return false;
        }
        JsonObject outline = model.getAsJsonObject(OBJ_OUTLINE_KEY);
        return outline.has("cuboids") && outline.get("cuboids").isJsonArray();
    }

    private static JsonObject collisionElement(JsonObject collision) {
        if (!collision.has("from") || !collision.has("to")) {
            return new JsonObject();
        }
        JsonObject element = new JsonObject();
        element.add("from", collision.get("from").deepCopy());
        element.add("to", collision.get("to").deepCopy());
        JsonObject faces = new JsonObject();
        for (String direction : List.of("north", "east", "south", "west", "up", "down")) {
            faces.add(direction, new JsonObject());
        }
        element.add("faces", faces);
        return element;
    }

    private static boolean sameSurface(PlaneFrame frame, VisibleFragment other) {
        if (frame.normal().dot(other.normal()) < 1.0D - NORMAL_EPSILON) {
            return false;
        }
        return Math.abs(frame.normal().dot(other.vertices().getFirst().subtract(frame.origin())))
                <= PLANE_EPSILON;
    }

    public static OutlineGeometry translate(OutlineGeometry geometry, double x, double y, double z) {
        return OutlineGeometry.of(geometry.lines().stream()
                .map(line -> new OutlineGeometry.Line(
                        line.from().add(x, y, z),
                        line.to().add(x, y, z)
                ))
                .toList());
    }

    public static OutlineGeometry rotateClockwise(OutlineGeometry geometry) {
        return OutlineGeometry.of(geometry.lines().stream()
                .map(line -> new OutlineGeometry.Line(
                        rotateClockwise(line.from()),
                        rotateClockwise(line.to())
                ))
                .toList());
    }

    private static Vec3 rotateClockwise(Vec3 point) {
        return new Vec3(16.0D - point.z, point.y, point.x);
    }

    /**
     * Subtracts a convex polygon from another convex polygon. At each clip
     * edge, the part outside that edge is emitted and only the part inside
     * all processed edges is carried forward. The emitted pieces are
     * disjoint except for their boundaries, so this handles a clipped face
     * without requiring a general-purpose non-convex polygon library.
     */
    private static List<Polygon2> subtractConvex(Polygon2 subject, Polygon2 clip) {
        List<Polygon2> inside = new ArrayList<>(List.of(subject));
        List<Polygon2> outside = new ArrayList<>();

        List<Point2> clipPoints = clip.points();
        for (int index = 0; index < clipPoints.size(); index++) {
            Point2 first = clipPoints.get(index);
            Point2 second = clipPoints.get((index + 1) % clipPoints.size());
            List<Polygon2> nextInside = new ArrayList<>();

            for (Polygon2 polygon : inside) {
                Polygon2 outsidePart = clipHalfPlane(polygon, first, second, false);
                if (outsidePart != null) {
                    outside.add(outsidePart);
                }

                Polygon2 insidePart = clipHalfPlane(polygon, first, second, true);
                if (insidePart != null) {
                    nextInside.add(insidePart);
                }
            }
            inside = nextInside;
            if (inside.isEmpty()) {
                break;
            }
        }
        return outside;
    }

    private static @Nullable Polygon2 clipHalfPlane(
            Polygon2 polygon,
            Point2 edgeStart,
            Point2 edgeEnd,
            boolean keepLeft
    ) {
        List<Point2> source = polygon.points();
        if (source.size() < 3) {
            return null;
        }

        List<Point2> result = new ArrayList<>(source.size() + 1);
        Point2 previous = source.getLast();
        double previousSide = side(edgeStart, edgeEnd, previous);
        boolean previousInside = inside(previousSide, keepLeft);

        for (Point2 current : source) {
            double currentSide = side(edgeStart, edgeEnd, current);
            boolean currentInside = inside(currentSide, keepLeft);

            if (currentInside != previousInside) {
                double denominator = previousSide - currentSide;
                if (Math.abs(denominator) > CLIP_EPSILON) {
                    double amount = previousSide / denominator;
                    result.add(interpolate(previous, current, amount));
                }
            }
            if (currentInside) {
                result.add(current);
            }

            previous = current;
            previousSide = currentSide;
            previousInside = currentInside;
        }

        return Polygon2.of(result);
    }

    private static boolean inside(double side, boolean keepLeft) {
        return keepLeft ? side >= -CLIP_EPSILON : side <= CLIP_EPSILON;
    }

    private static double side(Point2 edgeStart, Point2 edgeEnd, Point2 point) {
        return (edgeEnd.x() - edgeStart.x()) * (point.y() - edgeStart.y())
                - (edgeEnd.y() - edgeStart.y()) * (point.x() - edgeStart.x());
    }

    private static Point2 interpolate(Point2 first, Point2 second, double amount) {
        return new Point2(
                first.x() + (second.x() - first.x()) * amount,
                first.y() + (second.y() - first.y()) * amount
        );
    }

    private record Surface(Cuboid cuboid, CuboidFace face, PlaneFrame frame) {
        private double area() {
            return Math.abs(Polygon2.signedArea(frame.project(cuboid.faceVertices(face)).points()));
        }
    }

    private record VisibleFragment(List<Vec3> vertices, Vec3 normal) {
        private VisibleFragment {
            vertices = List.copyOf(vertices);
        }
    }

    /** A model together with its offset in the unrotated, complete structure. */
    public record ModelPart(JsonObject model, Vec3 offset) {
        public ModelPart {
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(offset, "offset");
        }
    }

    private interface CuboidFace {
        int[] corners();

        @Nullable
        Vec3 expectedNormal(Cuboid cuboid);
    }

    private enum Face implements CuboidFace {
        DOWN("down", new int[]{0, 1, 2, 3}, new Vec3(0.0D, -1.0D, 0.0D)),
        UP("up", new int[]{4, 5, 6, 7}, new Vec3(0.0D, 1.0D, 0.0D)),
        NORTH("north", new int[]{0, 4, 5, 1}, new Vec3(0.0D, 0.0D, -1.0D)),
        SOUTH("south", new int[]{3, 2, 6, 7}, new Vec3(0.0D, 0.0D, 1.0D)),
        WEST("west", new int[]{0, 3, 7, 4}, new Vec3(-1.0D, 0.0D, 0.0D)),
        EAST("east", new int[]{1, 5, 6, 2}, new Vec3(1.0D, 0.0D, 0.0D));

        private final String serializedName;
        private final int[] corners;
        private final Vec3 localNormal;

        Face(String serializedName, int[] corners, Vec3 localNormal) {
            this.serializedName = serializedName;
            this.corners = corners;
            this.localNormal = localNormal;
        }

        @Override
        public int[] corners() {
            return corners;
        }

        @Override
        public Vec3 expectedNormal(Cuboid cuboid) {
            return cuboid.transform.applyDirection(localNormal);
        }
    }

    private record OutlineFace(int[] corners) implements CuboidFace {
        @Override
        public @Nullable Vec3 expectedNormal(Cuboid cuboid) {
            return null;
        }
    }

    private static final class Cuboid {
        private static final int[][] EDGES = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        private final Vec3[] vertices;
        private final ElementTransform transform;
        private final List<CuboidFace> declaredFaces;
        private final List<int[]> edges;

        private Cuboid(
                ElementTransform transform,
                Vec3[] vertices,
                List<CuboidFace> declaredFaces,
                List<int[]> edges
        ) {
            this.transform = transform;
            this.vertices = vertices;
            this.declaredFaces = List.copyOf(declaredFaces);
            this.edges = List.copyOf(edges);
        }

        private static @Nullable Cuboid fromElement(JsonObject element, Vec3 offset) {
            if (!element.has("from") || !element.has("to")
                    || !element.get("from").isJsonArray() || !element.get("to").isJsonArray()) {
                return null;
            }

            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            if (from.size() < 3 || to.size() < 3) {
                return null;
            }

            double[] min = new double[3];
            double[] max = new double[3];
            for (int coordinate = 0; coordinate < 3; coordinate++) {
                double first = from.get(coordinate).getAsDouble();
                double second = to.get(coordinate).getAsDouble();
                min[coordinate] = Math.min(first, second);
                max[coordinate] = Math.max(first, second);
                if (max[coordinate] - min[coordinate] <= EDGE_EPSILON) {
                    return null;
                }
            }

            ElementTransform transform = ElementTransform.fromElement(element);
            Vec3[] vertices = new Vec3[]{
                    toWorld(transform, min[0], min[1], min[2], offset),
                    toWorld(transform, max[0], min[1], min[2], offset),
                    toWorld(transform, max[0], min[1], max[2], offset),
                    toWorld(transform, min[0], min[1], max[2], offset),
                    toWorld(transform, min[0], max[1], min[2], offset),
                    toWorld(transform, max[0], max[1], min[2], offset),
                    toWorld(transform, max[0], max[1], max[2], offset),
                    toWorld(transform, min[0], max[1], max[2], offset)
            };

            List<CuboidFace> declaredFaces = new ArrayList<>();
            if (element.has("faces") && element.get("faces").isJsonObject()) {
                JsonObject faces = element.getAsJsonObject("faces");
                for (Face face : Face.values()) {
                    if (faces.has(face.serializedName)) {
                        declaredFaces.add(face);
                    }
                }
            }
            if (declaredFaces.isEmpty()) {
                return null;
            }
            return new Cuboid(transform, vertices, declaredFaces, edgeList(EDGES));
        }

        private static @Nullable Cuboid fromOutline(JsonObject outline, Vec3 offset) {
            if (!outline.has("vertices") || !outline.get("vertices").isJsonArray()
                    || !outline.has("faces") || !outline.get("faces").isJsonArray()
                    || !outline.has("edges") || !outline.get("edges").isJsonArray()) {
                return null;
            }

            JsonArray sourceVertices = outline.getAsJsonArray("vertices");
            if (sourceVertices.size() != 8) {
                return null;
            }
            Vec3[] vertices = new Vec3[sourceVertices.size()];
            for (int index = 0; index < sourceVertices.size(); index++) {
                JsonElement value = sourceVertices.get(index);
                if (!value.isJsonArray() || value.getAsJsonArray().size() < 3) {
                    return null;
                }
                JsonArray point = value.getAsJsonArray();
                vertices[index] = new Vec3(
                        point.get(0).getAsDouble(),
                        point.get(1).getAsDouble(),
                        point.get(2).getAsDouble()
                ).add(offset);
            }

            JsonArray sourceFaces = outline.getAsJsonArray("faces");
            if (sourceFaces.size() == 0) {
                return null;
            }
            List<CuboidFace> faces = new ArrayList<>(sourceFaces.size());
            for (JsonElement value : sourceFaces) {
                if (!value.isJsonArray() || value.getAsJsonArray().size() != 4) {
                    return null;
                }
                JsonArray indices = value.getAsJsonArray();
                int[] corners = new int[4];
                for (int index = 0; index < corners.length; index++) {
                    corners[index] = indices.get(index).getAsInt();
                    if (corners[index] < 0 || corners[index] >= vertices.length) {
                        return null;
                    }
                    for (int previous = 0; previous < index; previous++) {
                        if (corners[previous] == corners[index]) {
                            return null;
                        }
                    }
                }
                faces.add(new OutlineFace(corners));
            }

            JsonArray sourceEdges = outline.getAsJsonArray("edges");
            if (sourceEdges.size() != EDGES.length) {
                return null;
            }
            List<int[]> edges = new ArrayList<>(sourceEdges.size());
            for (JsonElement value : sourceEdges) {
                if (!value.isJsonArray() || value.getAsJsonArray().size() != 2) {
                    return null;
                }
                JsonArray indices = value.getAsJsonArray();
                int first = indices.get(0).getAsInt();
                int second = indices.get(1).getAsInt();
                if (first < 0 || first >= vertices.length || second < 0
                        || second >= vertices.length || first == second) {
                    return null;
                }
                edges.add(new int[]{first, second});
            }
            return new Cuboid(ElementTransform.identity(), vertices, faces, edges);
        }

        private static List<int[]> edgeList(int[][] source) {
            List<int[]> edges = new ArrayList<>(source.length);
            for (int[] edge : source) {
                edges.add(edge.clone());
            }
            return edges;
        }

        private static Vec3 average(Vec3[] points) {
            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            for (Vec3 point : points) {
                x += point.x;
                y += point.y;
                z += point.z;
            }
            return new Vec3(x / points.length, y / points.length, z / points.length);
        }

        private static Vec3 toWorld(ElementTransform transform, double x, double y, double z, Vec3 offset) {
            return transform.apply(new Vec3(x, y, z)).add(offset);
        }

        private Vec3[] faceVertices(CuboidFace face) {
            int[] corners = face.corners();
            return new Vec3[]{
                    vertices[corners[0]],
                    vertices[corners[1]],
                    vertices[corners[2]],
                    vertices[corners[3]]
            };
        }

        private Vec3 faceNormal(CuboidFace face) {
            Vec3[] faceVertices = faceVertices(face);
            Vec3 cross = faceVertices[1].subtract(faceVertices[0])
                    .cross(faceVertices[2].subtract(faceVertices[0]));
            Vec3 expected = face.expectedNormal(this);
            if (expected == null) {
                expected = average(faceVertices).subtract(average(vertices));
            }
            if (cross.dot(expected) < 0.0D) {
                cross = cross.scale(-1.0D);
            }
            return cross.normalize();
        }

        private List<Vec3> allVertices() {
            return List.of(vertices);
        }

        private Vec3 vertex(int index) {
            return vertices[index];
        }
    }

    private record PlaneFrame(Vec3 origin, Vec3 normal, Vec3 u, Vec3 v) {
        private static PlaneFrame of(Cuboid cuboid, CuboidFace face) {
            Vec3[] vertices = cuboid.faceVertices(face);
            Vec3 normal = cuboid.faceNormal(face);
            Vec3 u = vertices[1].subtract(vertices[0]).normalize();
            Vec3 v = normal.cross(u).normalize();
            return new PlaneFrame(vertices[0], normal, u, v);
        }

        private Polygon2 project(Vec3[] vertices) {
            List<Point2> points = new ArrayList<>(vertices.length);
            for (Vec3 vertex : vertices) {
                points.add(projectPoint(vertex));
            }
            return Polygon2.of(points);
        }

        private Point2 projectPoint(Vec3 point) {
            Vec3 delta = point.subtract(origin);
            return new Point2(delta.dot(u), delta.dot(v));
        }

        private Vec3 unproject(Point2 point) {
            return origin.add(u.scale(point.x())).add(v.scale(point.y()));
        }

        private List<Vec3> unproject(Polygon2 polygon) {
            return polygon.points().stream().map(this::unproject).toList();
        }

        /** Returns the convex cross-section of a cuboid by this face plane. */
        private @Nullable Polygon2 intersection(Cuboid cuboid) {
            List<Point2> points = new ArrayList<>();
            for (Vec3 vertex : cuboid.allVertices()) {
                double distance = signedDistance(vertex);
                if (Math.abs(distance) <= PLANE_EPSILON) {
                    points.add(projectPoint(vertex));
                }
            }

            for (int[] edge : cuboid.edges) {
                Vec3 first = cuboid.vertex(edge[0]);
                Vec3 second = cuboid.vertex(edge[1]);
                double firstDistance = signedDistance(first);
                double secondDistance = signedDistance(second);
                if ((firstDistance > PLANE_EPSILON && secondDistance < -PLANE_EPSILON)
                        || (firstDistance < -PLANE_EPSILON && secondDistance > PLANE_EPSILON)) {
                    double amount = firstDistance / (firstDistance - secondDistance);
                    points.add(projectPoint(first.add(second.subtract(first).scale(amount))));
                }
            }
            return Polygon2.hull(points);
        }

        /**
         * A cuboid can hide this face only when it occupies the side of the
         * face toward the current outward normal. A cuboid entirely behind
         * the face shares the plane but does not obscure it; its coincident
         * surface is handled separately by the coplanar surface union.
         */
        private boolean occludes(Cuboid cuboid) {
            double maximum = Double.NEGATIVE_INFINITY;
            for (Vec3 vertex : cuboid.allVertices()) {
                maximum = Math.max(maximum, signedDistance(vertex));
            }
            return maximum > PLANE_EPSILON;
        }

        private double signedDistance(Vec3 point) {
            return normal.dot(point.subtract(origin));
        }
    }

    private record Point2(double x, double y) {
        private boolean near(Point2 other) {
            return Math.abs(x - other.x) <= CLIP_EPSILON
                    && Math.abs(y - other.y) <= CLIP_EPSILON;
        }
    }

    private record Polygon2(List<Point2> points) {
        private Polygon2 {
            points = List.copyOf(points);
        }

        private static @Nullable Polygon2 of(List<Point2> points) {
            List<Point2> cleaned = clean(points);
            if (cleaned.size() < 3 || Math.abs(signedArea(cleaned)) <= AREA_EPSILON) {
                return null;
            }
            if (signedArea(cleaned) < 0.0D) {
                List<Point2> reversed = new ArrayList<>(cleaned);
                Collections.reverse(reversed);
                cleaned = reversed;
            }
            return new Polygon2(cleaned);
        }

        private static @Nullable Polygon2 hull(List<Point2> points) {
            List<Point2> unique = new ArrayList<>();
            for (Point2 point : points) {
                if (unique.stream().noneMatch(point::near)) {
                    unique.add(point);
                }
            }
            if (unique.size() < 3) {
                return null;
            }
            unique.sort(Comparator.comparingDouble(Point2::x).thenComparingDouble(Point2::y));

            List<Point2> lower = new ArrayList<>();
            for (Point2 point : unique) {
                while (lower.size() >= 2
                        && cross(lower.get(lower.size() - 2), lower.getLast(), point) <= CLIP_EPSILON) {
                    lower.removeLast();
                }
                lower.add(point);
            }

            List<Point2> upper = new ArrayList<>();
            for (int index = unique.size() - 1; index >= 0; index--) {
                Point2 point = unique.get(index);
                while (upper.size() >= 2
                        && cross(upper.get(upper.size() - 2), upper.getLast(), point) <= CLIP_EPSILON) {
                    upper.removeLast();
                }
                upper.add(point);
            }

            lower.removeLast();
            upper.removeLast();
            lower.addAll(upper);
            return of(lower);
        }

        private static List<Point2> clean(List<Point2> source) {
            List<Point2> result = new ArrayList<>();
            for (Point2 point : source) {
                if (result.isEmpty() || !point.near(result.getLast())) {
                    result.add(point);
                }
            }
            if (result.size() > 1 && result.getFirst().near(result.getLast())) {
                result.removeLast();
            }

            boolean changed;
            do {
                changed = false;
                if (result.size() < 3) {
                    break;
                }
                for (int index = 0; index < result.size(); index++) {
                    Point2 previous = result.get((index + result.size() - 1) % result.size());
                    Point2 current = result.get(index);
                    Point2 next = result.get((index + 1) % result.size());
                    if (Math.abs(cross(previous, current, next)) <= CLIP_EPSILON
                            && dot(previous, current, next) >= -CLIP_EPSILON) {
                        result.remove(index);
                        changed = true;
                        break;
                    }
                }
            } while (changed);
            return result;
        }

        private static double signedArea(List<Point2> points) {
            double area = 0.0D;
            for (int index = 0; index < points.size(); index++) {
                Point2 first = points.get(index);
                Point2 second = points.get((index + 1) % points.size());
                area += first.x() * second.y() - second.x() * first.y();
            }
            return area * 0.5D;
        }

        private static double cross(Point2 first, Point2 second, Point2 third) {
            return (second.x() - first.x()) * (third.y() - first.y())
                    - (second.y() - first.y()) * (third.x() - first.x());
        }

        private static double dot(Point2 first, Point2 second, Point2 third) {
            return (first.x() - second.x()) * (third.x() - second.x())
                    + (first.y() - second.y()) * (third.y() - second.y());
        }
    }

    private record BoundaryEdge(Vec3 from, Vec3 to, Vec3 normal) {
    }

    /**
     * Groups all polygon-boundary occurrences on the same supporting line.
     * Each elementary interval is emitted once, unless two same-plane faces
     * with the same normal meet there; that is a coplanar face seam and must
     * be removed. Different normals are intentionally retained as one edge.
     */
    private static final class BoundaryCollector {
        private final List<BoundaryEdge> edges = new ArrayList<>();

        private void add(List<Vec3> polygon, Vec3 normal) {
            for (int index = 0; index < polygon.size(); index++) {
                Vec3 from = polygon.get(index);
                Vec3 to = polygon.get((index + 1) % polygon.size());
                if (from.distanceToSqr(to) > MIN_EDGE_LENGTH * MIN_EDGE_LENGTH) {
                    edges.add(new BoundaryEdge(from, to, normal));
                }
            }
        }

        private List<OutlineGeometry.Line> lines() {
            List<LineGroup> groups = new ArrayList<>();
            for (BoundaryEdge edge : edges) {
                LineGroup group = null;
                for (LineGroup candidate : groups) {
                    if (candidate.collinear(edge)) {
                        group = candidate;
                        break;
                    }
                }
                if (group == null) {
                    group = new LineGroup(edge);
                    groups.add(group);
                }
                group.edges.add(edge);
            }

            List<OutlineGeometry.Line> result = new ArrayList<>();
            for (LineGroup group : groups) {
                result.addAll(group.lines());
            }
            return result;
        }
    }

    private static final class LineGroup {
        private final BoundaryEdge reference;
        private final Vec3 direction;
        private final List<BoundaryEdge> edges = new ArrayList<>();

        private LineGroup(BoundaryEdge reference) {
            this.reference = reference;
            this.direction = reference.to().subtract(reference.from()).normalize();
        }

        private boolean collinear(BoundaryEdge edge) {
            Vec3 edgeDirection = edge.to().subtract(edge.from()).normalize();
            if (Math.abs(direction.dot(edgeDirection)) < 1.0D - 1.0E-6D) {
                return false;
            }
            return distanceFromLine(edge.from()) <= PLANE_EPSILON
                    && distanceFromLine(edge.to()) <= PLANE_EPSILON;
        }

        private double distanceFromLine(Vec3 point) {
            return point.subtract(reference.from()).cross(direction).length();
        }

        private List<OutlineGeometry.Line> lines() {
            List<Double> splitPoints = new ArrayList<>();
            for (BoundaryEdge edge : edges) {
                splitPoints.add(parameter(edge.from()));
                splitPoints.add(parameter(edge.to()));
            }
            splitPoints.sort(Comparator.naturalOrder());

            List<Double> unique = new ArrayList<>();
            for (double point : splitPoints) {
                if (unique.isEmpty() || point - unique.getLast() > PARAMETER_EPSILON) {
                    unique.add(point);
                }
            }

            List<OutlineGeometry.Line> result = new ArrayList<>();
            double pendingStart = Double.NaN;
            double pendingEnd = Double.NaN;
            for (int index = 0; index + 1 < unique.size(); index++) {
                double start = unique.get(index);
                double end = unique.get(index + 1);
                if (end - start <= PARAMETER_EPSILON) {
                    continue;
                }
                double midpoint = (start + end) * 0.5D;
                List<BoundaryEdge> covering = edges.stream()
                        .filter(edge -> {
                            double first = Math.min(parameter(edge.from()), parameter(edge.to()));
                            double second = Math.max(parameter(edge.from()), parameter(edge.to()));
                            return midpoint >= first - PARAMETER_EPSILON && midpoint <= second + PARAMETER_EPSILON;
                        })
                        .toList();
                boolean exposed = !covering.isEmpty() && !hasCoplanarSeam(covering);
                if (!exposed) {
                    if (!Double.isNaN(pendingStart)) {
                        addLine(result, pendingStart, pendingEnd);
                        pendingStart = Double.NaN;
                        pendingEnd = Double.NaN;
                    }
                    continue;
                }

                if (Double.isNaN(pendingStart)) {
                    pendingStart = start;
                    pendingEnd = end;
                } else if (start - pendingEnd <= PARAMETER_EPSILON) {
                    pendingEnd = end;
                } else {
                    addLine(result, pendingStart, pendingEnd);
                    pendingStart = start;
                    pendingEnd = end;
                }
            }
            if (!Double.isNaN(pendingStart)) {
                addLine(result, pendingStart, pendingEnd);
            }
            return result;
        }

        private void addLine(List<OutlineGeometry.Line> result, double start, double end) {
            Vec3 from = pointAt(start);
            Vec3 to = pointAt(end);
            if (from.distanceToSqr(to) > MIN_EDGE_LENGTH * MIN_EDGE_LENGTH) {
                result.add(new OutlineGeometry.Line(from, to));
            }
        }

        private boolean hasCoplanarSeam(List<BoundaryEdge> covering) {
            for (int first = 0; first < covering.size(); first++) {
                for (int second = first + 1; second < covering.size(); second++) {
                    BoundaryEdge a = covering.get(first);
                    BoundaryEdge b = covering.get(second);
                    if (a.normal().dot(b.normal()) < 1.0D - NORMAL_EPSILON) {
                        continue;
                    }
                    Vec3 offset = b.from().subtract(a.from());
                    if (Math.abs(a.normal().dot(offset)) <= PLANE_EPSILON) {
                        return true;
                    }
                }
            }
            return false;
        }

        private double parameter(Vec3 point) {
            return point.subtract(reference.from()).dot(direction);
        }

        private Vec3 pointAt(double parameter) {
            return reference.from().add(direction.scale(parameter));
        }
    }

    private static final class ElementTransform {
        private enum Kind {
            IDENTITY,
            SINGLE_AXIS,
            EULER
        }

        private final Kind kind;
        private final Vec3 origin;
        private final String axis;
        private final boolean rescale;
        private final double sinX;
        private final double cosX;
        private final double sinY;
        private final double cosY;
        private final double sinZ;
        private final double cosZ;

        private ElementTransform(
                Kind kind,
                Vec3 origin,
                String axis,
                boolean rescale,
                double sinX,
                double cosX,
                double sinY,
                double cosY,
                double sinZ,
                double cosZ
        ) {
            this.kind = kind;
            this.origin = origin;
            this.axis = axis;
            this.rescale = rescale;
            this.sinX = sinX;
            this.cosX = cosX;
            this.sinY = sinY;
            this.cosY = cosY;
            this.sinZ = sinZ;
            this.cosZ = cosZ;
        }

        private static ElementTransform fromElement(JsonObject element) {
            if (!element.has("rotation") || !element.get("rotation").isJsonObject()) {
                return identity();
            }

            JsonObject rotation = element.getAsJsonObject("rotation");
            JsonArray origin = rotation.has("origin") && rotation.get("origin").isJsonArray()
                    ? rotation.getAsJsonArray("origin")
                    : null;
            if (origin == null || origin.size() < 3) {
                return identity();
            }
            Vec3 rotationOrigin = new Vec3(
                    origin.get(0).getAsDouble(),
                    origin.get(1).getAsDouble(),
                    origin.get(2).getAsDouble()
            );

            if (rotation.has("axis") && rotation.has("angle")) {
                String axis = rotation.get("axis").getAsString();
                double angle = Math.toRadians(rotation.get("angle").getAsDouble());
                double sin = Math.sin(angle);
                double cos = Math.cos(angle);
                double sinX = 0.0D;
                double cosX = 1.0D;
                double sinY = 0.0D;
                double cosY = 1.0D;
                double sinZ = 0.0D;
                double cosZ = 1.0D;
                switch (axis) {
                    case "x" -> {
                        sinX = sin;
                        cosX = cos;
                    }
                    case "y" -> {
                        sinY = sin;
                        cosY = cos;
                    }
                    case "z" -> {
                        sinZ = sin;
                        cosZ = cos;
                    }
                    default -> {
                    }
                }
                return new ElementTransform(
                        Kind.SINGLE_AXIS,
                        rotationOrigin,
                        axis,
                        rotation.has("rescale") && rotation.get("rescale").getAsBoolean(),
                        sinX,
                        cosX,
                        sinY,
                        cosY,
                        sinZ,
                        cosZ
                );
            }

            if (rotation.has("x") || rotation.has("y") || rotation.has("z")) {
                double x = Math.toRadians(rotation.has("x") ? rotation.get("x").getAsDouble() : 0.0D);
                double y = Math.toRadians(rotation.has("y") ? rotation.get("y").getAsDouble() : 0.0D);
                double z = Math.toRadians(rotation.has("z") ? rotation.get("z").getAsDouble() : 0.0D);
                return new ElementTransform(
                        Kind.EULER,
                        rotationOrigin,
                        "",
                        false,
                        Math.sin(x),
                        Math.cos(x),
                        Math.sin(y),
                        Math.cos(y),
                        Math.sin(z),
                        Math.cos(z)
                );
            }
            return identity();
        }

        private static ElementTransform identity() {
            return new ElementTransform(
                    Kind.IDENTITY,
                    Vec3.ZERO,
                    "",
                    false,
                    0.0D,
                    1.0D,
                    0.0D,
                    1.0D,
                    0.0D,
                    1.0D
            );
        }

        private Vec3 apply(Vec3 point) {
            double x = point.x - origin.x;
            double y = point.y - origin.y;
            double z = point.z - origin.z;

            if (kind == Kind.IDENTITY) {
                return point;
            }
            if (kind == Kind.SINGLE_AXIS) {
                double[] scaled = applyRescale(x, y, z);
                double[] rotated = rotateSingleAxis(scaled[0], scaled[1], scaled[2]);
                return new Vec3(origin.x + rotated[0], origin.y + rotated[1], origin.z + rotated[2]);
            }

            // Match Minecraft's Matrix4f.rotationZYX(z, y, x): X, then Y, then Z.
            double rotatedY = y * cosX - z * sinX;
            double rotatedZ = y * sinX + z * cosX;
            y = rotatedY;
            z = rotatedZ;

            double rotatedX = x * cosY + z * sinY;
            rotatedZ = -x * sinY + z * cosY;
            x = rotatedX;
            z = rotatedZ;

            double rotatedX2 = x * cosZ - y * sinZ;
            double rotatedY2 = x * sinZ + y * cosZ;
            return new Vec3(origin.x + rotatedX2, origin.y + rotatedY2, origin.z + z);
        }

        private Vec3 applyDirection(Vec3 direction) {
            return apply(origin.add(direction)).subtract(apply(origin));
        }

        private double[] applyRescale(double x, double y, double z) {
            if (!rescale) {
                return new double[]{x, y, z};
            }
            double scale = rescaleFactor();
            return switch (axis) {
                case "x" -> new double[]{x, y * scale, z * scale};
                case "y" -> new double[]{x * scale, y, z * scale};
                case "z" -> new double[]{x * scale, y * scale, z};
                default -> new double[]{x, y, z};
            };
        }

        private double rescaleFactor() {
            double cos = switch (axis) {
                case "x" -> cosX;
                case "y" -> cosY;
                case "z" -> cosZ;
                default -> 1.0D;
            };
            return Math.abs(cos) <= EDGE_EPSILON ? 1.0D : 1.0D / cos;
        }

        private double[] rotateSingleAxis(double x, double y, double z) {
            return switch (axis) {
                case "x" -> new double[]{x, y * cosX - z * sinX, y * sinX + z * cosX};
                case "y" -> new double[]{x * cosY + z * sinY, y, -x * sinY + z * cosY};
                case "z" -> new double[]{x * cosZ - y * sinZ, x * sinZ + y * cosZ, z};
                default -> new double[]{x, y, z};
            };
        }
    }
}
