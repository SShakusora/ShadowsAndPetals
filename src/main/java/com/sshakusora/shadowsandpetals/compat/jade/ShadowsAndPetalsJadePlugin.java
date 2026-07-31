package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.block.decoration.RecessedLampCompositeBlock;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class ShadowsAndPetalsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(IroriBurnTimeServerDataProvider.INSTANCE, IroriBlock.class);
        registration.registerBlockDataProvider(SandExcavationCooldownServerDataProvider.INSTANCE, Block.class);
        registration.registerBlockDataProvider(RockeryHammerProgressServerDataProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(IroriBurnTimeComponentProvider.INSTANCE, IroriBlock.class);
        registration.registerBlockComponent(SandExcavationCooldownComponentProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(new RecessedLampHarvestComponentProvider(registration), RecessedLampCompositeBlock.class);
        registration.addTooltipCollectedCallback(RockeryHammerProgressOverlay::onTooltipCollected);
        registration.addAfterRenderCallback(200, RockeryHammerProgressOverlay::afterRender);
    }
}
