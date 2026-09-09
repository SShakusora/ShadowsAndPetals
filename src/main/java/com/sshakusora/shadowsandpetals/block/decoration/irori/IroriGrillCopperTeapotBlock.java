package com.sshakusora.shadowsandpetals.block.decoration.irori;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.block.decoration.CopperTeapotBlock;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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
                .setValue(ON_IRORI, true)
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
                super.getShape(state, level, pos, context),
                IroriGrillVoxelShapes.upper(state.getValue(IroriGrillBlock.GRILL_PART))
        );
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
        CollisionContext context
    ) {
        return Shapes.or(
                super.getShape(state, level, pos, context),
                IroriGrillVoxelShapes.upper(state.getValue(IroriGrillBlock.GRILL_PART))
        );
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            IroriBlockEntity.removeInstalledGrill(
                    level,
                    pos.below(),
                    pos,
                    !player.preventsBlockDrops()
            );
        }
        return super.playerWillDestroy(level, pos, state, player);
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
