package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.ct.CTModelRegistry;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.EntityRegistry;
import com.sshakusora.shadowsandpetals.registries.ParticleRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public class ClientRenderEvents {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.GINKGO.get(), FallingLeafParticle.GinkgoProvider::new);
        event.registerSpriteSet(ParticleRegistry.MAPLE.get(), FallingLeafParticle.MapleProvider::new);
        event.registerSpriteSet(ParticleRegistry.SAKURA.get(), FallingLeafParticle.SakuraProvider::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.IRORI.get(), IroriBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.VANITY.get(), VanityBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        BlockModelRegistry.registerStandaloneModels(event);
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        BlockModelRegistry.wrapBlockStateModels(event);
        CTModelRegistry.wrapModels(event);
    }

    @SubscribeEvent
    public static void cacheStandaloneModels(ModelEvent.BakingCompleted event) {
        BlockModelRegistry.cacheBakedModels(event);
    }
}
