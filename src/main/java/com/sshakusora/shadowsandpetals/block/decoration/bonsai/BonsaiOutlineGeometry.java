package com.sshakusora.shadowsandpetals.block.decoration.bonsai;

import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Cached wireframes for the seven cuboids that make up the bonsai pot model.
 * Coordinates use the model JSON's 0-16 convention.
 */
public final class BonsaiOutlineGeometry {
    private static final List<OutlineGeometry> ROTATED_OUTLINES = createRotatedOutlines();

    private BonsaiOutlineGeometry() {
    }

    public static OutlineGeometry forRotation(int rotationSegment) {
        return ROTATED_OUTLINES.get(Math.floorMod(rotationSegment, 16));
    }

    private static List<OutlineGeometry> createRotatedOutlines() {
        OutlineGeometry base = createBaseOutline();
        List<OutlineGeometry> outlines = new ArrayList<>(16);
        for (int rotation = 0; rotation < 16; rotation++) {
            int segment = rotation;
            outlines.add(OutlineGeometry.of(base.lines().stream()
                    .map(line -> new OutlineGeometry.Line(
                            BonsaiModelTransform.transformModelPoint(line.from(), segment),
                            BonsaiModelTransform.transformModelPoint(line.to(), segment)
                    ))
                    .toList()));
        }
        return List.copyOf(outlines);
    }

    /**
     * The seven cuboids in assets/.../models/block/bonsai/bonsai.json.
     * Keeping this in model units makes the outline independent from
     * VoxelShape's axis-aligned approximation.
     */
    private static OutlineGeometry createBaseOutline() {
        double[][] cuboids = {
                {4.0D, 1.0D, 3.0D, 12.0D, 6.0D, 13.0D},
                {4.0D, 0.0D, 11.0D, 12.0D, 1.0D, 13.0D},
                {4.0D, 0.0D, 3.0D, 12.0D, 1.0D, 5.0D},
                {3.0D, 1.0D, 3.0D, 4.0D, 7.0D, 13.0D},
                {12.0D, 1.0D, 3.0D, 13.0D, 7.0D, 13.0D},
                {4.0D, 1.0D, 13.0D, 12.0D, 7.0D, 14.0D},
                {4.0D, 1.0D, 2.0D, 12.0D, 7.0D, 3.0D}
        };
        List<OutlineGeometry.Line> lines = new ArrayList<>(cuboids.length * 12);
        for (double[] cuboid : cuboids) {
            lines.addAll(OutlineGeometry.box(
                    cuboid[0], cuboid[1], cuboid[2],
                    cuboid[3], cuboid[4], cuboid[5]
            ).lines());
        }
        return OutlineGeometry.of(lines);
    }
}