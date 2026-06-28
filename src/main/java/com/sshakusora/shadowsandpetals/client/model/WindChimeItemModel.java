package com.sshakusora.shadowsandpetals.client.model;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WindChimeItemModel implements ItemModel {
    public static final Identifier TYPE = ShadowsAndPetals.asResource("wind_chime");

    private final ItemModel body;
    private final Map<DyeColor, ItemModel> ribbons;
    private final Map<DyeColor, ItemModel> vanes;

    private WindChimeItemModel(ItemModel body, Map<DyeColor, ItemModel> ribbons, Map<DyeColor, ItemModel> vanes) {
        this.body = body;
        this.ribbons = ribbons;
        this.vanes = vanes;
    }

    public static void register(RegisterItemModelsEvent event) {
        event.register(TYPE, Unbaked.MAP_CODEC);
    }

    @Override
    public void update(
            ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
            ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed
    ) {
        WindChimeColors colors = WindChimeColors.fromStack(item);
        output.appendModelIdentityElement(this);
        output.ensureCapacity(3);
        body.update(output, item, resolver, displayContext, level, owner, seed);
        ribbons.getOrDefault(colors.ribbon(), ribbons.get(WindChimeColors.DEFAULT_COLOR))
                .update(output, item, resolver, displayContext, level, owner, seed);
        vanes.getOrDefault(colors.vane(), vanes.get(WindChimeColors.DEFAULT_COLOR))
                .update(output, item, resolver, displayContext, level, owner, seed);
    }

    public enum Unbaked implements ItemModel.Unbaked {
        INSTANCE;

        public static final MapCodec<WindChimeItemModel.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<WindChimeItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            child(WindChimeColors.itemBodyModelId()).resolveDependencies(resolver);
            for (DyeColor color : DyeColor.values()) {
                child(WindChimeColors.itemRibbonModelId(color)).resolveDependencies(resolver);
                child(WindChimeColors.itemVaneModelId(color)).resolveDependencies(resolver);
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ItemModel body = child(WindChimeColors.itemBodyModelId()).bake(context, transformation);
            Map<DyeColor, ItemModel> ribbons = new EnumMap<>(DyeColor.class);
            Map<DyeColor, ItemModel> vanes = new EnumMap<>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                ribbons.put(color, child(WindChimeColors.itemRibbonModelId(color)).bake(context, transformation));
                vanes.put(color, child(WindChimeColors.itemVaneModelId(color)).bake(context, transformation));
            }
            return new WindChimeItemModel(body, Map.copyOf(ribbons), Map.copyOf(vanes));
        }

        private static CuboidItemModelWrapper.Unbaked child(Identifier model) {
            return new CuboidItemModelWrapper.Unbaked(model, Optional.empty(), List.of());
        }
    }
}
