package com.sshakusora.shadowsandpetals.client;

import com.mojang.datafixers.util.Either;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.client.animation.SAPAnimationResources;
import com.sshakusora.shadowsandpetals.client.animation.SAPAnimations;
import com.sshakusora.shadowsandpetals.client.ct.CTModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.BlockModelRegistry;
import com.sshakusora.shadowsandpetals.client.model.RecessedLampCompositeClientExtensions;
import com.sshakusora.shadowsandpetals.client.model.WindChimeItemModel;
import com.sshakusora.shadowsandpetals.client.outline.BlockOutlineRegistry;
import com.sshakusora.shadowsandpetals.client.outline.BlockOutlineRenderer;
import com.sshakusora.shadowsandpetals.client.particle.FallingLeafParticle;
import com.sshakusora.shadowsandpetals.client.renderer.*;
import com.sshakusora.shadowsandpetals.client.screen.IroriScreen;
import com.sshakusora.shadowsandpetals.client.screen.TeapotScreen;
import com.sshakusora.shadowsandpetals.client.tooltip.ClientRockeryTooltip;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryPreviewRenderer;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryPreviewState;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryTooltipComponent;
import com.sshakusora.shadowsandpetals.item.hammer.HammerClientExtensions;
import com.sshakusora.shadowsandpetals.item.harrow.HarrowClientExtensions;
import com.sshakusora.shadowsandpetals.registries.*;
import com.sshakusora.shadowsandpetals.registries.builder.RegFluidBuilder;
import com.sshakusora.shadowsandpetals.tooltip.TooltipComponentRegistry;
import com.sshakusora.shadowsandpetals.tooltip.TooltipModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.resource.NeoForgeReloadListeners;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public class ClientRenderEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        HammerClientExtensions.clientTick();
        HarrowClientExtensions.clientTick();
    }

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
        event.registerEntityRenderer(EntityRegistry.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SAND_EXCAVATION.get(), SandExcavationBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.IRORI.get(), IroriBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.VANITY.get(), VanityBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHISHI_ODOSHI.get(), ShishiOdoshiBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.SHISHI_ODOSHI_PIPE.get(), ShishiOdoshiPipeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.WIND_CHIME.get(), WindChimeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.COPPER_TEAPOT.get(), CopperTeapotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.BONSAI.get(), BonsaiBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void extractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        var context = new BlockOutlineContext(
                event.getBlockPos(),
                event.getHitResult(),
                event.getCollisionContext()
        );
        var geometry = BlockOutlineRegistry.createGeometry(event.getBlockState(), context);
        if (geometry == null) {
            return;
        }

        float lineWidth = Minecraft.getInstance()
                .gameRenderer
                .getGameRenderState()
                .windowRenderState
                .appropriateLineWidth;
        event.addCustomRenderer(new BlockOutlineRenderer(geometry, lineWidth));
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
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        for (var definition : RegFluidBuilder.clientModels()) {
            event.register(
                    new FluidModel.Unbaked(
                            new Material(definition.stillTexture()),
                            new Material(definition.flowingTexture()),
                            definition.overlayTexture().map(Material::new).orElse(null),
                            FluidTintSources.constant(definition.tintColor())
                    ),
                    definition.source(),
                    definition.flowing()
            );
        }
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        BlockModelRegistry.wrapBlockStateModels(event);
        CTModelRegistry.wrapModels(event);
        BlockModelRegistry.wrapRecessedLampCompositeModels(event);
    }

    @SubscribeEvent
    public static void cacheStandaloneModels(ModelEvent.BakingCompleted event) {
        BlockModelRegistry.cacheBakedModels(event);
        BonsaiBlockEntityRenderer.invalidateCaches();
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new HammerClientExtensions(), ItemRegistry.HAMMER.get(), ItemRegistry.CHISEL.get());
        event.registerItem(new HarrowClientExtensions(), ItemRegistry.HARROW.get());
        event.registerBlock(
                new RecessedLampCompositeClientExtensions(),
                BlockRegistry.RECESSED_LAMP_COMPOSITE.get()
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        SAPAnimations.init();
        var key = ShadowsAndPetals.asResource("pose_animations");
        event.addListener(key, SAPAnimationResources.INSTANCE);
        event.addDependency(NeoForgeReloadListeners.ENTITY_ANIMATIONS, key);
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
