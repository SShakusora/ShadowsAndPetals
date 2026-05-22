package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;

import java.util.List;

public final class WoodPostBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {
    private final WoodPostBlock block;

    public WoodPostBlockStateModel(WoodPostBlock block, BlockStateModel delegate) {
        super(delegate);
        this.block = block;
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return new GeometryKey(state.getBlock(), state.getValue(WoodPostBlock.AXIS), WoodPostBlock.connections(level, pos, state));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        delegate.collectParts(level, pos, state, random, parts);

        if (state.getBlock() != block) {
            return;
        }

        WoodPostBlock.Connections connections = WoodPostBlock.connections(level, pos, state);
        for (Direction direction : Direction.values()) {
            WoodPostBlock.ConnectionType type = connections.get(direction);
            if (type == WoodPostBlock.ConnectionType.NONE) {
                continue;
            }

            BlockStateModel connectionModel = BlockModelRegistry.getWoodPostConnectionModel(block, type, direction);
            if (connectionModel != null) {
                connectionModel.collectParts(level, pos, state, random, parts);
            }
        }
    }

    private record GeometryKey(Block block, Direction.Axis axis, WoodPostBlock.Connections connections) {
    }
}
