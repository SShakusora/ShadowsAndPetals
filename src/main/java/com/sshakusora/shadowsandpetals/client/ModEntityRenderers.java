package com.sshakusora.shadowsandpetals.client;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.EntityRegistry;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.VANITY.get(), VanityBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (WoodBlockList.WoodType woodType : WoodBlockList.WoodType.values()) {
            event.register(ModelResourceLocation.standalone(ShadowsAndPetals.asResource("block/vanity/" + woodType.getName() + "_drawer")));
        }
        register(event, "block/teapot/copper/lid");
        register(event, "block/shishi_odoshi/main");
        for (IroriFuelState.FirewoodModel model : IroriFuelState.FirewoodModel.values()) {
            register(event, "block/irori/firewood/" + model.modelName());
        }
        for (DyeColor color : DyeColor.values()) {
            register(event, WindChimeColors.blockBodyModelId(color));
            register(event, WindChimeColors.blockMainRibbonModelId(color));
            register(event, WindChimeColors.blockVaneModelId(color));
            register(event, "block/curtain/static/left/open/" + color.getName() + "/upper");
            register(event, "block/curtain/static/left/open/" + color.getName() + "/lower");
            register(event, "block/curtain/static/left/closed/" + color.getName() + "/upper");
            register(event, "block/curtain/static/left/closed/" + color.getName() + "/lower");
            register(event, "block/curtain/static/right/open/" + color.getName() + "/upper");
            register(event, "block/curtain/static/right/open/" + color.getName() + "/lower");
            register(event, "block/curtain/static/right/closed/" + color.getName() + "/upper");
            register(event, "block/curtain/static/right/closed/" + color.getName() + "/lower");
            for (String side : new String[]{"left", "right"}) {
                for (String pose : new String[]{"open", "closed"}) {
                    for (String half : new String[]{"upper", "lower"}) {
                        for (String column : new String[]{"inner", "outer"}) {
                            register(event, "block/large_curtain/static/" + side + "/" + pose + "/"
                                    + color.getName() + "/" + half + "_" + column);
                        }
                    }
                }
            }
        }
    }

    private static void register(ModelEvent.RegisterAdditional event, String path) {
        register(event, ShadowsAndPetals.asResource(path));
    }

    private static void register(ModelEvent.RegisterAdditional event, net.minecraft.resources.ResourceLocation id) {
        event.register(ModelResourceLocation.standalone(id));
    }
}
