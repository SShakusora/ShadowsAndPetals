package com.sshakusora.shadowsandpetals.client.model;

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

@SuppressWarnings("ConstantConditions")
public final class WoodPostBlockStateModel extends DelegateBlockStateModel implements DynamicBlockStateModel {
    private final Block block;
    private static final WoodPostBlock.Connections NO_CONNECTIONS = new WoodPostBlock.Connections(
            WoodPostBlock.ConnectionType.NONE,
            WoodPostBlock.ConnectionType.NONE,
            WoodPostBlock.ConnectionType.NONE,
            WoodPostBlock.ConnectionType.NONE,
            WoodPostBlock.ConnectionType.NONE,
            WoodPostBlock.ConnectionType.NONE);

    public WoodPostBlockStateModel(Block block, BlockStateModel delegate) {
        super(delegate);
        this.block = block;
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (state == null || state.getBlock() != block) {
            return new GeometryKey(state == null ? block : state.getBlock(), Direction.Axis.Y, NO_CONNECTIONS);
        }

        return new GeometryKey(state.getBlock(), state.getValue(WoodPostBlock.AXIS), WoodPostBlock.connections(level, pos, state));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        if (state == null || state.getBlock() != block) {
            delegate.collectParts(random, parts);
            return;
        }

        delegate.collectParts(level, pos, state, random, parts);

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
