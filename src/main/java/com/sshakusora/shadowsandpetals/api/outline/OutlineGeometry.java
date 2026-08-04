package com.sshakusora.shadowsandpetals.api.outline;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable line geometry in block-local coordinates. Coordinates use the
 * same 0-16 convention as block model JSON and {@code Block.box}.
 */
public final class OutlineGeometry {
    private final List<Line> lines;

    private OutlineGeometry(List<Line> lines) {
        this.lines = List.copyOf(lines);
    }

    public static OutlineGeometry of(List<Line> lines) {
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Outline geometry must contain at least one line");
        }
        return new OutlineGeometry(lines);
    }

    public static OutlineGeometry box(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        return polygonPrism(
                List.of(
                        new Vec3(minX, minY, minZ),
                        new Vec3(maxX, minY, minZ),
                        new Vec3(maxX, minY, maxZ),
                        new Vec3(minX, minY, maxZ)
                ),
                List.of(
                        new Vec3(minX, maxY, minZ),
                        new Vec3(maxX, maxY, minZ),
                        new Vec3(maxX, maxY, maxZ),
                        new Vec3(minX, maxY, maxZ)
                )
        );
    }

    /**
     * Creates a prism whose bottom and top are regular polygons with matching
     * vertex order. This supports sloped sides when corresponding top and
     * bottom vertices do not share the same x/z coordinates.
     */
    public static OutlineGeometry polygonPrism(List<Vec3> bottom, List<Vec3> top) {
        if (bottom.size() < 3 || bottom.size() != top.size()) {
            throw new IllegalArgumentException("Prism rings must have the same size and at least three vertices");
        }

        List<Line> lines = new ArrayList<>(bottom.size() * 3);
        addRing(lines, bottom);
        addRing(lines, top);
        for (int index = 0; index < bottom.size(); index++) {
            lines.add(new Line(bottom.get(index), top.get(index)));
        }
        return new OutlineGeometry(lines);
    }

    /**
     * Creates an eight-sided vertical prism by cutting the four corners of a
     * rectangular footprint.
     */
    public static OutlineGeometry octagonalPrism(
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            double cornerCut,
            double minY,
            double maxY
    ) {
        double width = maxX - minX;
        double depth = maxZ - minZ;
        if (width <= 0.0D || depth <= 0.0D) {
            throw new IllegalArgumentException("Octagonal prism dimensions must be positive");
        }
        if (cornerCut <= 0.0D || cornerCut * 2.0D >= Math.min(width, depth)) {
            throw new IllegalArgumentException("Corner cut must be between zero and half the footprint size");
        }

        List<Vec3> bottom = List.of(
                new Vec3(minX + cornerCut, minY, minZ),
                new Vec3(maxX - cornerCut, minY, minZ),
                new Vec3(maxX, minY, minZ + cornerCut),
                new Vec3(maxX, minY, maxZ - cornerCut),
                new Vec3(maxX - cornerCut, minY, maxZ),
                new Vec3(minX + cornerCut, minY, maxZ),
                new Vec3(minX, minY, maxZ - cornerCut),
                new Vec3(minX, minY, minZ + cornerCut)
        );
        List<Vec3> top = bottom.stream()
                .map(point -> new Vec3(point.x, maxY, point.z))
                .toList();
        return polygonPrism(bottom, top);
    }

    public List<Line> lines() {
        return lines;
    }

    private static void addRing(List<Line> lines, List<Vec3> ring) {
        for (int index = 0; index < ring.size(); index++) {
            lines.add(new Line(ring.get(index), ring.get((index + 1) % ring.size())));
        }
    }

    public record Line(Vec3 from, Vec3 to) {
        public Line {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
    }
}
