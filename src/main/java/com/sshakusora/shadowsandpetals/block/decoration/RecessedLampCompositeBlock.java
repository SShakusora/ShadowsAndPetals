package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.blockentity.RecessedLampBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RecessedLampCompositeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<RecessedLampCompositeBlock> CODEC =
            simpleCodec(RecessedLampCompositeBlock::new);

    private static final VoxelShape BOTTOM_SLAB_SHAPE = box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final VoxelShape TOP_SLAB_SHAPE = box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape BOTTOM_LAMP_SHAPE = box(1.0, 5.0, 1.0, 15.0, 9.0, 15.0);
    private static final VoxelShape TOP_LAMP_SHAPE = box(1.0, 7.0, 1.0, 15.0, 11.0, 15.0);

    public RecessedLampCompositeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(RecessedLampBlock.MOUNT, RecessedLampBlock.Mount.FLOOR_SLAB)
                .setValue(RecessedLampBlock.LIT, false)
                .setValue(RecessedLampBlock.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<RecessedLampCompositeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RecessedLampBlock.MOUNT, RecessedLampBlock.LIT, RecessedLampBlock.WATERLOGGED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecessedLampBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        VoxelShape slabShape = storedSlab != null
                ? storedSlab.getShape(level, pos, context)
                : fallbackSlabShape(state);
        return Shapes.or(slabShape, lampShape(state));
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getCollisionShape(level, pos, context)
                : fallbackSlabShape(state);
    }

    @Override
    protected VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getVisualShape(level, pos, context)
                : fallbackSlabShape(state);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getBlockSupportShape(level, pos)
                : fallbackSlabShape(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return fallbackSlabShape(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getDestroyProgress(player, level, pos)
                : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(BlockRegistry.RECESSED_LAMP.get()));

        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof RecessedLampBlockEntity lampBlockEntity)) {
            return drops;
        }

        BlockState storedSlab = lampBlockEntity.getEffectiveStoredSlab();
        if (storedSlab == null) {
            return drops;
        }

        Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (breaker instanceof Player player
                && !storedSlab.canHarvestBlock(params.getLevel(), lampBlockEntity.getBlockPos(), player)) {
            return drops;
        }

        LootParams.Builder slabParams = new LootParams.Builder(params.getLevel())
                .withParameter(LootContextParams.ORIGIN, params.getParameter(LootContextParams.ORIGIN))
                .withParameter(LootContextParams.TOOL, params.getParameter(LootContextParams.TOOL))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, breaker);
        Float explosionRadius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (explosionRadius != null) {
            slabParams.withParameter(LootContextParams.EXPLOSION_RADIUS, explosionRadius);
        }

        drops.addAll(storedSlab.getDrops(slabParams));
        return drops;
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

        boolean lit = !state.getValue(RecessedLampBlock.LIT);
        level.setBlock(pos, state.setValue(RecessedLampBlock.LIT, lit), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, lit ? 0.6F : 0.5F);
        return InteractionResult.SUCCESS;
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
        if (state.getValue(RecessedLampBlock.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(RecessedLampBlock.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(BlockRegistry.RECESSED_LAMP.get());
    }

    @Override
    public float getFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getFriction(level, pos, entity)
                : super.getFriction();
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getSoundType(level, pos, entity)
                : SoundType.METAL;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getExplosionResistance(level, pos, explosion)
                : super.getExplosionResistance();
    }

    @Override
    public MapColor getMapColor(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            MapColor defaultColor
    ) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null
                ? storedSlab.getMapColor(level, pos)
                : defaultColor;
    }

    @Override
    public boolean ignitedByLava(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null && storedSlab.ignitedByLava(level, pos, direction);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null ? storedSlab.getFlammability(level, pos, direction) : 0;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockState storedSlab = getEffectiveStoredSlab(level, pos, state);
        return storedSlab != null && storedSlab.isFlammable(level, pos, direction);
    }

    @Override
    public BlockState getAppearance(
            BlockState state,
            BlockAndLightGetter level,
            BlockPos pos,
            Direction side,
            @Nullable BlockState queryState,
            @Nullable BlockPos queryPos
    ) {
        ModelData modelData = level.getModelData(pos);
        BlockState storedSlab = modelData.get(RecessedLampBlockEntity.STORED_SLAB_MODEL_PROPERTY);
        return storedSlab != null ? storedSlab : state;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    public static @Nullable BlockState getEffectiveStoredSlab(
            BlockGetter level,
            BlockPos pos,
            BlockState hostState
    ) {
        if (level.getBlockEntity(pos) instanceof RecessedLampBlockEntity blockEntity) {
            return RecessedLampBlockEntity.applyHostWaterlogged(blockEntity.getStoredSlab(), hostState);
        }
        return null;
    }

    private static VoxelShape fallbackSlabShape(BlockState state) {
        return slabType(state) == SlabType.TOP ? TOP_SLAB_SHAPE : BOTTOM_SLAB_SHAPE;
    }

    private static VoxelShape lampShape(BlockState state) {
        return slabType(state) == SlabType.TOP ? TOP_LAMP_SHAPE : BOTTOM_LAMP_SHAPE;
    }

    private static SlabType slabType(BlockState state) {
        return state.getValue(RecessedLampBlock.MOUNT) == RecessedLampBlock.Mount.CEILING_SLAB
                ? SlabType.TOP
                : SlabType.BOTTOM;
    }
}
