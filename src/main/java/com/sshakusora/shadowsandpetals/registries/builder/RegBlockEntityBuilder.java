package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.compat.BlockEntityAliasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class RegBlockEntityBuilder<T extends BlockEntity> {
    private final DeferredRegister<BlockEntityType<?>> registry;
    private final String name;
    private BiFunction<BlockPos, BlockState, T> factory;
    private final List<Supplier<? extends Block>> validBlocks = new ArrayList<>();
    private final List<ResourceLocation> aliases = new ArrayList<>();
    private final List<BlockEntityAliasSpec> dataAliases = new ArrayList<>();

    public RegBlockEntityBuilder(DeferredRegister<BlockEntityType<?>> registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    public RegBlockEntityBuilder<T> factory(BiFunction<BlockPos, BlockState, T> factory) {
        this.factory = factory;
        return this;
    }

    @SafeVarargs
    public final RegBlockEntityBuilder<T> validBlocks(Supplier<? extends Block>... blocks) {
        this.validBlocks.addAll(Arrays.asList(blocks));
        return this;
    }

    public RegBlockEntityBuilder<T> alias(String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
        return this;
    }

    public RegBlockEntityBuilder<T> alias(String oldNamespace, String oldPath) {
        this.aliases.add(ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    @SafeVarargs
    public final RegBlockEntityBuilder<T> dataAlias(
            String oldNamespace,
            String oldPath,
            BlockEntityAliasRegistry.LegacyDataConverter converter,
            Supplier<? extends Block>... legacyBlocks
    ) {
        this.dataAliases.add(new BlockEntityAliasSpec(
                ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath),
                List.of(legacyBlocks),
                converter
        ));
        return this;
    }

    public DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register() {
        if (factory == null) {
            throw new IllegalStateException("Block entity factory is required for '" + name + "'");
        }
        if (validBlocks.isEmpty()) {
            throw new IllegalStateException("At least one valid block is required for '" + name + "'");
        }

        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> type = registry.register(name, key ->
                BlockEntityType.Builder.of(factory::apply, validBlocks.stream().map(Supplier::get).toArray(Block[]::new)).build(null)
        );

        for (ResourceLocation alias : aliases) {
            registry.addAlias(alias, type.getId());
        }

        for (int i = 0; i < dataAliases.size(); i++) {
            BlockEntityAliasSpec alias = dataAliases.get(i);
            BlockEntityAliasRegistry.builder(registry, buildCompatAliasName(alias.aliasId(), i))
                    .alias(alias.aliasId().getNamespace(), alias.aliasId().getPath())
                    .validBlocks(alias.legacyBlocks().toArray(Supplier[]::new))
                    .target(type::get)
                    .convert(alias.converter())
                    .register();
        }

        return type;
    }

    private String buildCompatAliasName(ResourceLocation aliasId, int index) {
        return name + "__compat_be_alias_" + index + "__" + aliasId.getNamespace().replace(':', '_').replace('/', '_')
                + "__" + aliasId.getPath().replace('/', '_');
    }

    private record BlockEntityAliasSpec(
            ResourceLocation aliasId,
            List<Supplier<? extends Block>> legacyBlocks,
            BlockEntityAliasRegistry.LegacyDataConverter converter
    ) {}
}
