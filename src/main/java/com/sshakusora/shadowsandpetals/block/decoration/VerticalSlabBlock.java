package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class VerticalSlabBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<VerticalSlabBlock> CODEC = simpleCodec(VerticalSlabBlock::new);
    public static final EnumProperty<VerticalSlabType> TYPE = EnumProperty.create("type", VerticalSlabType.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public VerticalSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(TYPE, VerticalSlabType.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<? extends VerticalSlabBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(TYPE) != VerticalSlabType.DOUBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TYPE).shape();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState replacedState = context.getLevel().getBlockState(pos);
        if (replacedState.is(this)) {
            return replacedState
                    .setValue(TYPE, VerticalSlabType.DOUBLE)
                    .setValue(WATERLOGGED, false);
        }

        FluidState replacedFluidState = context.getLevel().getFluidState(pos);
        return defaultBlockState()
                .setValue(TYPE, VerticalSlabType.fromDirection(getDirectionForPlacement(context)))
                .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
    }

    private static Direction getDirectionForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis().isHorizontal()) {
            return clickedFace;
        }

        BlockPos pos = context.getClickedPos();
        Vec3 offset = context.getClickLocation()
                .subtract(pos.getX(), pos.getY(), pos.getZ())
                .subtract(0.5, 0.0, 0.5);
        double angle = Math.atan2(offset.x, offset.z) * -180.0 / Math.PI;
        return Direction.fromYRot(angle).getOpposite();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        VerticalSlabType type = state.getValue(TYPE);
        return type != VerticalSlabType.DOUBLE
                && stack.is(asItem())
                && ((context.replacingClickedOnBlock()
                        && context.getClickedFace() == type.direction()
                        && getDirectionForPlacement(context) == type.direction())
                        || (!context.replacingClickedOnBlock()
                        && context.getClickedFace() != type.direction()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        VerticalSlabType type = state.getValue(TYPE);
        return type == VerticalSlabType.DOUBLE
                ? state
                : state.setValue(TYPE, VerticalSlabType.fromDirection(rotation.rotate(type.direction())));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        VerticalSlabType type = state.getValue(TYPE);
        if (type == VerticalSlabType.DOUBLE || mirror == Mirror.NONE) {
            return state;
        }

        Direction direction = type.direction();
        if ((mirror == Mirror.LEFT_RIGHT && direction.getAxis() == Direction.Axis.Z)
                || (mirror == Mirror.FRONT_BACK && direction.getAxis() == Direction.Axis.X)) {
            return state.setValue(TYPE, VerticalSlabType.fromDirection(direction.getOpposite()));
        }
        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return state.getValue(TYPE) != VerticalSlabType.DOUBLE
                && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public boolean canPlaceLiquid(
            @Nullable LivingEntity user,
            BlockGetter level,
            BlockPos pos,
            BlockState state,
            Fluid fluid
    ) {
        return state.getValue(TYPE) != VerticalSlabType.DOUBLE
                && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, fluid);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.WATER && state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    public enum VerticalSlabType implements StringRepresentable {
        NORTH(Direction.NORTH, Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0)),
        SOUTH(Direction.SOUTH, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0)),
        WEST(Direction.WEST, Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0)),
        EAST(Direction.EAST, Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0)),
        DOUBLE(null, Shapes.block());

        private final @Nullable Direction direction;
        private final VoxelShape shape;

        VerticalSlabType(@Nullable Direction direction, VoxelShape shape) {
            this.direction = direction;
            this.shape = shape;
        }

        public Direction direction() {
            if (direction == null) {
                throw new IllegalStateException("A double vertical slab has no direction");
            }
            return direction;
        }

        public VoxelShape shape() {
            return shape;
        }

        @Override
        public String getSerializedName() {
            return direction == null ? "double" : direction.getSerializedName();
        }

        public static VerticalSlabType fromDirection(Direction direction) {
            return switch (direction) {
                case NORTH -> NORTH;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case EAST -> EAST;
                default -> throw new IllegalArgumentException("Vertical slab direction must be horizontal: " + direction);
            };
        }
    }
}
