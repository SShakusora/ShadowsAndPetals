package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.ShishiOdoshiBlockEntity;
import com.sshakusora.shadowsandpetals.item.barrel.WoodenBarrelItemFluidHandler;
import com.sshakusora.shadowsandpetals.compat.transfer.NativeCapabilityAdapters;
import com.sshakusora.shadowsandpetals.compat.transfer.access.ItemAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.sshakusora.shadowsandpetals.compat.transfer.item.VanillaContainerWrapper;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public class CapabilityRegistry {

    @SubscribeEvent
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.SHISHI_ODOSHI.get(),
                (blockEntity, side) -> NativeCapabilityAdapters.fluids(
                        new ShishiOdoshiBlockEntity.FluidHandler(blockEntity))
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityRegistry.IRORI.get(),
                (blockEntity, side) -> NativeCapabilityAdapters.items(
                        VanillaContainerWrapper.of(blockEntity))
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityRegistry.BONSAI.get(),
                (blockEntity, side) -> side == null
                        ? NativeCapabilityAdapters.items(blockEntity.getPlantStorage())
                        : null
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.COPPER_TEAPOT.get(),
                (blockEntity, side) -> NativeCapabilityAdapters.fluids(blockEntity.getFluidTank())
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.WOODEN_BARREL.get(),
                (blockEntity, side) -> NativeCapabilityAdapters.fluids(blockEntity.getFluidTank())
        );
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ignored) -> NativeCapabilityAdapters.fluidItem(
                        new WoodenBarrelItemFluidHandler(ItemAccess.forStack(stack)), stack),
                BlockRegistry.WOODEN_BARREL.get()
        );
    }
}
