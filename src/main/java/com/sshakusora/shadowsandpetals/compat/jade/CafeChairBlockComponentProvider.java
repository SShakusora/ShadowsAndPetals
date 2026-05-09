package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class CafeChairBlockComponentProvider implements IBlockComponentProvider {
    public static final String DYEABLE_TOOLTIP_KEY = BuiltinLanguageKeys.CAFE_CHAIR_DYEABLE_TOOLTIP.key();
    public static final CafeChairBlockComponentProvider INSTANCE = new CafeChairBlockComponentProvider();

    private CafeChairBlockComponentProvider() {}

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        tooltip.add(Component.translatable(DYEABLE_TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.cafe_chair_dyeable");
    }
}
