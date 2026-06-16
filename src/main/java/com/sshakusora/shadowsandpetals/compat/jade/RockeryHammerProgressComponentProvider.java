package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public final class RockeryHammerProgressComponentProvider implements IBlockComponentProvider {
    public static final RockeryHammerProgressComponentProvider INSTANCE = new RockeryHammerProgressComponentProvider();

    static final String PROGRESS_KEY = "RockeryHammerProgress";
    static final String DURATION_KEY = "RockeryHammerDuration";
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static final Identifier PROGRESS_UID = ShadowsAndPetals.asResource("jade.rockery_hammer_progress.bar");

    private RockeryHammerProgressComponentProvider() {}

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        int progressPercent = serverData.getInt(PROGRESS_KEY).orElse(-1);
        float duration = Math.max(1.0F, serverData.getFloat(DURATION_KEY).orElse(1.0F));
        if (progressPercent < 0) {
            return;
        }

        // Render a descriptive label
        tooltip.add(Component.translatable(BuiltinLanguageKeys.JADE_ROCKERY_HAMMERING.key())
                .withStyle(ChatFormatting.GRAY));

        // Render the progress bar
        float progress = Math.clamp(progressPercent / 100.0F, 0.0F, 1.0F);
        ProgressView.Part part = new ProgressView.PartBuilder()
                .id(0)
                .progress(progress)
                .target(1.0F / duration, 1.0F)
                .color(0xFFE0E0E0)
                .build();
        ProgressView view = new ProgressView(part, null, JadeUI.progressStyle().canDecrease(true), BoxStyle.nestedBox());
        tooltip.add(JadeUI.progress(view, BAR_WIDTH, BAR_HEIGHT).tag(PROGRESS_UID));
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.rockery_hammer_progress");
    }
}
