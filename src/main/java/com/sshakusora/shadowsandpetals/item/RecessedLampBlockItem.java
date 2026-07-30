package com.sshakusora.shadowsandpetals.item;

import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampBlock;
import com.sshakusora.shadowsandpetals.blockentity.RecessedLampBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public final class RecessedLampBlockItem extends BlockItem {
    public RecessedLampBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        RecessedLampBlock.Mount mount = getSlabMount(context);
        if (mount != null) {
            if (!RecessedLampBlockEntity.isValidStoredSlab(
                    context.getLevel().getBlockState(context.getClickedPos()))) {
                return InteractionResult.FAIL;
            }
            return place(new CompositePlaceContext(context, mount));
        }
        return super.useOn(context);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        if (!(context instanceof CompositePlaceContext compositeContext)) {
            return super.getPlacementState(context);
        }

        BlockState replacedState = context.getLevel().getBlockState(context.getClickedPos());
        boolean waterlogged = replacedState.hasProperty(BlockStateProperties.WATERLOGGED)
                ? replacedState.getValue(BlockStateProperties.WATERLOGGED)
                : context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        BlockState placementState = BlockRegistry.RECESSED_LAMP_COMPOSITE.get()
                .defaultBlockState()
                .setValue(RecessedLampBlock.MOUNT, compositeContext.mount)
                .setValue(RecessedLampBlock.WATERLOGGED, waterlogged);
        return canPlace(context, placementState) ? placementState : null;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState placementState) {
        if (!(context instanceof CompositePlaceContext)) {
            return super.placeBlock(context, placementState);
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState replacedState = level.getBlockState(pos);
        if (!super.placeBlock(context, placementState)) {
            return false;
        }

        if (level.getBlockEntity(pos) instanceof RecessedLampBlockEntity blockEntity) {
            blockEntity.setStoredSlab(replacedState);
            return true;
        }

        level.setBlock(pos, replacedState, Block.UPDATE_ALL);
        return false;
    }

    @Override
    protected SoundEvent getPlaceSound(
            BlockState state,
            Level level,
            BlockPos pos,
            @Nullable Player player
    ) {
        return SoundType.METAL.getPlaceSound();
    }

    private static RecessedLampBlock.@Nullable Mount getSlabMount(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            return null;
        }

        SlabType slabType = state.getValue(BlockStateProperties.SLAB_TYPE);
        Direction clickedFace = context.getClickedFace();
        if (slabType == SlabType.BOTTOM && clickedFace == Direction.UP) {
            return RecessedLampBlock.Mount.FLOOR_SLAB;
        }
        if (slabType == SlabType.TOP && clickedFace == Direction.DOWN) {
            return RecessedLampBlock.Mount.CEILING_SLAB;
        }
        return null;
    }

    private static final class CompositePlaceContext extends BlockPlaceContext {
        private final BlockPos targetPos;
        private final RecessedLampBlock.Mount mount;

        private CompositePlaceContext(UseOnContext context, RecessedLampBlock.Mount mount) {
            super(context);
            this.targetPos = context.getClickedPos().immutable();
            this.mount = mount;
        }

        @Override
        public BlockPos getClickedPos() {
            return targetPos;
        }

        @Override
        public boolean canPlace() {
            return true;
        }

        @Override
        public boolean replacingClickedOnBlock() {
            return true;
        }
    }
}
