package com.sshakusora.shadowsandpetals.block.decoration.irori;

import com.sshakusora.shadowsandpetals.compat.InteractionResultCompat;
import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class IroriGrillBlock extends Block implements SimpleWaterloggedBlock, IroriGrillPartHolder {
    public static final MapCodec<IroriGrillBlock> CODEC = simpleCodec(IroriGrillBlock::new);
    public static final EnumProperty<IroriGrillPart> GRILL_PART =
            EnumProperty.create("grill_part", IroriGrillPart.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public IroriGrillBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(GRILL_PART, IroriGrillPart.SINGLE)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<IroriGrillBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRILL_PART, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // This block has no item.  All placements are performed transactionally
        // by IroriBlockEntity.installGrill().
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return IroriGrillVoxelShapes.upper(state.getValue(GRILL_PART));
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context
    ) {
        return IroriGrillVoxelShapes.upperSurface(state.getValue(GRILL_PART));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.DOWN && !isValidLower(level.getBlockState(pos.below()))) {
            return state.getValue(WATERLOGGED)
                    ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            IroriBlockEntity.removeInstalledGrill(
                    level,
                    pos.below(),
                    pos,
                    !player.isCreative()
            );
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        BlockPos lowerPos = pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!isValidLower(lowerState)) {
            return InteractionResultCompat.asItem(InteractionResult.PASS);
        }
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isSecondaryUseActive()) {
            return InteractionResultCompat.asItem(IroriBlock.openMasterMenu(level, lowerPos, player));
        }

        if (lowerState.getValue(IroriBlock.WATERLOGGED)) {
            return InteractionResultCompat.asItem(InteractionResult.PASS);
        }

        InteractionResult baseResult = IroriBlock.interactWithBaseItem(
                stack,
                level,
                lowerPos,
                player,
                hand
        );
        if (baseResult != InteractionResult.PASS) {
            return InteractionResultCompat.asItem(baseResult);
        }

        return InteractionResultCompat.asItem(tryPlaceCookingItem(stack, state, level, lowerPos, player, hitResult));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        BlockPos lowerPos = pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!isValidLower(lowerState)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            return IroriBlock.openMasterMenu(level, lowerPos, player);
        }

        if (lowerState.getValue(IroriBlock.WATERLOGGED)) {
            return InteractionResult.PASS;
        }

        if (hitResult.getDirection() == Direction.UP
                && level.getBlockEntity(lowerPos) instanceof IroriBlockEntity irori
                && irori.hasCookingItem(lowerPos)) {
            if (!level.isClientSide()) {
                irori.takeCookingItem(lowerPos, player);
            }
            return InteractionResult.SUCCESS;
        }

        return IroriBlock.interactWithBaseEmptyHand(level, lowerPos, player);
    }

    /** Places one item on the lower Irori cell represented by this upper grill block. */
    private static InteractionResult tryPlaceCookingItem(
            ItemStack stack,
            BlockState grillState,
            Level level,
            BlockPos cookingPos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (hitResult.getDirection() != Direction.UP || grillState.getValue(WATERLOGGED)) {
            return InteractionResult.PASS;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(stack);
        boolean builtInCookingInput = level.getRecipeManager()
                .getRecipeFor(RecipeType.CAMPFIRE_COOKING, recipeInput, level)
                .isPresent()
                || level.getRecipeManager()
                .getRecipeFor(RecipeType.SMOKING, recipeInput, level)
                .isPresent();

        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(cookingPos) instanceof IroriBlockEntity irori
                && irori.tryPlaceCookingItem(serverLevel, cookingPos, player, stack)) {
            return InteractionResult.SUCCESS;
        }

        return builtInCookingInput
                ? level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return BlockRegistry.IRORI.toStack();
    }

    public static boolean isValidLower(BlockState state) {
        return state.getBlock() instanceof IroriBlock && IroriBlock.hasGrill(state);
    }

    public static BlockPos masterPosition(BlockPos upperPos, BlockState upperState) {
        return IroriGrillPartHolder.masterPosition(upperPos, upperState);
    }
}
