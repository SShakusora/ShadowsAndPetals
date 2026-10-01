package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** A single-cell curtain using the upper panel of the long-curtain rig. */
public class CurtainBlock extends AbstractCurtainBlock {
    public static final MapCodec<CurtainBlock> CODEC = simpleCodec(CurtainBlock::new);

    private static final VoxelShape NORTH_CLOSED = toShape(CurtainGeometry.closed());
    private static final VoxelShape NORTH_OPEN_RIGHT = Shapes.or(
            toShape(CurtainGeometry.open(false)),
            Block.box(0, 14, 14, 16, 16, 16)
    ).optimize();
    private static final VoxelShape NORTH_OPEN_LEFT = Shapes.or(
            toShape(CurtainGeometry.open(true)),
            Block.box(0, 14, 14, 16, 16, 16)
    ).optimize();
    private static final Map<Direction, VoxelShape> CLOSED_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_CLOSED);
    private static final Map<Direction, VoxelShape> OPEN_RIGHT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_RIGHT);
    private static final Map<Direction, VoxelShape> OPEN_LEFT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OPEN_LEFT);

    private static VoxelShape toShape(CurtainGeometry.CollisionBox box) {
        return Block.box(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    public CurtainBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends CurtainBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (!level.getBlockState(pos).canBeReplaced(context)) {
            return null;
        }
        Direction facing = context.getHorizontalDirection().getOpposite();
        CurtainSide side = CurtainStructure.sideForPlacement(
                level,
                pos,
                facing,
                context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()
        );
        boolean powered = level.hasNeighborSignal(pos);
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SIDE, side)
                .setValue(POWERED, powered)
                .setValue(OPEN, powered);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return CLOSED_SHAPES.get(state.getValue(FACING));
        }
        Map<Direction, VoxelShape> shapes = state.getValue(SIDE) == CurtainSide.LEFT
                ? OPEN_LEFT_SHAPES : OPEN_RIGHT_SHAPES;
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.CURTAIN.get().create(pos, state);
    }
}
