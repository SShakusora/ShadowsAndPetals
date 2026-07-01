package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.item.hammer.HammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public final class RockeryHammerProgressServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final RockeryHammerProgressServerDataProvider INSTANCE = new RockeryHammerProgressServerDataProvider();
    static final String PROGRESS_KEY = "RockeryHammerProgress";
    static final String DURATION_KEY = "RockeryHammerDuration";

    private RockeryHammerProgressServerDataProvider() {}

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockPos pos = accessor.getPosition();
        ItemStack item = accessor.getPlayer().getMainHandItem();
        if (!(item.getItem() instanceof HammerItem)) {
            return;
        }
        float progress = HammerItem.getHammeringProgress(accessor.getPlayer(), accessor.getLevel(), pos);
        float duration = HammerItem.getEffectiveUseDuration(item);
        if (progress < 0.0F) {
            return;
        }
        data.putInt(PROGRESS_KEY, Math.round(progress * 100));
        data.putFloat(DURATION_KEY, duration);
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return accessor.getPlayer().getMainHandItem().getItem() instanceof HammerItem
                && accessor.getPlayer().isUsingItem();
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.rockery_hammer_progress");
    }
}
