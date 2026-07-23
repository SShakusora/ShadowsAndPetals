package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WoodPostBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<WoodPostBlock> CODEC = simpleCodec(WoodPostBlock::new);

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    private static final double INNER_MIN = 6.0D;
    private static final double INNER_MAX = 10.0D;
    private static final VoxelShape CORE_SHAPE = Block.box(INNER_MIN, INNER_MIN, INNER_MIN, INNER_MAX, INNER_MAX, INNER_MAX);
    private static final VoxelShape HANGING_SUPPORT_SHAPE = Block.column(2.0D, 0.0D, 1.0D);
    private static final VoxelShape[] ARM_SHAPES = new VoxelShape[]{
            Block.box(INNER_MIN, 0.0D, INNER_MIN, INNER_MAX, INNER_MAX, INNER_MAX),
            Block.box(INNER_MIN, INNER_MIN, INNER_MIN, INNER_MAX, 16.0D, INNER_MAX),
            Block.box(INNER_MIN, INNER_MIN, 0.0D, INNER_MAX, INNER_MAX, INNER_MAX),
            Block.box(INNER_MIN, INNER_MIN, INNER_MIN, INNER_MAX, INNER_MAX, 16.0D),
            Block.box(0.0D, INNER_MIN, INNER_MIN, INNER_MAX, INNER_MAX, INNER_MAX),
            Block.box(INNER_MIN, INNER_MIN, INNER_MIN, 16.0D, INNER_MAX, INNER_MAX)
    };
    private static final VoxelShape[] SHAPES = new VoxelShape[Direction.Axis.values().length * 64];

    public WoodPostBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(WATERLOGGED, false)
                .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    public MapCodec<WoodPostBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, AXIS);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockPos pos = context.getClickedPos();
        LevelReader level = context.getLevel();
        return defaultBlockState()
                .setValue(AXIS, axis)
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
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

        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return cachedShape(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return cachedShape(state, level, pos);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.or(super.getBlockSupportShape(state, level, pos), HANGING_SUPPORT_SHAPE);
    }

    private static VoxelShape cachedShape(BlockState state, BlockGetter level, BlockPos pos) {
        int index = state.getValue(AXIS).ordinal() * 64 + connectionMask(connections(level, pos, state), state.getValue(AXIS));
        VoxelShape shape = SHAPES[index];
        if (shape != null) {
            return shape;
        }

        VoxelShape built = CORE_SHAPE;
        for (Direction direction : ALL_DIRECTIONS) {
            if (hasSolidArm(state.getValue(AXIS), connections(level, pos, state), direction)) {
                built = Shapes.or(built, ARM_SHAPES[direction.ordinal()]);
            }
        }

        SHAPES[index] = built;
        return built;
    }

    private static int connectionMask(Connections connections, Direction.Axis axis) {
        int mask = 0;
        for (Direction direction : ALL_DIRECTIONS) {
            if (direction.getAxis() == axis || connections.get(direction).isSolid()) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    private static boolean hasSolidArm(Direction.Axis axis, Connections connections, Direction direction) {
        return axis == direction.getAxis() || connections.get(direction).isSolid();
    }

    public static Connections connections(BlockGetter level, BlockPos pos, BlockState state) {
        return new Connections(
                resolveConnection(level, pos, Direction.DOWN),
                resolveConnection(level, pos, Direction.UP),
                resolveConnection(level, pos, Direction.NORTH),
                resolveConnection(level, pos, Direction.SOUTH),
                resolveConnection(level, pos, Direction.WEST),
                resolveConnection(level, pos, Direction.EAST)
        );
    }

    public static ConnectionType resolveConnection(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));

        if (isAlignedChain(neighbor, direction)) {
            return ConnectionType.fromChainBlock(neighbor.getBlock());
        }

        if (direction == Direction.DOWN && isHangingConnection(neighbor)) {
            return ConnectionType.fromHangingBlock(neighbor.getBlock());
        }

        if (neighbor.getBlock() instanceof WoodPostBlock && neighbor.getValue(AXIS) == direction.getAxis()) {
            return ConnectionType.OTHER_POST;
        }

        return ConnectionType.NONE;
    }

    private static boolean isAlignedChain(BlockState state, Direction direction) {
        return state.is(BlockTags.CHAINS)
                && state.hasProperty(BlockStateProperties.AXIS)
                && state.getValue(BlockStateProperties.AXIS) == direction.getAxis();
    }

    private static boolean isHangingConnection(BlockState state) {
        return state.is(BlockTagRegistry.WOOD_POST_HANGING_CONNECTIONS)
                && (!state.hasProperty(BlockStateProperties.HANGING)
                || state.getValue(BlockStateProperties.HANGING));
    }

    public record Connections(
            ConnectionType down,
            ConnectionType up,
            ConnectionType north,
            ConnectionType south,
            ConnectionType west,
            ConnectionType east
    ) {
        public ConnectionType get(Direction direction) {
            return switch (direction) {
                case DOWN -> down;
                case UP -> up;
                case NORTH -> north;
                case SOUTH -> south;
                case WEST -> west;
                case EAST -> east;
            };
        }
    }

    public enum ConnectionType implements StringRepresentable {
        NONE("none"),
        IRON_CHAIN("iron_chain", Identifier.withDefaultNamespace("block/iron_chain")),
        COPPER_CHAIN("copper_chain", Identifier.withDefaultNamespace("block/copper_chain")),
        EXPOSED_COPPER_CHAIN("exposed_copper_chain", Identifier.withDefaultNamespace("block/exposed_copper_chain")),
        WEATHERED_COPPER_CHAIN("weathered_copper_chain", Identifier.withDefaultNamespace("block/weathered_copper_chain")),
        OXIDIZED_COPPER_CHAIN("oxidized_copper_chain", Identifier.withDefaultNamespace("block/oxidized_copper_chain")),
        WAXED_COPPER_CHAIN("waxed_copper_chain", Identifier.withDefaultNamespace("block/copper_chain")),
        WAXED_EXPOSED_COPPER_CHAIN("waxed_exposed_copper_chain", Identifier.withDefaultNamespace("block/exposed_copper_chain")),
        WAXED_WEATHERED_COPPER_CHAIN("waxed_weathered_copper_chain", Identifier.withDefaultNamespace("block/weathered_copper_chain")),
        WAXED_OXIDIZED_COPPER_CHAIN("waxed_oxidized_copper_chain", Identifier.withDefaultNamespace("block/oxidized_copper_chain")),
        OTHER_POST("other_post");

        private static final Map<String, ConnectionType> BY_BLOCK_PATH = Arrays.stream(values())
                .filter(ConnectionType::isChain)
                .collect(Collectors.toUnmodifiableMap(ConnectionType::getSerializedName, Function.identity()));

        private final String serializedName;
        private final @Nullable Identifier texture;

        ConnectionType(String serializedName) {
            this(serializedName, null);
        }

        ConnectionType(String serializedName, @Nullable Identifier texture) {
            this.serializedName = serializedName;
            this.texture = texture;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public boolean isSolid() {
            return this != NONE;
        }

        public boolean isChain() {
            return texture != null;
        }

        public @Nullable Identifier texture() {
            return texture;
        }

        public static ConnectionType fromChainBlock(Block block) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            return BY_BLOCK_PATH.getOrDefault(path, IRON_CHAIN);
        }

        public static ConnectionType fromHangingBlock(Block block) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            for (Map.Entry<String, ConnectionType> entry : BY_BLOCK_PATH.entrySet()) {
                String chainPath = entry.getKey();
                String materialPrefix = chainPath.substring(0, chainPath.length() - "_chain".length());
                if (path.startsWith(materialPrefix + "_")) {
                    return entry.getValue();
                }
            }
            return IRON_CHAIN;
        }

        @Override
        public String toString() {
            return serializedName;
        }
    }
}
