package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class IroriBlockEntity extends BlockEntity {
    private boolean lit;

    public IroriBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.IRORI.get(), pos, blockState);
    }

    public boolean isLit() {
        return this.lit;
    }

    public boolean setLit(boolean lit) {
        if (this.lit == lit) {
            return false;
        }

        this.lit = lit;
        this.setChanged();
        Level level = this.getLevel();
        if (level != null) {
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
        return true;
    }

    public boolean setRegionLit(boolean lit) {
        Level level = this.getLevel();
        if (level == null) {
            return false;
        }

        IroriRegion region = this.getRegion();
        boolean shiftPlaced = this.getBlockState().getValue(IroriBlock.SHIFT_PLACED);
        boolean changed = false;
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int z = region.minZ(); z <= region.maxZ(); z++) {
                BlockPos currentPos = new BlockPos(x, this.worldPosition.getY(), z);
                BlockState currentState = level.getBlockState(currentPos);
                if (!isSameIroriGroup(currentState, shiftPlaced)) {
                    continue;
                }

                if (level.getBlockEntity(currentPos) instanceof IroriBlockEntity iroriBlockEntity) {
                    changed |= iroriBlockEntity.setLit(lit);
                }
            }
        }
        return changed;
    }

    public boolean isMaster() {
        return getRegion().masterPos().equals(this.worldPosition);
    }

    public BlockPos getMasterPos() {
        return getRegion().masterPos();
    }

    public IroriRegion getRegion() {
        Level level = getLevel();
        if (level == null) {
            return IroriRegion.single(this.worldPosition);
        }
        return calculateRegion(level, this.worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.lit = input.getBooleanOr("lit", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("lit", this.lit);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    private static IroriRegion calculateRegion(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!(originState.getBlock() instanceof IroriBlock)) {
            return IroriRegion.single(origin);
        }

        boolean shiftPlaced = originState.getValue(IroriBlock.SHIFT_PLACED);
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos immutableOrigin = origin.immutable();
        visited.add(immutableOrigin);
        queue.add(immutableOrigin);

        int minX = origin.getX();
        int maxX = origin.getX();
        int minZ = origin.getZ();
        int maxZ = origin.getZ();

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            BlockState currentState = level.getBlockState(current);
            if (!isSameIroriGroup(currentState, shiftPlaced)) {
                continue;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!currentState.getValue(IroriBlock.getConnectionProperty(direction))) {
                    continue;
                }

                BlockPos next = current.relative(direction).immutable();
                if (visited.contains(next)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(next);
                if (!isSameIroriGroup(nextState, shiftPlaced)
                        || !nextState.getValue(IroriBlock.getConnectionProperty(direction.getOpposite()))) {
                    continue;
                }

                visited.add(next);
                queue.add(next);
                minX = Math.min(minX, next.getX());
                maxX = Math.max(maxX, next.getX());
                minZ = Math.min(minZ, next.getZ());
                maxZ = Math.max(maxZ, next.getZ());
            }
        }

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        BlockPos masterPos = new BlockPos(minX + (width - 1) / 2, origin.getY(), minZ + (depth - 1) / 2);
        return new IroriRegion(masterPos, minX, maxX, minZ, maxZ, width, depth);
    }

    private static boolean isSameIroriGroup(BlockState state, boolean shiftPlaced) {
        return state.getBlock() instanceof IroriBlock && state.getValue(IroriBlock.SHIFT_PLACED) == shiftPlaced;
    }

    public record IroriRegion(BlockPos masterPos, int minX, int maxX, int minZ, int maxZ, int width, int depth) {
        private static IroriRegion single(BlockPos pos) {
            return new IroriRegion(pos.immutable(), pos.getX(), pos.getX(), pos.getZ(), pos.getZ(), 1, 1);
        }

        public double centerX() {
            return this.minX + this.width / 2.0D;
        }

        public double centerZ() {
            return this.minZ + this.depth / 2.0D;
        }
    }
}
