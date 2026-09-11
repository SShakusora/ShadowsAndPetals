package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.particle.FallingLeafParticle;
import com.sshakusora.shadowsandpetals.client.renderer.*;
import com.sshakusora.shadowsandpetals.client.screen.IroriScreen;
import com.sshakusora.shadowsandpetals.client.screen.TeapotScreen;
import com.sshakusora.shadowsandpetals.item.hammer.HammerClientExtensions;
import com.sshakusora.shadowsandpetals.item.harrow.HarrowClientExtensions;
import com.sshakusora.shadowsandpetals.registries.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
@SuppressWarnings({"removal"})
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.GINKGO.get(), FallingLeafParticle.GinkgoProvider::new);
        event.registerSpriteSet(ParticleRegistry.MAPLE.get(), FallingLeafParticle.MapleProvider::new);
        event.registerSpriteSet(ParticleRegistry.SAKURA.get(), FallingLeafParticle.SakuraProvider::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.IRORI.get(), IroriScreen::new);
        event.register(MenuRegistry.TEAPOT.get(), TeapotScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SAND_EXCAVATION.get(), SandExcavationBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.IRORI.get(), IroriBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.VANITY.get(), VanityBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHISHI_ODOSHI.get(), ShishiOdoshiBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get(), ShishiOdoshiPipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.WOODEN_BARREL.get(), WoodenBarrelBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.CURTAIN.get(), CurtainBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.WIND_CHIME.get(), WindChimeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.LARGE_CURTAIN.get(), LargeCurtainBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.COPPER_TEAPOT.get(), CopperTeapotBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new HammerClientExtensions(), ItemRegistry.HAMMER.get(), ItemRegistry.CHISEL.get());
        event.registerItem(new HarrowClientExtensions(), ItemRegistry.HARROW.get());
        event.registerBlock(new com.sshakusora.shadowsandpetals.client.model.RecessedLampCompositeClientExtensions(),
                BlockRegistry.RECESSED_LAMP_COMPOSITE.get());
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
    }
}
