package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public final class IroriBurnTimeServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final IroriBurnTimeServerDataProvider INSTANCE = new IroriBurnTimeServerDataProvider();

    private IroriBurnTimeServerDataProvider() {}

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof IroriBlockEntity irori)) {
            return;
        }

        int burnTime = irori.getBurnTime();
        int burnTimeTotal = irori.getBurnTimeTotal();
        if (burnTime <= 0 || burnTimeTotal <= 0) {
            return;
        }

        data.putInt(IroriBurnTimeComponentProvider.BURN_TIME_KEY, burnTime);
        data.putInt(IroriBurnTimeComponentProvider.BURN_TIME_TOTAL_KEY, burnTimeTotal);
        data.putInt(IroriBurnTimeComponentProvider.BURN_CYCLE_KEY, irori.getBurnCycle());
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.irori_burn_time");
    }
}
