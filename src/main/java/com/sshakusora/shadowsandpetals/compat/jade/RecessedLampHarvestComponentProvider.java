package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.RecessedLampBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IPluginConfig;

import java.util.Objects;

public final class RecessedLampHarvestComponentProvider implements IBlockComponentProvider {
    private final IWailaClientRegistration registration;

    public RecessedLampHarvestComponentProvider(IWailaClientRegistration registration) {
        this.registration = Objects.requireNonNull(registration);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof RecessedLampBlockEntity recessedLamp)) {
            return;
        }

        BlockState storedSlab = recessedLamp.getEffectiveStoredSlab();
        if (storedSlab == null) {
            return;
        }

        // Let Jade's own harvest provider handle layout, tool icons, colors, and configuration.
        BlockAccessor storedSlabAccessor = registration.blockAccessor()
                .from(accessor)
                .blockState(storedSlab)
                .build();
        HarvestToolProvider.INSTANCE.appendTooltip(tooltip, storedSlabAccessor, config);
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.recessed_lamp_harvest");
    }

    @Override
    public int getDefaultPriority() {
        return HarvestToolProvider.INSTANCE.getDefaultPriority();
    }
}
