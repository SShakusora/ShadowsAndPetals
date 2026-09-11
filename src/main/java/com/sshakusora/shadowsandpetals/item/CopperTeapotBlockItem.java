package com.sshakusora.shadowsandpetals.item;

import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillBlock;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPart;
import com.sshakusora.shadowsandpetals.block.decoration.irori.IroriGrillPartHolder;
import com.sshakusora.shadowsandpetals.blockentity.CopperTeapotBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

public final class CopperTeapotBlockItem extends BlockItem {
    public CopperTeapotBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        if (clickedState.getBlock() instanceof IroriGrillBlock
                && IroriGrillPartHolder.isGrillPart(clickedState)) {
            BlockPos lowerPos = clickedPos.below();
            if (!(context.getLevel().getBlockEntity(lowerPos) instanceof IroriBlockEntity irori)
                    || irori.hasCookingItem(lowerPos)
                    || !IroriGrillBlock.isValidLower(context.getLevel().getBlockState(lowerPos))) {
                return InteractionResult.FAIL;
            }

            IroriGrillPart part = IroriGrillPartHolder.getGrillPart(clickedState);
            if (!IroriGrillPartHolder.masterPosition(clickedPos, clickedState)
                    .equals(irori.resolveMaster().getBlockPos())) {
                return InteractionResult.FAIL;
            }
            return place(new CompositePlaceContext(context, part,
                    clickedState.getValue(IroriGrillBlock.WATERLOGGED)));
        }
        return super.useOn(context);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        if (!(context instanceof CompositePlaceContext compositeContext)) {
            return super.getPlacementState(context);
        }

        BlockState placementState = BlockRegistry.IRORI_GRILL_COPPER_TEAPOT.get()
                .defaultBlockState()
                .setValue(CopperTeapotBlock.FACING, context.getHorizontalDirection().getOpposite())
                .setValue(CopperTeapotBlock.WATERLOGGED, compositeContext.waterlogged)
                .setValue(IroriGrillBlock.GRILL_PART, compositeContext.part);
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

        if (level.getBlockEntity(pos) instanceof CopperTeapotBlockEntity) {
            return true;
        }

        level.setBlock(pos, replacedState, Block.UPDATE_ALL);
        return false;
    }

    @Override
    protected SoundEvent getPlaceSound(BlockState state, Level level, BlockPos pos, @Nullable Player player) {
        return SoundType.METAL.getPlaceSound();
    }

    private static final class CompositePlaceContext extends BlockPlaceContext {
        private final BlockPos targetPos;
        private final IroriGrillPart part;
        private final boolean waterlogged;

        private CompositePlaceContext(UseOnContext context, IroriGrillPart part, boolean waterlogged) {
            super(context);
            this.targetPos = context.getClickedPos().immutable();
            this.part = part;
            this.waterlogged = waterlogged;
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