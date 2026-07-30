package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jspecify.annotations.Nullable;

public final class RecessedLampBlockEntity extends BlockEntity {
    private static final String STORED_SLAB_KEY = "StoredSlab";

    public static final ModelProperty<BlockState> STORED_SLAB_MODEL_PROPERTY =
            new ModelProperty<>(RecessedLampBlockEntity::isValidStoredSlab);

    private @Nullable BlockState storedSlab;

    public RecessedLampBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.RECESSED_LAMP.get(), pos, blockState);
    }

    public @Nullable BlockState getStoredSlab() {
        return storedSlab;
    }

    public @Nullable BlockState getEffectiveStoredSlab() {
        return applyHostWaterlogged(storedSlab, getBlockState());
    }

    public void setStoredSlab(BlockState storedSlab) {
        if (!isValidStoredSlab(storedSlab)) {
            throw new IllegalArgumentException("Unsupported recessed-lamp host slab: " + storedSlab);
        }

        this.storedSlab = storedSlab;
        setChanged();
        requestModelDataUpdate();

        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedSlab = input.read(STORED_SLAB_KEY, BlockState.CODEC)
                .filter(RecessedLampBlockEntity::isValidStoredSlab)
                .orElse(null);
        requestModelDataUpdate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (storedSlab != null) {
            output.store(STORED_SLAB_KEY, BlockState.CODEC, storedSlab);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ModelData getModelData() {
        BlockState effectiveSlab = getEffectiveStoredSlab();
        return effectiveSlab == null
                ? ModelData.EMPTY
                : ModelData.of(STORED_SLAB_MODEL_PROPERTY, effectiveSlab);
    }

    public static boolean isValidStoredSlab(@Nullable BlockState state) {
        return state != null
                && state.getBlock() instanceof SlabBlock
                && state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE
                && !state.hasBlockEntity();
    }

    public static @Nullable BlockState applyHostWaterlogged(
            @Nullable BlockState storedSlab,
            BlockState hostState
    ) {
        if (storedSlab == null
                || !storedSlab.hasProperty(BlockStateProperties.WATERLOGGED)
                || !hostState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return storedSlab;
        }
        return storedSlab.setValue(
                BlockStateProperties.WATERLOGGED,
                hostState.getValue(BlockStateProperties.WATERLOGGED)
        );
    }
}
