package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public final class IroriBurnTimeComponentProvider implements IBlockComponentProvider {
    public static final IroriBurnTimeComponentProvider INSTANCE = new IroriBurnTimeComponentProvider();

    static final String BURN_TIME_KEY = "IroriBurnTime";
    static final String BURN_TIME_TOTAL_KEY = "IroriBurnTimeTotal";
    static final String BURN_CYCLE_KEY = "IroriBurnCycle";
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static final Identifier PROGRESS_UID = ShadowsAndPetals.asResource("jade.irori_burn_time.progress");

    private IroriBurnTimeComponentProvider() {}

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof IroriBlockEntity irori)) {
            return;
        }

        CompoundTag serverData = accessor.getServerData();
        int burnTime = serverData.getInt(BURN_TIME_KEY).orElse(-1);
        int burnTimeTotal = serverData.getInt(BURN_TIME_TOTAL_KEY).orElse(-1);
        int burnCycle = serverData.getInt(BURN_CYCLE_KEY).orElse(-1);
        if (burnTime < 0 || burnTimeTotal < 0) {
            burnTime = irori.getBurnTime();
            burnTimeTotal = irori.getBurnTimeTotal();
            burnCycle = irori.getBurnCycle();
        } else if (burnCycle < 0) {
            burnCycle = irori.getBurnCycle();
        }
        if (burnTime <= 0 || burnTimeTotal <= 0) {
            if (irori.hasAsh()) {
                tooltip.add(Component.translatable(BuiltinLanguageKeys.JADE_IRORI_BURNED_OUT.key()).withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        ProgressView.Part part = new ProgressView.PartBuilder()
                .id(burnCycle)
                .progress(Math.clamp(burnTime / (float) burnTimeTotal, 0.0F, 1.0F))
                .target(-1.0F / burnTimeTotal, 0.0F)
                .color(0xFFE0E0E0)
                .build();
        ProgressView view = new ProgressView(
                part,
                null,
                JadeUI.progressStyle().canDecrease(true),
                BoxStyle.nestedBox()
        );
        tooltip.add(Component.translatable(BuiltinLanguageKeys.JADE_IRORI_BURNING.key()).withStyle(ChatFormatting.GRAY));
        tooltip.add(JadeUI.progress(view, BAR_WIDTH, BAR_HEIGHT).tag(PROGRESS_UID));
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.irori_burn_time");
    }

}
