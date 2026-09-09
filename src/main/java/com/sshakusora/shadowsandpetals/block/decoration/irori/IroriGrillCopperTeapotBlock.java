package com.sshakusora.shadowsandpetals.block.decoration.irori;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import com.sshakusora.shadowsandpetals.blockentity.CopperTeapotBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class IroriGrillCopperTeapotBlock extends CopperTeapotBlock implements IroriGrillPartHolder {
    public static final MapCodec<IroriGrillCopperTeapotBlock> CODEC =
            simpleCodec(IroriGrillCopperTeapotBlock::new);

    public IroriGrillCopperTeapotBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(IroriGrillBlock.GRILL_PART, IroriGrillPart.SINGLE));
    }

    @Override
    protected MapCodec<IroriGrillCopperTeapotBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(IroriGrillBlock.GRILL_PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(
                getIroriShape(state.getValue(FACING)),
                IroriGrillVoxelShapes.upper(state.getValue(IroriGrillBlock.GRILL_PART))
        ).optimize();
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
        CollisionContext context
    ) {
        return Shapes.or(
                getIroriShape(state.getValue(FACING)),
                IroriGrillVoxelShapes.upperSurface(
                        state.getValue(IroriGrillBlock.GRILL_PART)
                )
        ).optimize();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            ItemStack toolStack,
            boolean willHarvest,
            FluidState fluid
    ) {
        // A composite teapot occupies the upper half of an installed grill.  Breaking it
        // should remove only the teapot while preserving the component-wide grill state.
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof CopperTeapotBlockEntity teapot) {
            Containers.dropContents(level, pos, teapot);
        }

        BlockState grillState = BlockRegistry.IRORI_GRILL.get()
                .defaultBlockState()
                .setValue(IroriGrillBlock.GRILL_PART, state.getValue(IroriGrillBlock.GRILL_PART))
                .setValue(IroriGrillBlock.WATERLOGGED, state.getValue(WATERLOGGED));
        return level.setBlock(pos, grillState, Block.UPDATE_ALL);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        BlockState replacement = level.getBlockState(pos);
        boolean preservesGrillPart = IroriGrillPartHolder.isGrillPart(replacement)
                && IroriGrillPartHolder.masterPosition(pos, state)
                .equals(IroriGrillPartHolder.masterPosition(pos, replacement));
        if (replacement.getBlock() != this
                && !preservesGrillPart
                && IroriGrillBlock.isValidLower(level.getBlockState(pos.below()))) {
            IroriBlockEntity.removeInstalledGrill(level, pos.below(), pos, true);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            boolean includeData,
            Player player
    ) {
        return new ItemStack(BlockRegistry.COPPER_TEAPOT.get());
    }
}
