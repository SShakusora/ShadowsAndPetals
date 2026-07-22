package com.sshakusora.shadowsandpetals.client;

import com.mojang.datafixers.util.Either;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.ct.CTModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.WindChimeItemModel;
import com.sshakusora.shadowsandpetals.client.particle.FallingLeafParticle;
import com.sshakusora.shadowsandpetals.client.renderer.*;
import com.sshakusora.shadowsandpetals.client.screen.IroriScreen;
import com.sshakusora.shadowsandpetals.client.tooltip.ClientRockeryTooltip;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryPreviewRenderer;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryPreviewState;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryTooltipComponent;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipComponentRegistry;
import com.sshakusora.shadowsandpetals.foundation.tooltip.TooltipModifier;
import com.sshakusora.shadowsandpetals.item.hammer.HammerClientExtensions;
import com.sshakusora.shadowsandpetals.registries.*;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public class ClientRenderEvents {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.GINKGO.get(), FallingLeafParticle.GinkgoProvider::new);
        event.registerSpriteSet(ParticleRegistry.MAPLE.get(), FallingLeafParticle.MapleProvider::new);
        event.registerSpriteSet(ParticleRegistry.SAKURA.get(), FallingLeafParticle.SakuraProvider::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.IRORI.get(), IroriScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.IRORI.get(), IroriBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.VANITY.get(), VanityBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHISHI_ODOSHI.get(), ShishiOdoshiBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get(), ShishiOdoshiPipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.WIND_CHIME.get(), WindChimeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.COPPER_TEAPOT.get(), CopperTeapotBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        BlockModelRegistry.registerStandaloneModels(event);
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        WindChimeItemModel.register(event);
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

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new HammerClientExtensions(), ItemRegistry.HAMMER.get(), ItemRegistry.CHISEL.get());
    }

    @SubscribeEvent
    public static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(RockeryPreviewState.class, RockeryPreviewRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RockeryTooltipComponent.class, ClientRockeryTooltip::new);
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        for (TooltipComponentRegistry.Entry entry : TooltipComponentRegistry.gather(event.getItemStack())) {
            event.getTooltipElements().add(Either.right(entry.component()));
            event.setMaxWidth(Math.max(event.getMaxWidth(), entry.minimumWidth()));
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        TooltipModifier.applyIfPresent(event);
    }
}
