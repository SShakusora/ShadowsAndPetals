package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public class CapabilityRegistry {

    @SubscribeEvent
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                BlockEntityRegistry.SHISHI_ODOSHI.get(),
                (blockEntity, side) -> new ShishiOdoshiBlockEntity.FluidHandler(blockEntity)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                BlockEntityRegistry.IRORI.get(),
                (blockEntity, side) -> VanillaContainerWrapper.of(blockEntity)
        );
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                BlockEntityRegistry.BONSAI.get(),
                (blockEntity, side) -> side == null ? blockEntity.getPlantStorage() : null
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                BlockEntityRegistry.COPPER_TEAPOT.get(),
                (blockEntity, side) -> blockEntity.getFluidTank()
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                BlockEntityRegistry.WOODEN_BARREL.get(),
                (blockEntity, side) -> blockEntity.getFluidTank()
        );
    }
}
