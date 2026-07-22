package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.api.irori.IroriApi;
import com.sshakusora.shadowsandpetals.api.irori.IroriIgnitionBehavior;
import com.sshakusora.shadowsandpetals.api.irori.IroriIgnitionContext;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriComponentTopology;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class IroriBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<IroriBlock> CODEC = simpleCodec(IroriBlock::new);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double STANDALONE_BASIN_MIN = 3.0D / 16.0D;
    private static final double STANDALONE_BASIN_MAX = 13.0D / 16.0D;
    private static final double CONNECTED_BASIN_INSET = 4.0D / 16.0D;
    private static final double BASIN_FLOOR_Y = 10.0D / 16.0D;
    private static final double ITEM_BASIN_VERTICAL_EPSILON = 1.0D / 16.0D;

    private static final VoxelShape BASE_SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);
    private static final VoxelShape STANDALONE_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 1.0D),
            box(15.0D, 10.0D, 1.0D, 16.0D, 15.0D, 15.0D),
            box(0.0D, 10.0D, 15.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 10.0D, 1.0D, 1.0D, 15.0D, 15.0D),
            box(0.0D, 15.0D, 0.0D, 13.0D, 16.0D, 3.0D),
            box(13.0D, 15.0D, 0.0D, 16.0D, 16.0D, 13.0D),
            box(3.0D, 15.0D, 13.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 15.0D, 3.0D, 3.0D, 16.0D, 16.0D)
    ).optimize();
    private static final VoxelShape SINGLE_EDGE_NORTH_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 1.0D),
            box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 4.0D)
    );
    private static final VoxelShape DOUBLE_EDGE_NORTH_SOUTH_SHAPE = Shapes.or(
            SINGLE_EDGE_NORTH_SHAPE,
            box(0.0D, 10.0D, 15.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 15.0D, 12.0D, 16.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape CORNER_NORTH_EAST_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 15.0D, 15.0D, 1.0D),
            box(15.0D, 10.0D, 0.0D, 16.0D, 15.0D, 1.0D),
            box(15.0D, 10.0D, 1.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 15.0D, 0.0D, 12.0D, 16.0D, 4.0D),
            box(12.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape END_NORTH_EAST_WEST_SHAPE = Shapes.or(
            BASE_SHAPE,
            box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 2.0D),
            box(15.0D, 10.0D, 1.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 10.0D, 1.0D, 1.0D, 15.0D, 16.0D),
            box(0.0D, 15.0D, 0.0D, 12.0D, 16.0D, 4.0D),
            box(12.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 15.0D, 4.0D, 4.0D, 16.0D, 16.0D)
    );

    private static final Map<Direction, VoxelShape> SINGLE_EDGE_SHAPES = VoxelShapeUtils.rotateHorizontal(SINGLE_EDGE_NORTH_SHAPE);
    private static final Map<Direction, VoxelShape> DOUBLE_EDGE_SHAPES = VoxelShapeUtils.rotateHorizontal(DOUBLE_EDGE_NORTH_SOUTH_SHAPE);
    private static final Map<Direction, VoxelShape> CORNER_SHAPES = VoxelShapeUtils.rotateHorizontal(CORNER_NORTH_EAST_SHAPE);
    private static final Map<Direction, VoxelShape> END_SHAPES = createEndShapes();
    private static final VoxelShape[] SHAPES_BY_CONNECTIONS = createShapes();

    public IroriBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        return updateConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED);
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

        if (direction.getAxis().isHorizontal()) {
            return updateConnections(state, level, pos);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(WATERLOGGED)) {
            return 0;
        }

        AuxiliaryLightManager lightManager = level.getAuxLightManager(pos);
        return lightManager != null ? lightManager.getLightAt(pos) : 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES_BY_CONNECTIONS[getConnectionMask(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected MapCodec<IroriBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.IRORI.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.IRORI.get(), IroriBlockEntity::tick);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IroriBlockEntity irori) {
            irori.dropContentsOnRemoval(pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise
    ) {
        if (level.isClientSide()
                || state.getValue(WATERLOGGED)
                || !(entity instanceof ItemEntity itemEntity)
                || !isItemInBasin(state, pos, itemEntity)) {
            return;
        }

        ItemStack droppedStack = itemEntity.getItem();
        if (!isFuel(droppedStack, level)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IroriBlockEntity irori)) {
            return;
        }
        IroriBlockEntity master = irori.resolveMaster();
        if (!isValidFuelAcceptor(pos, master, level)) {
            return;
        }

        if (tryAddDroppedFuel(droppedStack, master, level, pos)) {
            if (droppedStack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(droppedStack);
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (state.getValue(WATERLOGGED) || !isBasinHit(state, pos, hitResult)) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IroriBlockEntity irori)) {
            return InteractionResult.PASS;
        }

        IroriBlockEntity master = irori.resolveMaster();
        boolean hasAsh = master.hasAsh();
        IroriIgnitionBehavior ignitionBehavior = IroriApi.findIgnitionBehavior(stack).orElse(null);
        if (!hasAsh && ignitionBehavior == null) {
            return InteractionResult.PASS;
        }
        if (!isValidFuelAcceptor(pos, master, level)) {
            return InteractionResult.PASS;
        }

        if (hasAsh) {
            if (!level.isClientSide()) {
                master.clearAshAndDropResults();
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        if (!master.canIgnite()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!master.tryIgnite(level, level.getRandom())) {
            return InteractionResult.PASS;
        }

        ignitionBehavior.onIgnited(new IroriIgnitionContext(
                level,
                pos,
                master.getBlockPos(),
                player,
                hand,
                stack
        ));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSecondaryUseActive()) {
            return openMasterMenu(level, pos, player);
        }
        if (state.getValue(WATERLOGGED) || !isBasinHit(state, pos, hitResult)) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IroriBlockEntity irori)) {
            return InteractionResult.PASS;
        }

        IroriBlockEntity master = irori.resolveMaster();
        if (!master.hasAsh()) {
            return InteractionResult.PASS;
        }
        if (!isValidFuelAcceptor(pos, master, level)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            master.clearAshAndDropResults();
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult openMasterMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof IroriBlockEntity irori) {
            IroriBlockEntity master = irori.resolveMaster();
            player.openMenu(master, master.getBlockPos());
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    private static boolean canAcceptFuel(ItemStack currentFuel, ItemStack heldStack) {
        return currentFuel.isEmpty()
                || ItemStack.isSameItemSameComponents(currentFuel, heldStack) && currentFuel.getCount() < currentFuel.getMaxStackSize();
    }

    private static boolean tryAddDroppedFuel(ItemStack droppedStack, IroriBlockEntity master, Level level, BlockPos soundPos) {
        if (!isFuel(droppedStack, level)) {
            return false;
        }

        ItemStack currentFuel = master.getFuelStack();
        if (!canAcceptFuel(currentFuel, droppedStack)) {
            return false;
        }

        int currentCount = currentFuel.isEmpty() ? 0 : currentFuel.getCount();
        int maxCount = currentFuel.isEmpty() ? droppedStack.getMaxStackSize() : currentFuel.getMaxStackSize();
        int insertCount = Math.min(droppedStack.getCount(), maxCount - currentCount);
        if (insertCount <= 0) {
            return false;
        }

        ItemStack updatedFuel = currentFuel.isEmpty() ? droppedStack.copyWithCount(insertCount) : currentFuel.copy();
        if (!currentFuel.isEmpty()) {
            updatedFuel.grow(insertCount);
        }

        master.clearAshAndDropResults();
        master.setFuelStack(updatedFuel, level.getRandom());
        level.playSound(null, soundPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.9F, 0.95F + level.getRandom().nextFloat() * 0.1F);
        droppedStack.shrink(insertCount);
        return true;
    }

    private static boolean isFuel(ItemStack stack, Level level) {
        return IroriApi.getFuelBurnTime(stack, level) > 0;
    }

    private static boolean isValidFuelAcceptor(BlockPos pos, IroriBlockEntity master, Level level) {
        return IroriComponentTopology.bounds(level, master.getBlockPos()).containsCenter(pos);
    }

    private static boolean isBasinHit(BlockState state, BlockPos pos, BlockHitResult hitResult) {
        if (hitResult.getDirection() != Direction.UP) {
            return false;
        }

        return isBasinPosition(state, pos, hitResult.getLocation());
    }

    private static boolean isItemInBasin(BlockState state, BlockPos pos, ItemEntity itemEntity) {
        double localY = itemEntity.getY() - pos.getY();
        return localY >= BASIN_FLOOR_Y - ITEM_BASIN_VERTICAL_EPSILON
                && localY <= 1.0D + itemEntity.getBbHeight()
                && isBasinPosition(state, pos, itemEntity.position());
    }

    private static boolean isBasinPosition(BlockState state, BlockPos pos, Vec3 location) {
        double localX = location.x - pos.getX();
        double localZ = location.z - pos.getZ();

        double minX = getBasinMin(state.getValue(WEST), state.getValue(EAST), state.getValue(NORTH), state.getValue(SOUTH));
        double maxX = getBasinMax(state.getValue(WEST), state.getValue(EAST), state.getValue(NORTH), state.getValue(SOUTH));
        double minZ = getBasinMin(state.getValue(NORTH), state.getValue(SOUTH), state.getValue(WEST), state.getValue(EAST));
        double maxZ = getBasinMax(state.getValue(NORTH), state.getValue(SOUTH), state.getValue(WEST), state.getValue(EAST));
        return localX >= minX && localX <= maxX && localZ >= minZ && localZ <= maxZ;
    }

    private static double getBasinMin(boolean negativeConnected, boolean positiveConnected, boolean sideAConnected, boolean sideBConnected) {
        if (!negativeConnected && !positiveConnected && !sideAConnected && !sideBConnected) {
            return STANDALONE_BASIN_MIN;
        }
        return negativeConnected ? 0.0D : CONNECTED_BASIN_INSET;
    }

    private static double getBasinMax(boolean negativeConnected, boolean positiveConnected, boolean sideAConnected, boolean sideBConnected) {
        if (!negativeConnected && !positiveConnected && !sideAConnected && !sideBConnected) {
            return STANDALONE_BASIN_MAX;
        }
        return positiveConnected ? 1.0D : 1.0D - CONNECTED_BASIN_INSET;
    }

    private BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        IroriComponentTopology.ConnectionSelection selection =
                IroriComponentTopology.selectConnections(level, pos, state);
        BlockState newState = selection.applyTo(state);
        if (level instanceof Level blockLevel && newState != state) {
            BlockEntity blockEntity = blockLevel.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori) {
                irori.dropContentsAndReset();
            }
            IroriBlockEntity.reelectMaster(blockLevel, selection.positions());
        }

        return newState;
    }

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < shapes.length; mask++) {
            if (mask == 0) {
                shapes[mask] = STANDALONE_SHAPE;
                continue;
            }

            boolean edgeNorth = (mask & 1) == 0;
            boolean edgeEast = (mask & 2) == 0;
            boolean edgeSouth = (mask & 4) == 0;
            boolean edgeWest = (mask & 8) == 0;
            shapes[mask] = getShapeForEdges(edgeNorth, edgeEast, edgeSouth, edgeWest).optimize();
        }
        return shapes;
    }

    private static VoxelShape getShapeForEdges(boolean north, boolean east, boolean south, boolean west) {
        int edgeCount = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);
        return switch (edgeCount) {
            case 0 -> BASE_SHAPE;
            case 1 -> SINGLE_EDGE_SHAPES.get(north ? Direction.NORTH : east ? Direction.EAST : south ? Direction.SOUTH : Direction.WEST);
            case 2 -> getDoubleEdgeShape(north, east, south, west);
            case 3 -> END_SHAPES.get(!north ? Direction.NORTH : !east ? Direction.EAST : !south ? Direction.SOUTH : Direction.WEST);
            case 4 -> STANDALONE_SHAPE;
            default -> throw new IllegalStateException("Unexpected edge count: " + edgeCount);
        };
    }

    private static VoxelShape getDoubleEdgeShape(boolean north, boolean east, boolean south, boolean west) {
        if (north && south) {
            return DOUBLE_EDGE_SHAPES.get(Direction.NORTH);
        }
        if (east && west) {
            return DOUBLE_EDGE_SHAPES.get(Direction.EAST);
        }
        if (north && east) {
            return CORNER_SHAPES.get(Direction.NORTH);
        }
        if (east && south) {
            return CORNER_SHAPES.get(Direction.EAST);
        }
        if (south && west) {
            return CORNER_SHAPES.get(Direction.SOUTH);
        }
        return CORNER_SHAPES.get(Direction.WEST);
    }

    private static Map<Direction, VoxelShape> createEndShapes() {
        Map<Direction, VoxelShape> rotated = VoxelShapeUtils.rotateHorizontal(END_NORTH_EAST_WEST_SHAPE);
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.SOUTH, rotated.get(Direction.NORTH));
        shapes.put(Direction.WEST, rotated.get(Direction.EAST));
        shapes.put(Direction.NORTH, rotated.get(Direction.SOUTH));
        shapes.put(Direction.EAST, rotated.get(Direction.WEST));
        return shapes;
    }

    private static int getConnectionMask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0)
                | (state.getValue(EAST) ? 2 : 0)
                | (state.getValue(SOUTH) ? 4 : 0)
                | (state.getValue(WEST) ? 8 : 0);
    }

}
