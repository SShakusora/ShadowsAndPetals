package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class ShadowsAndPetalsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(IroriBurnTimeServerDataProvider.INSTANCE, IroriBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CafeChairBlockComponentProvider.INSTANCE, CafeChairBlock.class);
        registration.registerBlockComponent(IroriBurnTimeComponentProvider.INSTANCE, IroriBlock.class);
    }
}
