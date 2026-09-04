package com.sshakusora.shadowsandpetals.client.model;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
import com.sshakusora.shadowsandpetals.client.renderer.ClientFluidRenderInfo;
import com.sshakusora.shadowsandpetals.client.renderer.WoodenBarrelFluidGeometry;
import com.sshakusora.shadowsandpetals.client.renderer.WoodenBarrelFluidSpecialRenderer;
import com.sshakusora.shadowsandpetals.item.barrel.WoodenBarrelItemFluid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class WoodenBarrelItemModel implements ItemModel {
    public static final Identifier TYPE = ShadowsAndPetals.asResource("wooden_barrel");
    private static final Identifier BASE_MODEL = ShadowsAndPetals.asResource("item/wooden_barrel");
    private static final int GUI_FLUID_LEVELS = 16;

    private final ItemModel baseModel;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Supplier<Vector3fc[]> fluidExtents = () -> {
        java.util.Set<Vector3fc> extents = new java.util.HashSet<>();
        WoodenBarrelFluidGeometry.addExtents(extents::add);
        return extents.toArray(Vector3fc[]::new);
    };

    private WoodenBarrelItemModel(
            ItemModel baseModel,
            ModelRenderProperties properties,
            Matrix4fc transformation
    ) {
        this.baseModel = baseModel;
        this.properties = properties;
        this.transformation = transformation;
    }

    public static void register(RegisterItemModelsEvent event) {
        event.register(TYPE, Unbaked.MAP_CODEC);
    }

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed
    ) {
        baseModel.update(output, item, resolver, displayContext, level, owner, seed);

        Optional<FluidStack> fluidResult = WoodenBarrelItemFluid.read(item);
        if (fluidResult.isEmpty()) {
            return;
        }

        var fluidStack = fluidResult.get();
        int amount = Math.min(fluidStack.getAmount(), WoodenBarrelBlockEntity.FLUID_CAPACITY);
        if (amount <= 0) {
            return;
        }

        ClientLevel sampleLevel = level;
        BlockPos samplePos = null;
        if (owner != null) {
            samplePos = BlockPos.containing(owner.position());
            if (sampleLevel == null && owner.level() instanceof ClientLevel ownerLevel) {
                sampleLevel = ownerLevel;
            }
        }
        if (samplePos == null && Minecraft.getInstance().player != null) {
            samplePos = Minecraft.getInstance().player.blockPosition();
        }
        if (sampleLevel == null) {
            sampleLevel = Minecraft.getInstance().level;
        }

        ClientFluidRenderInfo.Info renderInfo = ClientFluidRenderInfo.createItemSurface(
                fluidStack,
                sampleLevel,
                samplePos
        );
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(fluidStack.getFluid());
        output.appendModelIdentityElement(Math.clamp(
                amount * GUI_FLUID_LEVELS / WoodenBarrelBlockEntity.FLUID_CAPACITY,
                1,
                GUI_FLUID_LEVELS
        ));
        output.appendModelIdentityElement(renderInfo.color());
        if (renderInfo.sprite().contents().isAnimated()) {
            output.setAnimated();
        }
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            output.setAnimated();
        }
        layer.setExtents(fluidExtents);
        layer.setLocalTransform(transformation);
        layer.setupSpecialModel(
                WoodenBarrelFluidSpecialRenderer.INSTANCE,
                new WoodenBarrelFluidSpecialRenderer.RenderData(
                        renderInfo.sprite(),
                        renderInfo.color(),
                        amount,
                        WoodenBarrelBlockEntity.FLUID_CAPACITY,
                        renderInfo.lightEmission()
                )
        );
        properties.applyToLayer(layer, displayContext);
    }

    public enum Unbaked implements ItemModel.Unbaked {
        INSTANCE;

        public static final MapCodec<WoodenBarrelItemModel.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<WoodenBarrelItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            child().resolveDependencies(resolver);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolvedModel = baker.getModel(BASE_MODEL);
            TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(
                    baker,
                    resolvedModel,
                    textureSlots
            );
            return new WoodenBarrelItemModel(
                    child().bake(context, transformation),
                    properties,
                    new Matrix4f(transformation)
            );
        }

        private static CuboidItemModelWrapper.Unbaked child() {
            return new CuboidItemModelWrapper.Unbaked(BASE_MODEL, Optional.empty(), List.of());
        }
    }
}
