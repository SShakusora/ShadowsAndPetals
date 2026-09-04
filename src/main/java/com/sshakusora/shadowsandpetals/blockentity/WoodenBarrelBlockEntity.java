package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import static net.minecraft.world.level.material.Fluids.WATER;

/**
 * Stores the barrel's fluid contents using NeoForge's current transfer API.
 */
public class WoodenBarrelBlockEntity extends BlockEntity {
    public static final int FLUID_CAPACITY = FluidType.BUCKET_VOLUME;
    public static final int BOTTLE_AMOUNT = FLUID_CAPACITY / 4;
    public static final int RAIN_AMOUNT = 50;

    private final WoodenBarrelFluidTank fluidTank = new WoodenBarrelFluidTank();

    public WoodenBarrelBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.WOODEN_BARREL.get(), pos, blockState);
    }

    public FluidStacksResourceHandler getFluidTank() {
        return fluidTank;
    }

    public boolean hasFluid() {
        return !fluidTank.getResource(0).isEmpty() && fluidTank.getAmountAsLong(0) > 0;
    }

    public boolean canInsert(FluidResource resource, int amount) {
        if (resource.isEmpty() || amount <= 0) {
            return false;
        }

        FluidResource current = fluidTank.getResource(0);
        return (current.isEmpty() || current.equals(resource))
                && amount <= FLUID_CAPACITY - fluidTank.getAmountAsInt(0);
    }

    public int insert(FluidResource resource, int amount) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }

        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = fluidTank.insert(resource, amount, transaction);
            if (inserted > 0) {
                transaction.commit();
            }
            return inserted;
        }
    }

    public boolean insertExactly(FluidResource resource, int amount) {
        if (!canInsert(resource, amount)) {
            return false;
        }

        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = fluidTank.insert(resource, amount, transaction);
            if (inserted != amount) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    public boolean canExtract(FluidResource resource, int amount) {
        return !resource.isEmpty()
                && amount > 0
                && fluidTank.getResource(0).equals(resource)
                && amount <= fluidTank.getAmountAsInt(0);
    }

    public boolean extractExactly(FluidResource resource, int amount) {
        if (!canExtract(resource, amount)) {
            return false;
        }

        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = fluidTank.extract(0, resource, amount, transaction);
            if (extracted != amount) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    public boolean canInsertWater(int amount) {
        return canInsert(FluidResource.of(WATER), amount);
    }

    public boolean canExtractWater(int amount) {
        return canExtract(FluidResource.of(WATER), amount);
    }

    public int insertWater(int amount) {
        return insert(FluidResource.of(WATER), amount);
    }

    public boolean insertWaterExactly(int amount) {
        return insertExactly(FluidResource.of(WATER), amount);
    }

    public boolean extractWaterExactly(int amount) {
        return extractExactly(FluidResource.of(WATER), amount);
    }

    public void fillFromRain() {
        if (level != null && !level.isClientSide()) {
            insertWater(RAIN_AMOUNT);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluidTank.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluidTank.serialize(output);
    }

    private void onFluidChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    private class WoodenBarrelFluidTank extends FluidStacksResourceHandler {
        private WoodenBarrelFluidTank() {
            super(1, FLUID_CAPACITY);
        }

        @Override
        protected void onContentsChanged(int index, net.neoforged.neoforge.fluids.FluidStack previousContents) {
            WoodenBarrelBlockEntity.this.onFluidChanged();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            if (index != 0 || resource.isEmpty()) {
                return false;
            }

            FluidResource current = getResource(index);
            return current.isEmpty() || current.equals(resource);
        }
    }
}
