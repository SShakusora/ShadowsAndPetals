package com.sshakusora.shadowsandpetals.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class LegacyBlockEntity extends BlockEntity {
    private CompoundTag rawData = new CompoundTag();

    public LegacyBlockEntity(Supplier<BlockEntityType<?>> typeSupplier, BlockPos pos, BlockState blockState) {
        super(typeSupplier.get(), pos, blockState);
    }

    public CompoundTag getRawData() {
        return rawData;
    }

    @Override
    protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        this.rawData = input.copy();
    }
}
