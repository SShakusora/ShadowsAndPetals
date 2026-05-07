package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class IngotPileBlock extends SlabBlock {
    public static final MapCodec<IngotPileBlock> CODEC = simpleCodec(IngotPileBlock::new);
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape INGOT_PILE_BOTTOM_SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 0.5D, 7.75D, 4.0D, 15.5D),
            Block.box(8.25D, 0.0D, 0.5D, 15.0D, 4.0D, 15.5D),
            Block.box(0.5D, 4.0D, 8.25D, 15.5D, 8.0D, 15.0D),
            Block.box(0.5D, 4.0D, 1.0D, 15.5D, 8.0D, 7.75D)
    );

    private static final VoxelShape INGOT_PILE_DOUBLE_SHAPE = Shapes.or(
            INGOT_PILE_BOTTOM_SHAPE,
            Block.box(1.0D, 8.0D, 0.5D, 7.75D, 12.0D, 15.5D),
            Block.box(8.25D, 8.0D, 0.5D, 15.0D, 12.0D, 15.5D),
            Block.box(0.5D, 12.0D, 1.0D, 15.5D, 16.0D, 7.75D),
            Block.box(0.5D, 12.0D, 8.25D, 15.5D, 16.0D, 15.0D)
    );

    private static final Map<Direction.Axis, VoxelShape> BOTTOM_SHAPES = new EnumMap<>(Direction.Axis.class);
    private static final Map<Direction.Axis, VoxelShape> DOUBLE_SHAPES = new EnumMap<>(Direction.Axis.class);

    static {
        Map<Direction, VoxelShape> bottomRotated = VoxelShapeUtils.rotateHorizontal(INGOT_PILE_BOTTOM_SHAPE);
        Map<Direction, VoxelShape> doubleRotated = VoxelShapeUtils.rotateHorizontal(INGOT_PILE_DOUBLE_SHAPE);

        BOTTOM_SHAPES.put(Direction.Axis.X, bottomRotated.get(Direction.NORTH));
        BOTTOM_SHAPES.put(Direction.Axis.Z, bottomRotated.get(Direction.EAST));

        DOUBLE_SHAPES.put(Direction.Axis.X, doubleRotated.get(Direction.NORTH));
        DOUBLE_SHAPES.put(Direction.Axis.Z, doubleRotated.get(Direction.EAST));
    }

    public IngotPileBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(TYPE, SlabType.BOTTOM)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<IngotPileBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState currentState = context.getLevel().getBlockState(pos);
        if (currentState.is(this)
                && currentState.getValue(TYPE) == SlabType.BOTTOM
                && context.getItemInHand().is(asItem())
                && context.getClickedFace() == Direction.UP) {
            return currentState.setValue(TYPE, SlabType.DOUBLE);
        }

        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Direction.Axis axis = context.getClickedFace().getAxis().isHorizontal()
                ? context.getClickedFace().getAxis()
                : context.getHorizontalDirection().getAxis();
        return state.setValue(HORIZONTAL_AXIS, axis)
                .setValue(TYPE, state.getValue(TYPE) == SlabType.DOUBLE ? SlabType.DOUBLE : SlabType.BOTTOM);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_AXIS);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return state.getValue(TYPE) == SlabType.BOTTOM
                && context.getItemInHand().is(asItem())
                && context.replacingClickedOnBlock()
                && context.getClickedFace() == Direction.UP;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            return state.setValue(HORIZONTAL_AXIS, state.getValue(HORIZONTAL_AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.is(BlockRegistry.GOLD_INGOT_PILE.get()) || level.getBlockState(pos.above()).isSolidRender(level, pos.above()) || random.nextInt(12) != 0) {
            return;
        }

        int particleCount = 1 + random.nextInt(2);
        for (int i = 0; i < particleCount; i++) {
            double x = pos.getX() + 0.15D + random.nextDouble() * 0.7D;
            double y = pos.getY() + (state.getValue(TYPE) == SlabType.DOUBLE ? 0.96D : 0.46D) + random.nextDouble() * 0.08D;
            double z = pos.getZ() + 0.15D + random.nextDouble() * 0.7D;
            double speedX = (random.nextDouble() - 0.5D) * 0.01D;
            double speedY = 0.008D + random.nextDouble() * 0.012D;
            double speedZ = (random.nextDouble() - 0.5D) * 0.01D;

            if (random.nextBoolean()) {
                level.addParticle(ParticleTypes.END_ROD, x, y, z, speedX, speedY, speedZ);
            } else {
                level.addParticle(ParticleTypes.WAX_ON, x, y, z, speedX * 0.5D, speedY * 0.7D, speedZ * 0.5D);
            }
        }

        if (random.nextInt(20) == 0) {
            double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
            double y = pos.getY() + (state.getValue(TYPE) == SlabType.DOUBLE ? 1.02D : 0.52D);
            double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
            level.addParticle(ParticleTypes.FIREWORK, x, y, z, 0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(HORIZONTAL_AXIS);
        return state.getValue(TYPE) == SlabType.DOUBLE ? DOUBLE_SHAPES.get(axis) : BOTTOM_SHAPES.get(axis);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(HORIZONTAL_AXIS);
        return state.getValue(TYPE) == SlabType.DOUBLE ? DOUBLE_SHAPES.get(axis) : BOTTOM_SHAPES.get(axis);
    }
}
