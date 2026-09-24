package com.sshakusora.shadowsandpetals.block.decoration.sofa;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.block.decoration.AbstractSeatBlock;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/** A one-block sofa section whose rendered shape is chosen by the connection planner. */
public class SofaBlock extends AbstractSeatBlock {
    public static final MapCodec<SofaBlock> CODEC = simpleCodec(SofaBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<SofaShape> SHAPE = EnumProperty.create("shape", SofaShape.class);

    private static final double SEAT_HEIGHT = 0.5D;

    private static final VoxelShape NORTH_SINGLE = Shapes.or(
            Block.box(13.5D, 0.0D, 0.5D, 15.5D, 4.0D, 14.5D),
            Block.box(13.0D, 4.0D, 0.5D, 17.0D, 11.0D, 14.5D),
            Block.box(0.5D, 0.0D, 0.5D, 2.5D, 4.0D, 14.5D),
            Block.box(-1.0D, 4.0D, 0.5D, 3.0D, 11.0D, 14.5D),
            Block.box(2.5D, 0.5D, 0.5D, 13.5D, 4.5D, 14.5D),
            Block.box(1.0D, 0.025D, 14.0D, 15.0D, 15.025D, 16.0D),
            Block.box(1.0D, 5.025D, 12.0D, 15.0D, 15.025D, 14.0D),
            Block.box(2.5D, 4.0D, 0.0D, 13.5D, 7.0D, 12.0D)
    ).optimize();

    private static final VoxelShape NORTH_CENTER = Shapes.or(
            Block.box(0.0D, 0.5D, 0.55D, 16.0D, 4.5D, 14.55D),
            Block.box(0.0D, 5.025D, 12.0D, 16.0D, 15.025D, 14.0D),
            Block.box(0.0D, 4.0D, 0.0D, 16.0D, 7.0D, 12.0D),
            Block.box(0.0D, 0.025D, 14.0D, 16.0D, 15.025D, 16.0D)
    ).optimize();

    private static final VoxelShape NORTH_LEFT_EDGE = Shapes.or(
            Block.box(0.5D, 0.0D, 0.5D, 2.5D, 4.0D, 14.5D),
            Block.box(-1.0D, 4.0D, 0.5D, 3.0D, 11.0D, 14.5D),
            Block.box(2.0D, 0.5D, 0.55D, 16.0D, 4.5D, 14.55D),
            Block.box(1.0D, 0.025D, 14.0D, 16.0D, 15.025D, 16.0D),
            Block.box(1.0D, 5.025D, 12.0D, 16.0D, 15.025D, 14.0D),
            Block.box(2.0D, 4.0D, 0.0D, 16.0D, 7.0D, 12.0D)
    ).optimize();

    private static final VoxelShape NORTH_RIGHT_EDGE = Shapes.or(
            Block.box(13.5D, 0.0D, 0.5D, 15.5D, 4.0D, 14.5D),
            Block.box(13.0D, 4.0D, 0.5D, 17.0D, 11.0D, 14.5D),
            Block.box(0.0D, 0.5D, 0.55D, 14.0D, 4.5D, 14.55D),
            Block.box(0.0D, 0.025D, 14.0D, 15.0D, 15.025D, 16.0D),
            Block.box(0.0D, 5.025D, 12.0D, 15.0D, 15.025D, 14.0D),
            Block.box(0.0D, 4.0D, 0.0D, 14.0D, 7.0D, 12.0D)
    ).optimize();

    /** The unrotated contents of sofa_inner_corner.json. */
    private static final VoxelShape INNER_CORNER_MODEL = Shapes.or(
            Block.box(4.0D, 4.0D, 0.0D, 16.0D, 7.0D, 12.0D),
            Block.box(0.0D, 0.025D, 0.0D, 2.0D, 15.025D, 14.0D),
            Block.box(2.0D, 6.025D, 0.0D, 4.0D, 15.025D, 12.0D),
            Block.box(2.0D, 0.025D, 14.0D, 16.0D, 15.025D, 16.0D),
            Block.box(0.0D, 0.025D, 14.0D, 2.0D, 15.025D, 16.0D),
            Block.box(4.0D, 6.025D, 12.0D, 16.0D, 15.025D, 14.0D),
            Block.box(14.0D, 0.0D, 0.0D, 16.0D, 0.5D, 2.0D),
            Block.box(2.0D, 0.5D, 0.0D, 16.0D, 4.5D, 14.0D),
            Block.box(2.0D, 13.025D, 12.0D, 4.0D, 15.025D, 14.0D)
    ).optimize();

    /** The unrotated contents of sofa_outer_corner.json. */
    private static final VoxelShape OUTER_CORNER_MODEL = Shapes.or(
            Block.box(14.0D, 0.025D, 14.0D, 16.0D, 15.025D, 16.0D),
            Block.box(14.0D, 5.025D, 12.0D, 16.0D, 15.025D, 14.0D),
            Block.box(12.0D, 4.0D, 0.0D, 16.0D, 7.0D, 12.0D),
            Block.box(5.5D, 0.5D, 0.55D, 16.0D, 4.5D, 5.55D),
            Block.box(5.5D, 0.5D, 5.55D, 16.0D, 4.5D, 16.0D),
            Block.box(0.55D, 0.5D, 5.55D, 5.5D, 4.5D, 16.0D),
            Block.box(0.55D, 0.5D, 0.55D, 5.5D, 4.5D, 5.55D),
            Block.box(0.0D, 4.0D, 12.0D, 12.0D, 7.0D, 16.0D),
            Block.box(0.0D, 4.0D, 0.0D, 12.0D, 7.0D, 12.0D),
            Block.box(12.0D, 5.025D, 14.0D, 14.0D, 15.025D, 16.0D),
            Block.box(12.0D, 5.025D, 12.0D, 14.0D, 15.025D, 14.0D),
            Block.box(0.75D, 0.0D, 0.75D, 2.75D, 1.0D, 2.75D)
    ).optimize();

    private static final VoxelShape NORTH_INNER_LEFT = rotateModel(INNER_CORNER_MODEL, 270);
    private static final VoxelShape NORTH_INNER_RIGHT = INNER_CORNER_MODEL;
    private static final VoxelShape NORTH_OUTER_LEFT = OUTER_CORNER_MODEL;
    private static final VoxelShape NORTH_OUTER_RIGHT = rotateModel(OUTER_CORNER_MODEL, 90);

    private static final Map<Direction, VoxelShape> SINGLE_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_SINGLE);
    private static final Map<Direction, VoxelShape> CENTER_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_CENTER);
    private static final Map<Direction, VoxelShape> LEFT_EDGE_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_LEFT_EDGE);
    private static final Map<Direction, VoxelShape> RIGHT_EDGE_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_RIGHT_EDGE);
    private static final Map<Direction, VoxelShape> INNER_LEFT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_INNER_LEFT);
    private static final Map<Direction, VoxelShape> INNER_RIGHT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_INNER_RIGHT);
    private static final Map<Direction, VoxelShape> OUTER_LEFT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OUTER_LEFT);
    private static final Map<Direction, VoxelShape> OUTER_RIGHT_SHAPES =
            VoxelShapeUtils.rotateHorizontal(NORTH_OUTER_RIGHT);

    public SofaBlock(BlockBehaviour.Properties properties) {
        super(properties, SEAT_HEIGHT);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, SofaShape.SINGLE)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<SofaBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(SHAPE, SofaShape.SINGLE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, SHAPE);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel
                && oldState.getBlock() != state.getBlock()) {
            SofaConnectionManager.reconcileAfterPlacement(serverLevel, pos);
        }
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            // BlockItem may apply a BLOCK_STATE component between onPlace and
            // setPlacedBy. Reconcile again only when that final state differs
            // from the state already processed by onPlace.
            SofaConnectionManager.reconcileAfterItemPlacement(serverLevel, pos, state);
        }
    }

    /**
     * Recomputes the affected connection components after this sofa has been
     * removed. The removal is never cancelled; invalid ports are closed by
     * converting affected sections to a representable edge, corner, or single
     * shape.
     */
    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        SofaConnectionManager.reconcileAfterRemoval(level, pos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        return mirror == Mirror.NONE
                ? mirrored
                : mirrored.setValue(SHAPE, mirrorShape(state.getValue(SHAPE)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context
    ) {
        return shapeFor(state);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5F);
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        Vec3 deltaMovement = entity.getDeltaMovement();
        if (deltaMovement.y < 0.0D) {
            double bounceScale = entity instanceof LivingEntity ? 1.0D : 0.8D;
            entity.setDeltaMovement(deltaMovement.x, -deltaMovement.y * 0.66D * bounceScale, deltaMovement.z);
        }
    }

    private static SofaShape mirrorShape(SofaShape shape) {
        return switch (shape) {
            case LEFT_EDGE -> SofaShape.RIGHT_EDGE;
            case RIGHT_EDGE -> SofaShape.LEFT_EDGE;
            case INNER_LEFT -> SofaShape.INNER_RIGHT;
            case INNER_RIGHT -> SofaShape.INNER_LEFT;
            case OUTER_LEFT -> SofaShape.OUTER_RIGHT;
            case OUTER_RIGHT -> SofaShape.OUTER_LEFT;
            default -> shape;
        };
    }

    private static VoxelShape shapeFor(BlockState state) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(SHAPE)) {
            case SINGLE -> SINGLE_SHAPES.get(facing);
            case CENTER -> CENTER_SHAPES.get(facing);
            case LEFT_EDGE -> LEFT_EDGE_SHAPES.get(facing);
            case RIGHT_EDGE -> RIGHT_EDGE_SHAPES.get(facing);
            case INNER_LEFT -> INNER_LEFT_SHAPES.get(facing);
            case INNER_RIGHT -> INNER_RIGHT_SHAPES.get(facing);
            case OUTER_LEFT -> OUTER_LEFT_SHAPES.get(facing);
            case OUTER_RIGHT -> OUTER_RIGHT_SHAPES.get(facing);
        };
    }

    private static VoxelShape rotateModel(VoxelShape shape, int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> shape;
            case 90 -> VoxelShapeUtils.rotateHorizontal(shape).get(Direction.EAST);
            case 180 -> VoxelShapeUtils.rotateHorizontal(shape).get(Direction.SOUTH);
            case 270 -> VoxelShapeUtils.rotateHorizontal(shape).get(Direction.WEST);
            default -> throw new IllegalArgumentException("Sofa model rotation must be a quarter turn");
        };
    }

    public enum SofaShape implements StringRepresentable {
        SINGLE("single", "", 0, Set.of()),
        LEFT_EDGE("left_edge", "left_edge", 0, Set.of(new SofaPort(Direction.EAST, Direction.SOUTH))),
        RIGHT_EDGE("right_edge", "right_edge", 0, Set.of(new SofaPort(Direction.WEST, Direction.SOUTH))),
        CENTER("center", "center", 0, Set.of(
                new SofaPort(Direction.EAST, Direction.SOUTH),
                new SofaPort(Direction.WEST, Direction.SOUTH))),
        INNER_LEFT("inner_left", "inner_corner", 270, Set.of(
                new SofaPort(Direction.NORTH, Direction.WEST),
                new SofaPort(Direction.EAST, Direction.SOUTH))),
        INNER_RIGHT("inner_right", "inner_corner", 0, Set.of(
                new SofaPort(Direction.NORTH, Direction.WEST),
                new SofaPort(Direction.EAST, Direction.SOUTH))),
        OUTER_LEFT("outer_left", "outer_corner", 0, Set.of(
                new SofaPort(Direction.SOUTH, Direction.EAST),
                new SofaPort(Direction.EAST, Direction.SOUTH))),
        OUTER_RIGHT("outer_right", "outer_corner", 90, Set.of(
                new SofaPort(Direction.SOUTH, Direction.EAST),
                new SofaPort(Direction.EAST, Direction.SOUTH)));

        private final String name;
        private final String modelSuffix;
        private final int modelRotationDegrees;
        private final Set<SofaPort> localPorts;

        SofaShape(
                String name,
                String modelSuffix,
                int modelRotationDegrees,
                Set<SofaPort> localPorts
        ) {
            this.name = name;
            this.modelSuffix = modelSuffix;
            this.modelRotationDegrees = modelRotationDegrees;
            this.localPorts = localPorts;
        }

        /**
         * Returns the extra clockwise model rotation used by the asymmetric
         * corner mesh, in degrees. This is shared by blockstate generation
         * and connection validation so the topology follows the rendered
         * model if these rotations ever change.
         */
        public int modelRotationDegrees() {
            return modelRotationDegrees;
        }

        /** Returns the complete blockstate Y rotation for this shape. */
        public int modelRotationDegrees(Direction facing) {
            int facingRotation = switch (facing) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> throw new IllegalArgumentException(
                        "Sofa facing must be horizontal: " + facing);
            };
            return facingRotation + modelRotationDegrees();
        }

        /** Returns the physical model suffix used by data generation. */
        public String modelSuffix() {
            return modelSuffix;
        }

        /** Returns the complete set of local model ports before rotation. */
        Set<SofaPort> localPorts() {
            return localPorts;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
