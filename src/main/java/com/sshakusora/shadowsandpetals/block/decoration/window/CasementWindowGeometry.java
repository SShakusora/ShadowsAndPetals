package com.sshakusora.shadowsandpetals.block.decoration.window;

import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public final class CasementWindowGeometry {
    private static final VoxelShape CLOSED_NORTH = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape OPEN_LEFT_NORTH = Block.box(14, 0, 7, 16, 16, 23);
    private static final VoxelShape OPEN_RIGHT_NORTH = Block.box(0, 0, 7, 2, 16, 23);

    private static final Map<Direction, VoxelShape> CLOSED =
            VoxelShapeUtils.rotateHorizontal(CLOSED_NORTH);
    private static final Map<Direction, VoxelShape> OPEN_LEFT =
            VoxelShapeUtils.rotateHorizontal(OPEN_LEFT_NORTH);
    private static final Map<Direction, VoxelShape> OPEN_RIGHT =
            VoxelShapeUtils.rotateHorizontal(OPEN_RIGHT_NORTH);

    private CasementWindowGeometry() {
    }

    public static Direction partnerDirection(
            Direction facing,
            CasementWindowSide side
    ) {
        return side == CasementWindowSide.RIGHT
                ? facing.getClockWise()
                : facing.getCounterClockWise();
    }

    public static BlockPos partnerPosition(
            BlockPos pos,
            Direction facing,
            CasementWindowSide side
    ) {
        return pos.relative(partnerDirection(facing, side));
    }

    public static Direction hingeDirection(
            Direction facing,
            CasementWindowSide side
    ) {
        return partnerDirection(facing, side).getOpposite();
    }

    public static VoxelShape shape(
            Direction facing,
            CasementWindowSide side,
            boolean open
    ) {
        if (!open) {
            return CLOSED.get(facing);
        }
        return side == CasementWindowSide.LEFT
                ? OPEN_LEFT.get(facing)
                : OPEN_RIGHT.get(facing);
    }

    public static Vec3 pivot(Direction facing, CasementWindowSide side) {
        Direction hingeDirection = hingeDirection(facing, side);
        double offset = 7.0D / 16.0D;
        return new Vec3(
                0.5D + hingeDirection.getStepX() * offset,
                0.5D,
                0.5D + hingeDirection.getStepZ() * offset
        );
    }

    public static float targetAngle(CasementWindowSide side) {
        return side == CasementWindowSide.LEFT ? 90.0F : -90.0F;
    }

    public static boolean sameFacing(
            BlockState first,
            BlockState second
    ) {
        return first.getValue(CasementWindowBlock.FACING)
                == second.getValue(CasementWindowBlock.FACING);
    }
}
