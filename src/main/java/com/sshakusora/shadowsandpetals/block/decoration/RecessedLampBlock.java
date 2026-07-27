package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RecessedLampBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<RecessedLampBlock> CODEC = simpleCodec(RecessedLampBlock::new);
    public static final EnumProperty<Mount> MOUNT = EnumProperty.create("mount", Mount.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape FLOOR_SHAPE = box(1.0, -3.0, 1.0, 15.0, 1.0, 15.0);
    private static final VoxelShape FLOOR_SLAB_SHAPE = box(1.0, -11.0, 1.0, 15.0, -7.0, 15.0);
    private static final VoxelShape CEILING_SHAPE = box(1.0, 15.0, 1.0, 15.0, 19.0, 15.0);
    private static final VoxelShape CEILING_SLAB_SHAPE = box(1.0, 23.0, 1.0, 15.0, 27.0, 15.0);

    public RecessedLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(MOUNT, Mount.FLOOR)
                .setValue(LIT, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<RecessedLampBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOUNT, LIT, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() != Direction.Axis.Y) {
            return null;
        }

        BlockPos pos = context.getClickedPos();
        BlockPos supportPos = pos.relative(clickedFace.getOpposite());
        Mount mount = Mount.forPlacement(clickedFace, context.getLevel().getBlockState(supportPos));
        BlockState state = defaultBlockState()
                .setValue(MOUNT, mount)
                .setValue(WATERLOGGED, context.getLevel().getFluidState(pos).is(Fluids.WATER));
        return state;
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
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(MOUNT)) {
            case FLOOR -> FLOOR_SHAPE;
            case FLOOR_SLAB -> FLOOR_SLAB_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case CEILING_SLAB -> CEILING_SLAB_SHAPE;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean lit = !state.getValue(LIT);
        level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, lit ? 0.6F : 0.5F);
        return InteractionResult.SUCCESS;
    }

    private static boolean isSlabType(BlockState state, SlabType type) {
        return state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == type;
    }

    public enum Mount implements StringRepresentable {
        FLOOR("floor", Direction.UP, null),
        FLOOR_SLAB("floor_slab", Direction.UP, SlabType.BOTTOM),
        CEILING("ceiling", Direction.DOWN, null),
        CEILING_SLAB("ceiling_slab", Direction.DOWN, SlabType.TOP);

        private final String name;
        private final Direction face;
        private final @Nullable SlabType slabType;

        Mount(String name, Direction face, @Nullable SlabType slabType) {
            this.name = name;
            this.face = face;
            this.slabType = slabType;
        }

        public Direction face() {
            return face;
        }

        public @Nullable SlabType slabType() {
            return slabType;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        private static Mount forPlacement(Direction face, BlockState supportState) {
            if (face == Direction.UP) {
                return isSlabType(supportState, SlabType.BOTTOM) ? FLOOR_SLAB : FLOOR;
            }
            return isSlabType(supportState, SlabType.TOP) ? CEILING_SLAB : CEILING;
        }
    }
}
