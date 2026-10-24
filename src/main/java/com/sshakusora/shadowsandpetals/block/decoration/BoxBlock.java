package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.BoxBlockEntity;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A half-block-tall open box that stores its content in a block entity. Two boxes can be
 * stacked into a full block, keeping one content stack per vertical half.
 */
public class BoxBlock extends BaseEntityBlock {
    public static final MapCodec<BoxBlock> CODEC = simpleCodec(BoxBlock::new);
    public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape BOTTOM_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final VoxelShape TOP_SHAPE = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    // A solid collision shape makes vanilla sample inner faces from the neighboring block.
    private static final VoxelShape DOUBLE_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D),
            Block.box(0.0D, 8.0D, 0.0D, 16.0D, 9.0D, 16.0D),
            Block.box(0.0D, 1.0D, 0.0D, 1.0D, 16.0D, 16.0D),
            Block.box(15.0D, 1.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(1.0D, 1.0D, 0.0D, 15.0D, 16.0D, 1.0D),
            Block.box(1.0D, 1.0D, 15.0D, 15.0D, 16.0D, 16.0D)
    );

    public BoxBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(TYPE, SlabType.BOTTOM));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(TYPE));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(TYPE));
    }

    private static VoxelShape shapeFor(SlabType type) {
        return switch (type) {
            case DOUBLE -> DOUBLE_SHAPE;
            case TOP -> TOP_SHAPE;
            default -> BOTTOM_SHAPE;
        };
    }

    /**
     * Rebuilds the picked stack from the block entity so middle-click pick block and Jade show
     * the stored content instead of a bare box. A double box yields a regular single box item
     * carrying the topmost content, never the stacked double item; {@link BlockEntity#saveToItem}
     * keeps the half-box item data identical to the loot drop path.
     */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(asItem());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BoxBlockEntity boxEntity) {
            if (state.getValue(TYPE) == SlabType.DOUBLE) {
                return topContentItem(boxEntity);
            }
            boxEntity.saveToItem(stack, level.registryAccess());
        }
        return stack;
    }

    /**
     * Builds a regular single box stack carrying the upper content of a double box, so the
     * picked item renders like and stacks with other boxes holding the same content.
     */
    private static ItemStack topContentItem(BoxBlockEntity boxEntity) {
        ItemStack stack = new ItemStack(boxEntity.getBlockState().getBlock().asItem());
        ItemStack topContent = boxEntity.topContentStack();
        if (!topContent.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putString(BoxContents.CONTENT_TAG, BuiltInRegistries.ITEM.getKey(topContent.getItem()).toString());
            BlockItem.setBlockEntityData(stack, boxEntity.getType(), tag);
        }
        return stack;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState currentState = context.getLevel().getBlockState(context.getClickedPos());
        boolean doubleItem = BoxContents.isDoubleItem(context.getItemInHand());
        if (currentState.is(this)) {
            if (doubleItem || currentState.getValue(TYPE) == SlabType.DOUBLE) {
                return null;
            }

            return currentState.setValue(TYPE, SlabType.DOUBLE);
        }

        BlockState state = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (doubleItem) {
            return state.setValue(TYPE, SlabType.DOUBLE);
        }

        Direction face = context.getClickedFace();
        boolean top = face == Direction.DOWN
                || (face != Direction.UP && context.getClickLocation().y - context.getClickedPos().getY() > 0.5D);
        return state.setValue(TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.getItemInHand().is(asItem()) || BoxContents.isDoubleItem(context.getItemInHand())) {
            return false;
        }

        SlabType type = state.getValue(TYPE);
        if (type == SlabType.DOUBLE) {
            return false;
        }

        if (!context.replacingClickedOnBlock()) {
            return true;
        }

        boolean upper = context.getClickLocation().y - context.getClickedPos().getY() > 0.5D;
        Direction face = context.getClickedFace();
        return type == SlabType.BOTTOM
                ? face == Direction.UP || upper && face.getAxis().isHorizontal()
                : face == Direction.DOWN || !upper && face.getAxis().isHorizontal();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntity.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }
}
