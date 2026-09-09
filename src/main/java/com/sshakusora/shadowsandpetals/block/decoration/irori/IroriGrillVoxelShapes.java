package com.sshakusora.shadowsandpetals.block.decoration.irori;

import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

/**
 * Geometry for the split grill models.
 *
 * <p>The coordinates in this class are the cuboid coordinates exported by the
 * generated {@code grill/double/*} models. The detailed upper geometry is kept
 * for selection outlines, while the physical upper surface is represented by
 * the model geometry intersecting one one-pixel-thick Y slice. For each Z slice,
 * only the outermost X extents are retained, so internal grill bars do not add
 * internal edges to the physical surface.</p>
 */
final class IroriGrillVoxelShapes {
    private static final double UPPER_SURFACE_MIN_Y = 4.0D;
    private static final double UPPER_SURFACE_MAX_Y = 5.0D;

    private static final VoxelShape SINGLE_LOWER_SHAPE = shape(
            3.0D, 14.0D, 3.0D, 4.0D, 16.0D, 4.0D,
            3.0D, 14.0D, 12.0D, 4.0D, 16.0D, 13.0D,
            12.0D, 14.0D, 3.0D, 13.0D, 16.0D, 4.0D,
            12.0D, 14.0D, 12.0D, 13.0D, 16.0D, 13.0D,
            3.0D, 10.0D, 3.0D, 4.0D, 14.0D, 4.0D,
            12.0D, 10.0D, 12.0D, 13.0D, 14.0D, 13.0D,
            12.0D, 10.0D, 3.0D, 13.0D, 14.0D, 4.0D,
            3.0D, 10.0D, 12.0D, 4.0D, 14.0D, 13.0D
    );

    private static final VoxelShape SINGLE_UPPER_SHAPE = shape(
            1.0D, 3.5D, 2.0D, 15.0D, 5.5D, 3.0D,
            1.0D, 3.5D, 13.0D, 15.0D, 5.5D, 14.0D,
            7.0D, 4.0D, 0.0D, 9.0D, 5.0D, 1.0D,
            9.0D, 4.0D, 0.0D, 10.0D, 5.0D, 2.0D,
            6.0D, 4.0D, 0.0D, 7.0D, 5.0D, 2.0D,
            9.0D, 4.0D, 14.0D, 10.0D, 5.0D, 16.0D,
            7.0D, 4.0D, 15.0D, 9.0D, 5.0D, 16.0D,
            6.0D, 4.0D, 14.0D, 7.0D, 5.0D, 16.0D,
            1.5D, 0.5D, 2.5D, 14.5D, 1.5D, 3.5D,
            1.5D, 0.5D, 12.5D, 14.5D, 1.5D, 13.5D,
            1.5D, 2.5D, 3.0D, 2.5D, 3.5D, 13.0D,
            13.5D, 2.5D, 3.0D, 14.5D, 3.5D, 13.0D,
            1.5D, 4.0D, 3.0D, 2.5D, 5.0D, 13.0D,
            3.5D, 4.0D, 3.0D, 4.5D, 5.0D, 13.0D,
            5.5D, 4.0D, 3.0D, 6.5D, 5.0D, 13.0D,
            7.5D, 4.0D, 3.0D, 8.5D, 5.0D, 13.0D,
            9.5D, 4.0D, 3.0D, 10.5D, 5.0D, 13.0D,
            11.5D, 4.0D, 3.0D, 12.5D, 5.0D, 13.0D,
            13.5D, 4.0D, 3.0D, 14.5D, 5.0D, 13.0D,
            3.0D, 0.0D, 3.0D, 4.0D, 4.0D, 4.0D,
            3.0D, 0.0D, 12.0D, 4.0D, 4.0D, 13.0D,
            12.0D, 0.0D, 3.0D, 13.0D, 4.0D, 4.0D,
            12.0D, 0.0D, 12.0D, 13.0D, 4.0D, 13.0D
    );

    private static final VoxelShape STRIP_NORTH_LOWER_SHAPE = shape(
            11.0D, 14.0D, 4.0D, 12.0D, 16.0D, 5.0D,
            4.0D, 14.0D, 4.0D, 5.0D, 16.0D, 5.0D,
            4.0D, 10.0D, 4.0D, 5.0D, 14.0D, 5.0D,
            11.0D, 10.0D, 4.0D, 12.0D, 14.0D, 5.0D
    );

    private static final VoxelShape STRIP_NORTH_UPPER_SHAPE = shape(
            11.75D, 2.5D, 13.5D, 12.75D, 3.5D, 16.0D,
            11.0D, 0.0D, 4.0D, 12.0D, 4.0D, 5.0D,
            4.0D, 0.0D, 4.0D, 5.0D, 4.0D, 5.0D,
            13.5D, 4.0D, 4.0D, 14.5D, 5.0D, 14.0D,
            11.5D, 4.0D, 4.0D, 12.5D, 5.0D, 14.0D,
            9.5D, 4.0D, 4.0D, 10.5D, 5.0D, 14.0D,
            7.5D, 4.0D, 4.0D, 8.5D, 5.0D, 14.0D,
            5.5D, 4.0D, 4.0D, 6.5D, 5.0D, 14.0D,
            3.5D, 4.0D, 4.0D, 4.5D, 5.0D, 14.0D,
            1.5D, 4.0D, 4.0D, 2.5D, 5.0D, 14.0D,
            11.75D, 2.5D, 3.5D, 12.75D, 3.5D, 13.5D,
            3.25D, 2.5D, 13.5D, 4.25D, 3.5D, 16.0D,
            3.25D, 2.5D, 3.5D, 4.25D, 3.5D, 13.5D,
            2.5D, 0.5D, 3.5D, 13.5D, 1.5D, 4.5D,
            6.0D, 4.0D, 1.0D, 7.0D, 5.0D, 3.0D,
            9.0D, 4.0D, 1.0D, 10.0D, 5.0D, 3.0D,
            7.0D, 4.0D, 1.0D, 9.0D, 5.0D, 2.0D,
            1.0D, 3.5D, 3.0D, 15.0D, 5.5D, 4.0D,
            1.5D, 4.0D, 14.0D, 2.5D, 5.0D, 16.0D,
            3.5D, 4.0D, 14.0D, 4.5D, 5.0D, 16.0D,
            5.5D, 4.0D, 14.0D, 6.5D, 5.0D, 16.0D,
            7.5D, 4.0D, 14.0D, 8.5D, 5.0D, 16.0D,
            9.5D, 4.0D, 14.0D, 10.5D, 5.0D, 16.0D,
            11.5D, 4.0D, 14.0D, 12.5D, 5.0D, 16.0D,
            13.5D, 4.0D, 14.0D, 14.5D, 5.0D, 16.0D
    );

    private static final VoxelShape STRIP_SOUTH_LOWER_SHAPE = shape(
            11.0D, 14.0D, 11.0D, 12.0D, 16.0D, 12.0D,
            4.0D, 14.0D, 11.0D, 5.0D, 16.0D, 12.0D,
            11.0D, 10.0D, 11.0D, 12.0D, 14.0D, 12.0D,
            4.0D, 10.0D, 11.0D, 5.0D, 14.0D, 12.0D
    );

    private static final VoxelShape STRIP_SOUTH_UPPER_SHAPE = shape(
            11.75D, 2.5D, 0.0D, 12.75D, 3.5D, 2.5D,
            11.0D, 0.0D, 11.0D, 12.0D, 4.0D, 12.0D,
            4.0D, 0.0D, 11.0D, 5.0D, 4.0D, 12.0D,
            1.5D, 4.0D, 2.0D, 2.5D, 5.0D, 12.0D,
            3.5D, 4.0D, 2.0D, 4.5D, 5.0D, 12.0D,
            5.5D, 4.0D, 2.0D, 6.5D, 5.0D, 12.0D,
            7.5D, 4.0D, 2.0D, 8.5D, 5.0D, 12.0D,
            9.5D, 4.0D, 2.0D, 10.5D, 5.0D, 12.0D,
            11.5D, 4.0D, 2.0D, 12.5D, 5.0D, 12.0D,
            13.5D, 4.0D, 2.0D, 14.5D, 5.0D, 12.0D,
            11.75D, 2.5D, 2.5D, 12.75D, 3.5D, 12.5D,
            3.25D, 2.5D, 2.5D, 4.25D, 3.5D, 12.5D,
            3.25D, 2.5D, 0.0D, 4.25D, 3.5D, 2.5D,
            2.5D, 0.5D, 11.5D, 13.5D, 1.5D, 12.5D,
            6.0D, 4.0D, 13.0D, 7.0D, 5.0D, 15.0D,
            7.0D, 4.0D, 14.0D, 9.0D, 5.0D, 15.0D,
            9.0D, 4.0D, 13.0D, 10.0D, 5.0D, 15.0D,
            1.0D, 3.5D, 12.0D, 15.0D, 5.5D, 13.0D,
            1.5D, 4.0D, 0.0D, 2.5D, 5.0D, 2.0D,
            3.5D, 4.0D, 0.0D, 4.5D, 5.0D, 2.0D,
            5.5D, 4.0D, 0.0D, 6.5D, 5.0D, 2.0D,
            7.5D, 4.0D, 0.0D, 8.5D, 5.0D, 2.0D,
            9.5D, 4.0D, 0.0D, 10.5D, 5.0D, 2.0D,
            11.5D, 4.0D, 0.0D, 12.5D, 5.0D, 2.0D,
            13.5D, 4.0D, 0.0D, 14.5D, 5.0D, 2.0D
    );

    private static final VoxelShape QUAD_NORTH_WEST_LOWER_SHAPE = shape(
            15.5D, 14.0D, 4.0D, 16.0D, 16.0D, 5.0D,
            4.0D, 14.0D, 4.0D, 5.0D, 16.0D, 5.0D,
            4.0D, 10.0D, 4.0D, 5.0D, 14.0D, 5.0D,
            15.5D, 10.0D, 4.0D, 16.0D, 14.0D, 5.0D
    );

    private static final VoxelShape QUAD_NORTH_WEST_UPPER_SHAPE = shape(
            2.5D, 4.0D, 4.0D, 3.5D, 5.0D, 14.0D,
            2.5D, 2.5D, 4.0D, 3.5D, 3.5D, 14.0D,
            4.5D, 4.0D, 4.0D, 5.5D, 5.0D, 14.0D,
            6.5D, 4.0D, 4.0D, 7.5D, 5.0D, 14.0D,
            8.5D, 4.0D, 4.0D, 9.5D, 5.0D, 14.0D,
            10.5D, 4.0D, 4.0D, 11.5D, 5.0D, 14.0D,
            12.5D, 4.0D, 4.0D, 13.5D, 5.0D, 14.0D,
            14.5D, 4.0D, 4.0D, 15.5D, 5.0D, 14.0D,
            14.5D, 2.5D, 4.0D, 15.5D, 3.5D, 14.0D,
            2.5D, 4.0D, 14.0D, 3.5D, 5.0D, 16.0D,
            4.5D, 4.0D, 14.0D, 5.5D, 5.0D, 16.0D,
            6.5D, 4.0D, 14.0D, 7.5D, 5.0D, 16.0D,
            8.5D, 4.0D, 14.0D, 9.5D, 5.0D, 16.0D,
            10.5D, 4.0D, 14.0D, 11.5D, 5.0D, 16.0D,
            12.5D, 4.0D, 14.0D, 13.5D, 5.0D, 16.0D,
            14.5D, 4.0D, 14.0D, 15.5D, 5.0D, 16.0D,
            2.5D, 2.5D, 14.0D, 3.5D, 3.5D, 16.0D,
            14.5D, 2.5D, 14.0D, 15.5D, 3.5D, 16.0D,
            2.0D, 3.5D, 3.0D, 16.0D, 5.5D, 4.0D,
            2.75D, 0.5D, 3.0D, 15.75D, 1.5D, 4.0D,
            15.5D, 0.0D, 4.0D, 16.0D, 4.0D, 5.0D,
            4.0D, 0.0D, 4.0D, 5.0D, 4.0D, 5.0D,
            7.0D, 4.0D, 1.0D, 8.0D, 5.0D, 3.0D,
            8.0D, 4.0D, 1.0D, 10.0D, 5.0D, 2.0D,
            10.0D, 4.0D, 1.0D, 11.0D, 5.0D, 3.0D
    );

    private static final VoxelShape QUAD_NORTH_EAST_LOWER_SHAPE = shape(
            11.0D, 14.0D, 4.0D, 12.0D, 16.0D, 5.0D,
            0.0D, 14.0D, 4.0D, 0.5D, 16.0D, 5.0D,
            0.0D, 10.0D, 4.0D, 0.5D, 14.0D, 5.0D,
            11.0D, 10.0D, 4.0D, 12.0D, 14.0D, 5.0D
    );

    private static final VoxelShape QUAD_NORTH_EAST_UPPER_SHAPE = shape(
            0.0D, 3.5D, 3.0D, 14.0D, 5.5D, 4.0D,
            0.5D, 4.0D, 4.0D, 1.5D, 5.0D, 14.0D,
            8.0D, 4.0D, 1.0D, 9.0D, 5.0D, 3.0D,
            6.0D, 4.0D, 1.0D, 8.0D, 5.0D, 2.0D,
            0.25D, 0.5D, 3.0D, 13.25D, 1.5D, 4.0D,
            0.5D, 2.5D, 4.0D, 1.5D, 3.5D, 14.0D,
            5.0D, 4.0D, 1.0D, 6.0D, 5.0D, 3.0D,
            2.5D, 4.0D, 4.0D, 3.5D, 5.0D, 14.0D,
            4.5D, 4.0D, 4.0D, 5.5D, 5.0D, 14.0D,
            6.5D, 4.0D, 4.0D, 7.5D, 5.0D, 14.0D,
            8.5D, 4.0D, 4.0D, 9.5D, 5.0D, 14.0D,
            10.5D, 4.0D, 4.0D, 11.5D, 5.0D, 14.0D,
            12.5D, 4.0D, 4.0D, 13.5D, 5.0D, 14.0D,
            11.0D, 0.0D, 4.0D, 12.0D, 4.0D, 5.0D,
            12.5D, 2.5D, 4.0D, 13.5D, 3.5D, 14.0D,
            0.5D, 4.0D, 14.0D, 1.5D, 5.0D, 16.0D,
            2.5D, 4.0D, 14.0D, 3.5D, 5.0D, 16.0D,
            4.5D, 4.0D, 14.0D, 5.5D, 5.0D, 16.0D,
            6.5D, 4.0D, 14.0D, 7.5D, 5.0D, 16.0D,
            8.5D, 4.0D, 14.0D, 9.5D, 5.0D, 16.0D,
            10.5D, 4.0D, 14.0D, 11.5D, 5.0D, 16.0D,
            12.5D, 4.0D, 14.0D, 13.5D, 5.0D, 16.0D,
            0.5D, 2.5D, 14.0D, 1.5D, 3.5D, 16.0D,
            12.5D, 2.5D, 14.0D, 13.5D, 3.5D, 16.0D,
            0.0D, 0.0D, 4.0D, 0.5D, 4.0D, 5.0D
    );

    private static final VoxelShape QUAD_SOUTH_WEST_LOWER_SHAPE = shape(
            4.0D, 14.0D, 11.0D, 5.0D, 16.0D, 12.0D,
            15.5D, 14.0D, 11.0D, 16.0D, 16.0D, 12.0D,
            4.0D, 10.0D, 11.0D, 5.0D, 14.0D, 12.0D,
            15.5D, 10.0D, 11.0D, 16.0D, 14.0D, 12.0D
    );

    private static final VoxelShape QUAD_SOUTH_WEST_UPPER_SHAPE = shape(
            2.5D, 4.0D, 2.0D, 3.5D, 5.0D, 12.0D,
            2.5D, 2.5D, 2.0D, 3.5D, 3.5D, 12.0D,
            4.5D, 4.0D, 2.0D, 5.5D, 5.0D, 12.0D,
            6.5D, 4.0D, 2.0D, 7.5D, 5.0D, 12.0D,
            8.5D, 4.0D, 2.0D, 9.5D, 5.0D, 12.0D,
            10.5D, 4.0D, 2.0D, 11.5D, 5.0D, 12.0D,
            12.5D, 4.0D, 2.0D, 13.5D, 5.0D, 12.0D,
            14.5D, 4.0D, 2.0D, 15.5D, 5.0D, 12.0D,
            14.5D, 2.5D, 2.0D, 15.5D, 3.5D, 12.0D,
            2.5D, 4.0D, 0.0D, 3.5D, 5.0D, 2.0D,
            4.5D, 4.0D, 0.0D, 5.5D, 5.0D, 2.0D,
            6.5D, 4.0D, 0.0D, 7.5D, 5.0D, 2.0D,
            8.5D, 4.0D, 0.0D, 9.5D, 5.0D, 2.0D,
            10.5D, 4.0D, 0.0D, 11.5D, 5.0D, 2.0D,
            12.5D, 4.0D, 0.0D, 13.5D, 5.0D, 2.0D,
            14.5D, 4.0D, 0.0D, 15.5D, 5.0D, 2.0D,
            2.5D, 2.5D, 0.0D, 3.5D, 3.5D, 2.0D,
            14.5D, 2.5D, 0.0D, 15.5D, 3.5D, 2.0D,
            4.0D, 0.0D, 11.0D, 5.0D, 4.0D, 12.0D,
            2.0D, 3.5D, 12.0D, 16.0D, 5.5D, 13.0D,
            8.0D, 4.0D, 14.0D, 10.0D, 5.0D, 15.0D,
            7.0D, 4.0D, 13.0D, 8.0D, 5.0D, 15.0D,
            10.0D, 4.0D, 13.0D, 11.0D, 5.0D, 15.0D,
            15.5D, 0.0D, 11.0D, 16.0D, 4.0D, 12.0D,
            2.75D, 0.5D, 12.0D, 15.75D, 1.5D, 13.0D
    );

    private static final VoxelShape QUAD_SOUTH_EAST_LOWER_SHAPE = shape(
            11.0D, 14.0D, 11.0D, 12.0D, 16.0D, 12.0D,
            0.0D, 14.0D, 11.0D, 0.5D, 16.0D, 12.0D,
            0.0D, 10.0D, 11.0D, 0.5D, 14.0D, 12.0D,
            11.0D, 10.0D, 11.0D, 12.0D, 14.0D, 12.0D
    );

    private static final VoxelShape QUAD_SOUTH_EAST_UPPER_SHAPE = shape(
            5.0D, 4.0D, 13.0D, 6.0D, 5.0D, 15.0D,
            6.0D, 4.0D, 14.0D, 8.0D, 5.0D, 15.0D,
            8.0D, 4.0D, 13.0D, 9.0D, 5.0D, 15.0D,
            11.0D, 0.0D, 11.0D, 12.0D, 4.0D, 12.0D,
            0.0D, 3.5D, 12.0D, 14.0D, 5.5D, 13.0D,
            0.25D, 0.5D, 12.0D, 13.25D, 1.5D, 13.0D,
            0.5D, 4.0D, 2.0D, 1.5D, 5.0D, 12.0D,
            0.5D, 2.5D, 2.0D, 1.5D, 3.5D, 12.0D,
            2.5D, 4.0D, 2.0D, 3.5D, 5.0D, 12.0D,
            4.5D, 4.0D, 2.0D, 5.5D, 5.0D, 12.0D,
            6.5D, 4.0D, 2.0D, 7.5D, 5.0D, 12.0D,
            8.5D, 4.0D, 2.0D, 9.5D, 5.0D, 12.0D,
            10.5D, 4.0D, 2.0D, 11.5D, 5.0D, 12.0D,
            12.5D, 4.0D, 2.0D, 13.5D, 5.0D, 12.0D,
            12.5D, 2.5D, 2.0D, 13.5D, 3.5D, 12.0D,
            0.5D, 4.0D, 0.0D, 1.5D, 5.0D, 2.0D,
            2.5D, 4.0D, 0.0D, 3.5D, 5.0D, 2.0D,
            4.5D, 4.0D, 0.0D, 5.5D, 5.0D, 2.0D,
            6.5D, 4.0D, 0.0D, 7.5D, 5.0D, 2.0D,
            8.5D, 4.0D, 0.0D, 9.5D, 5.0D, 2.0D,
            10.5D, 4.0D, 0.0D, 11.5D, 5.0D, 2.0D,
            12.5D, 4.0D, 0.0D, 13.5D, 5.0D, 2.0D,
            0.5D, 2.5D, 0.0D, 1.5D, 3.5D, 2.0D,
            12.5D, 2.5D, 0.0D, 13.5D, 3.5D, 2.0D,
            0.0D, 0.0D, 11.0D, 0.5D, 4.0D, 12.0D
    );

    private static final Map<Direction, VoxelShape> STRIP_NORTH_LOWER_ROTATED =
            VoxelShapeUtils.rotateHorizontal(STRIP_NORTH_LOWER_SHAPE);
    private static final Map<Direction, VoxelShape> STRIP_SOUTH_LOWER_ROTATED =
            VoxelShapeUtils.rotateHorizontal(STRIP_SOUTH_LOWER_SHAPE);
    private static final Map<Direction, VoxelShape> STRIP_NORTH_UPPER_ROTATED =
            VoxelShapeUtils.rotateHorizontal(STRIP_NORTH_UPPER_SHAPE);
    private static final Map<Direction, VoxelShape> STRIP_SOUTH_UPPER_ROTATED =
            VoxelShapeUtils.rotateHorizontal(STRIP_SOUTH_UPPER_SHAPE);

    private static final Map<IroriGrillPart, VoxelShape> LOWER_SHAPES = Map.of(
            IroriGrillPart.SINGLE, SINGLE_LOWER_SHAPE,
            IroriGrillPart.STRIP_NORTH, STRIP_NORTH_LOWER_SHAPE,
            IroriGrillPart.STRIP_SOUTH, STRIP_SOUTH_LOWER_SHAPE,
            IroriGrillPart.STRIP_WEST, STRIP_SOUTH_LOWER_ROTATED.get(Direction.EAST),
            IroriGrillPart.STRIP_EAST, STRIP_NORTH_LOWER_ROTATED.get(Direction.EAST),
            IroriGrillPart.QUAD_NORTH_WEST, QUAD_NORTH_WEST_LOWER_SHAPE,
            IroriGrillPart.QUAD_NORTH_EAST, QUAD_NORTH_EAST_LOWER_SHAPE,
            IroriGrillPart.QUAD_SOUTH_WEST, QUAD_SOUTH_WEST_LOWER_SHAPE,
            IroriGrillPart.QUAD_SOUTH_EAST, QUAD_SOUTH_EAST_LOWER_SHAPE
    );

    private static final Map<IroriGrillPart, VoxelShape> UPPER_SHAPES = Map.of(
            IroriGrillPart.SINGLE, SINGLE_UPPER_SHAPE,
            IroriGrillPart.STRIP_NORTH, STRIP_NORTH_UPPER_SHAPE,
            IroriGrillPart.STRIP_SOUTH, STRIP_SOUTH_UPPER_SHAPE,
            IroriGrillPart.STRIP_WEST, STRIP_SOUTH_UPPER_ROTATED.get(Direction.EAST),
            IroriGrillPart.STRIP_EAST, STRIP_NORTH_UPPER_ROTATED.get(Direction.EAST),
            IroriGrillPart.QUAD_NORTH_WEST, QUAD_NORTH_WEST_UPPER_SHAPE,
            IroriGrillPart.QUAD_NORTH_EAST, QUAD_NORTH_EAST_UPPER_SHAPE,
            IroriGrillPart.QUAD_SOUTH_WEST, QUAD_SOUTH_WEST_UPPER_SHAPE,
            IroriGrillPart.QUAD_SOUTH_EAST, QUAD_SOUTH_EAST_UPPER_SHAPE
    );
    private static final Map<IroriGrillPart, VoxelShape> UPPER_SURFACE_SHAPES =
            createUpperSurfaceShapes();

    private IroriGrillVoxelShapes() {
    }

    static VoxelShape lower(IroriGrillPart part) {
        return LOWER_SHAPES.get(part);
    }

    static VoxelShape upper(IroriGrillPart part) {
        return upperOutline(part);
    }

    static VoxelShape upperSurface(IroriGrillPart part) {
        return UPPER_SURFACE_SHAPES.get(part);
    }

    static VoxelShape upperOutline(IroriGrillPart part) {
        return Shapes.or(UPPER_SHAPES.get(part), upperSurface(part)).optimize();
    }

    private static Map<IroriGrillPart, VoxelShape> createUpperSurfaceShapes() {
        Map<IroriGrillPart, VoxelShape> surfaces = new EnumMap<>(IroriGrillPart.class);
        for (Map.Entry<IroriGrillPart, VoxelShape> entry : UPPER_SHAPES.entrySet()) {
            surfaces.put(entry.getKey(), projectToUpperSurface(entry.getValue()));
        }
        return Map.copyOf(surfaces);
    }

    private static VoxelShape projectToUpperSurface(VoxelShape modelShape) {
        double minY = UPPER_SURFACE_MIN_Y / 16.0D;
        double maxY = UPPER_SURFACE_MAX_Y / 16.0D;
        List<SurfaceBox> surfaceBoxes = new ArrayList<>();
        TreeSet<Double> zCoordinates = new TreeSet<>();
        modelShape.forAllBoxes((minX, boxMinY, minZ, maxX, boxMaxY, maxZ) -> {
            if (boxMinY < maxY && boxMaxY > minY) {
                surfaceBoxes.add(new SurfaceBox(minX, minZ, maxX, maxZ));
                zCoordinates.add(minZ);
                zCoordinates.add(maxZ);
            }
        });

        if (surfaceBoxes.isEmpty()) {
            return Shapes.empty();
        }

        List<Double> rows = List.copyOf(zCoordinates);
        VoxelShape[] projected = {Shapes.empty()};
        for (int index = 0; index + 1 < rows.size(); index++) {
            double rowMinZ = rows.get(index);
            double rowMaxZ = rows.get(index + 1);
            double rowMinX = Double.POSITIVE_INFINITY;
            double rowMaxX = Double.NEGATIVE_INFINITY;
            for (SurfaceBox box : surfaceBoxes) {
                if (box.minZ < rowMaxZ && box.maxZ > rowMinZ) {
                    rowMinX = Math.min(rowMinX, box.minX);
                    rowMaxX = Math.max(rowMaxX, box.maxX);
                }
            }
            if (rowMinX < rowMaxX) {
                projected[0] = Shapes.or(
                        projected[0],
                        Shapes.box(rowMinX, minY, rowMinZ, rowMaxX, maxY, rowMaxZ)
                );
            }
        }
        return projected[0].optimize();
    }

    private record SurfaceBox(double minX, double minZ, double maxX, double maxZ) {
    }

    private static VoxelShape shape(double... bounds) {
        if (bounds.length % 6 != 0) {
            throw new IllegalArgumentException("A grill shape must contain complete cuboid bounds");
        }
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < bounds.length; i += 6) {
            shape = Shapes.or(shape, Shapes.box(
                    bounds[i] / 16.0D, bounds[i + 1] / 16.0D, bounds[i + 2] / 16.0D,
                    bounds[i + 3] / 16.0D, bounds[i + 4] / 16.0D, bounds[i + 5] / 16.0D
            ));
        }
        return shape.optimize();
    }
}
