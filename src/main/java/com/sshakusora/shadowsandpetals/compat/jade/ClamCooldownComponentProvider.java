package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public final class ClamCooldownComponentProvider implements IBlockComponentProvider {
    public static final ClamCooldownComponentProvider INSTANCE = new ClamCooldownComponentProvider();
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;
    private static final Identifier PROGRESS_UID = ShadowsAndPetals.asResource("jade.clam_cooldown.progress");

    private ClamCooldownComponentProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getBlockState().is(Blocks.SAND)) {
            return;
        }

        long cooldownEndTick = accessor.getServerData()
                .getLong(ClamCooldownServerDataProvider.COOLDOWN_END_TICK_KEY)
                .orElse(0L);
        long cooldownDuration = accessor.getServerData()
                .getLong(ClamCooldownServerDataProvider.COOLDOWN_DURATION_KEY)
                .orElse(0L);
        long remainingTicks = cooldownEndTick - accessor.getLevel().getGameTime();
        if (remainingTicks <= 0L || cooldownDuration <= 0L) {
            return;
        }

        ProgressView.Part part = new ProgressView.PartBuilder()
                .id(Long.hashCode(cooldownEndTick))
                .progress(Math.clamp(remainingTicks / (float) cooldownDuration, 0.0F, 1.0F))
                .target(-1.0F / cooldownDuration, 0.0F)
                .color(0xFFE0E0E0)
                .build();
        ProgressView view = new ProgressView(
                part,
                null,
                JadeUI.progressStyle().canDecrease(true),
                BoxStyle.nestedBox()
        );
        tooltip.add(Component.translatable(BuiltinLanguageKeys.JADE_CLAM_COOLDOWN.key())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(JadeUI.progress(view, BAR_WIDTH, BAR_HEIGHT).tag(PROGRESS_UID));
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.clam_cooldown");
    }
}
