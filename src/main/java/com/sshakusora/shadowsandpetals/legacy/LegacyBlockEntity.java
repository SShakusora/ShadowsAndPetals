package com.sshakusora.shadowsandpetals.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class LegacyBlockEntity extends BlockEntity {
    private CompoundTag rawData;

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
            try {
                Field field = TagValueInput.class.getDeclaredField("input");
                field.setAccessible(true);
                this.rawData = (CompoundTag) field.get(tagValueInput);
            } catch (Exception e) {
                this.rawData = new CompoundTag();
            }
        } else {
            this.rawData = new CompoundTag();
        }
    }
}
