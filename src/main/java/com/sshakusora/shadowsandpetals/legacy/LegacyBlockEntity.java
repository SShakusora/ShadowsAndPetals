package com.sshakusora.shadowsandpetals.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (input instanceof TagValueInput tagValueInput) {
            this.rawData = tagValueInput.input;
        }
    }
}
