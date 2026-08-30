package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BedroomLampBlock extends Block {
    public static final MapCodec<BedroomLampBlock> CODEC = simpleCodec(BedroomLampBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE = Shapes.or(
            box(3.5, 0, 3.5, 12.5, 1, 12.5),
            box(6, 1, 6, 10, 3, 10),
            box(7, 3, 7, 9, 7, 9),
            box(6.5, 7, 6.5, 9.5, 11, 9.5),
            box(7, 11.01, 7, 9, 12.01, 9),
            box(5, 5, 13.5, 11, 13, 14.5),
            box(5, 5, 1.5, 11, 13, 2.5),
            box(1.49999592, 5, 10.29289270, 5.70728127, 13, 14.50017805),
            box(1.49982270, 5, 1.49999873, 5.70710805, 13, 5.70728408),
            box(10.29271270, 5, 10.29288873, 14.49999805, 13, 14.50017408),
            box(10.29271592, 5, 1.49982270, 14.50000127, 13, 5.70710805),
            box(1.5, 5, 5, 2.5, 13, 11),
            box(13.5, 5, 5, 14.5, 13, 11),
            box(7.5, 6, 2.5, 8.5, 7, 13.5),
            box(2.5, 11, 7.5, 13.5, 12, 8.5),
            box(7.5, 11, 2.5, 8.5, 12, 13.5),
            box(2.5, 6, 7.5, 13.5, 7, 8.5)
    ).optimize();

    public BedroomLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected MapCodec<BedroomLampBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return LampToggle.toggle(state, level, pos, player, LIT);
    }
}
