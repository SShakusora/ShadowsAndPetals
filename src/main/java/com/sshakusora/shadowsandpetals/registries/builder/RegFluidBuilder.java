package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.data.DatagenBlockStateRegistry;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Fluent builder for a standard source/flowing fluid pair and its {@link FluidType}.
 */
public final class RegFluidBuilder {
    private static final List<ClientModelDefinition> CLIENT_MODELS = new ArrayList<>();

    private final DeferredRegister.Blocks blockRegistry;
    private final DeferredRegister<Fluid> fluidRegistry;
    private final DeferredRegister<FluidType> fluidTypeRegistry;
    private final String name;
    private UnaryOperator<FluidType.Properties> typeProperties = UnaryOperator.identity();
    private UnaryOperator<BaseFlowingFluid.Properties> fluidProperties = UnaryOperator.identity();
    private Function<FluidType.Properties, FluidType> typeFactory = FluidType::new;
    private Function<BaseFlowingFluid.Properties, BaseFlowingFluid.Source> sourceFactory =
            BaseFlowingFluid.Source::new;
    private Function<BaseFlowingFluid.Properties, BaseFlowingFluid.Flowing> flowingFactory =
            BaseFlowingFluid.Flowing::new;
    private UnaryOperator<BlockBehaviour.Properties> liquidBlockProperties;
    private Identifier stillTexture;
    private Identifier flowingTexture;
    private Optional<Identifier> overlayTexture = Optional.empty();
    private int tintColor;
    private final Map<String, String> langNames = new LinkedHashMap<>();

    public RegFluidBuilder(
            DeferredRegister.Blocks blockRegistry,
            DeferredRegister<Fluid> fluidRegistry,
            DeferredRegister<FluidType> fluidTypeRegistry,
            String name
    ) {
        this.blockRegistry = blockRegistry;
        this.fluidRegistry = fluidRegistry;
        this.fluidTypeRegistry = fluidTypeRegistry;
        this.name = name;
    }

    /** Configures the properties used to construct the fluid type. */
    public RegFluidBuilder typeProperties(UnaryOperator<FluidType.Properties> configurator) {
        this.typeProperties = Objects.requireNonNull(configurator);
        return this;
    }

    /** Configures properties shared by the source and flowing fluid instances. */
    public RegFluidBuilder properties(UnaryOperator<BaseFlowingFluid.Properties> configurator) {
        this.fluidProperties = Objects.requireNonNull(configurator);
        return this;
    }

    /**
     * Registers a world-placeable liquid block with water-like block properties.
     */
    public RegFluidBuilder liquidBlock() {
        return liquidBlock(properties -> properties
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollision()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY));
    }

    /** Registers a world-placeable liquid block with custom block properties. */
    public RegFluidBuilder liquidBlock(UnaryOperator<BlockBehaviour.Properties> configurator) {
        this.liquidBlockProperties = Objects.requireNonNull(configurator);
        return this;
    }

    /** Configures the client fluid model textures and tint. */
    public RegFluidBuilder clientModel(Identifier stillTexture, Identifier flowingTexture, int tintColor) {
        return clientModel(stillTexture, flowingTexture, Optional.empty(), tintColor);
    }

    /** Configures the client fluid model textures, optional overlay and tint. */
    public RegFluidBuilder clientModel(
            Identifier stillTexture,
            Identifier flowingTexture,
            Optional<Identifier> overlayTexture,
            int tintColor
    ) {
        this.stillTexture = Objects.requireNonNull(stillTexture);
        this.flowingTexture = Objects.requireNonNull(flowingTexture);
        this.overlayTexture = Objects.requireNonNull(overlayTexture);
        this.tintColor = tintColor;
        return this;
    }

    /** Replaces the default {@link FluidType} factory. */
    public RegFluidBuilder typeFactory(Function<FluidType.Properties, ? extends FluidType> factory) {
        Objects.requireNonNull(factory);
        this.typeFactory = properties -> factory.apply(properties);
        return this;
    }

    /** Replaces the default source-fluid factory. */
    public RegFluidBuilder sourceFactory(
            Function<BaseFlowingFluid.Properties, ? extends BaseFlowingFluid.Source> factory
    ) {
        Objects.requireNonNull(factory);
        this.sourceFactory = properties -> factory.apply(properties);
        return this;
    }

    /** Replaces the default flowing-fluid factory. */
    public RegFluidBuilder flowingFactory(
            Function<BaseFlowingFluid.Properties, ? extends BaseFlowingFluid.Flowing> factory
    ) {
        Objects.requireNonNull(factory);
        this.flowingFactory = properties -> factory.apply(properties);
        return this;
    }

    /** Adds the default English language entry for the fluid type. */
    public RegFluidBuilder lang(String value) {
        return lang(DatagenLangRegistry.DEFAULT_LOCALE, value);
    }

    /** Adds a locale-specific language entry for the fluid type. */
    public RegFluidBuilder lang(String locale, String value) {
        this.langNames.put(locale, value);
        return this;
    }

    /** Registers the type, source and flowing entries as one fluid family. */
    public RegisteredFluid register() {
        String descriptionId = "fluid_type." + ShadowsAndPetals.MOD_ID + "." + name;
        Registration registration = new Registration();

        registration.type = fluidTypeRegistry.register(name, () -> typeFactory.apply(
                typeProperties.apply(FluidType.Properties.create().descriptionId(descriptionId))
        ));
        registration.source = fluidRegistry.register(name, () -> sourceFactory.apply(
                createFluidProperties(registration)
        ));
        registration.flowing = fluidRegistry.register("flowing_" + name, () -> flowingFactory.apply(
                createFluidProperties(registration)
        ));
        if (liquidBlockProperties != null) {
            registration.block = blockRegistry.registerBlock(
                    name,
                    properties -> new LiquidBlock(registration.source.get(), properties),
                    liquidBlockProperties
            );
        }

        DatagenLangRegistry.addFallback(descriptionId, name);
        for (var entry : langNames.entrySet()) {
            DatagenLangRegistry.add(entry.getKey(), descriptionId, entry.getValue());
        }
        if (registration.block != null) {
            String blockDescriptionId = "block." + ShadowsAndPetals.MOD_ID + "." + name;
            DatagenLangRegistry.addFallback(blockDescriptionId, name);
            for (var entry : langNames.entrySet()) {
                DatagenLangRegistry.add(entry.getKey(), blockDescriptionId, entry.getValue());
            }

            Identifier particleTexture = stillTexture != null
                    ? stillTexture
                    : Identifier.withDefaultNamespace("block/water_still");
            DatagenBlockStateRegistry.add(
                    registration.block.getId(),
                    provider -> provider.fluidBlock(registration.block.get(), particleTexture)
            );
        }

        RegisteredFluid registeredFluid = new RegisteredFluid(
                registration.type,
                registration.source,
                registration.flowing,
                Optional.ofNullable(registration.block)
        );
        if (stillTexture != null && flowingTexture != null) {
            CLIENT_MODELS.add(new ClientModelDefinition(
                    stillTexture,
                    flowingTexture,
                    overlayTexture,
                    tintColor,
                    registeredFluid.source,
                    registeredFluid.flowing
            ));
        }
        return registeredFluid;
    }

    public static List<ClientModelDefinition> clientModels() {
        return List.copyOf(CLIENT_MODELS);
    }

    private BaseFlowingFluid.Properties createFluidProperties(Registration registration) {
        BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(
                registration.type::get,
                registration.source::get,
                registration.flowing::get
        );
        if (registration.block != null) {
            properties.block(registration.block);
        }
        return fluidProperties.apply(properties);
    }

    private static final class Registration {
        private DeferredHolder<FluidType, FluidType> type;
        private DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private DeferredBlock<LiquidBlock> block;
    }

    public record RegisteredFluid(
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
            Optional<DeferredBlock<LiquidBlock>> block
    ) {
    }

    public record ClientModelDefinition(
            Identifier stillTexture,
            Identifier flowingTexture,
            Optional<Identifier> overlayTexture,
            int tintColor,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing
    ) {
    }
}
