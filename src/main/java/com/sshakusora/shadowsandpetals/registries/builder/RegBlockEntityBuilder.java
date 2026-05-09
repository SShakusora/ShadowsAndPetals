package com.sshakusora.shadowsandpetals.registries.builder;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.legacy.BlockEntityAliasRegistry;
import com.sshakusora.shadowsandpetals.legacy.LegacyCompatIds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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

/**
 * Fluent builder for {@link BlockEntityType} registration.
 * <p>
 * Besides normal block entity registration, this builder can declare registry aliases and
 * migration aliases that deserialize legacy block entity ids into temporary compatibility
 * instances before converting their NBT into the new block entity format.
 *
 * @param <T> registered block entity type
 */
public class RegBlockEntityBuilder<T extends BlockEntity> {
    private final DeferredRegister<BlockEntityType<?>> registry;
    private final String name;
    private BiFunction<BlockPos, BlockState, T> factory;
    private final List<Supplier<? extends Block>> validBlocks = new ArrayList<>();
    private final List<Identifier> aliases = new ArrayList<>();
    private final List<BlockEntityAliasSpec> dataAliases = new ArrayList<>();

    public RegBlockEntityBuilder(DeferredRegister<BlockEntityType<?>> registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    /**
     * Sets the constructor used by the resulting {@link BlockEntityType}.
     */
    public RegBlockEntityBuilder<T> factory(BiFunction<BlockPos, BlockState, T> factory) {
        this.factory = factory;
        return this;
    }

    /**
     * Declares the blocks this block entity type is valid for.
     */
    @SafeVarargs
    public final RegBlockEntityBuilder<T> validBlocks(Supplier<? extends Block>... blocks) {
        this.validBlocks.addAll(Arrays.asList(blocks));
        return this;
    }

    /**
     * Adds a same-namespace registry alias for the block entity type id.
     */
    public RegBlockEntityBuilder<T> alias(String oldPath) {
        this.aliases.add(Identifier.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath));
        return this;
    }

    /**
     * Adds a cross-namespace registry alias for the block entity type id.
     */
    public RegBlockEntityBuilder<T> alias(String oldNamespace, String oldPath) {
        this.aliases.add(Identifier.fromNamespaceAndPath(oldNamespace, oldPath));
        return this;
    }

    /**
     * Declares a legacy block entity id whose NBT should be migrated into this block entity type.
     * <p>
     * The provided legacy blocks must match the compatibility blocks that can host the old block
     * entity while the migration runs.
     */
    @SafeVarargs
    public final RegBlockEntityBuilder<T> dataAlias(
            String oldNamespace,
            String oldPath,
            BlockEntityAliasRegistry.LegacyDataConverter converter,
            Supplier<? extends Block>... legacyBlocks
    ) {
        this.dataAliases.add(new BlockEntityAliasSpec(
                Identifier.fromNamespaceAndPath(oldNamespace, oldPath),
                List.of(legacyBlocks),
                converter
        ));
        return this;
    }

    /**
     * Finalizes the block entity type registration and all declared compatibility aliases.
     */
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

        for (Identifier alias : aliases) {
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

    private String buildCompatAliasName(Identifier aliasId, int index) {
        return LegacyCompatIds.blockEntityName(name, aliasId, index);
    }

    private record BlockEntityAliasSpec(
            Identifier aliasId,
            List<Supplier<? extends Block>> legacyBlocks,
            BlockEntityAliasRegistry.LegacyDataConverter converter
    ) {}
}
