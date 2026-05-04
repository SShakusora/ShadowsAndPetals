package com.sshakusora.shadowsandpetals.legacy;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class BlockEntityAliasRegistry {
    private static final List<Rule> RULES = new ArrayList<>();

    private BlockEntityAliasRegistry() {}

    public static Builder builder(DeferredRegister<BlockEntityType<?>> registry, String name) {
        return new Builder(registry, name);
    }

    private static void add(
            Supplier<BlockEntityType<LegacyBlockEntity>> legacyType,
            Supplier<BlockEntityType<?>> targetType,
            LegacyDataConverter converter
    ) {
        RULES.add(new Rule(legacyType, targetType, converter));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || chunk.getLevel().isClientSide() || RULES.isEmpty()) {
            return;
        }

        List<BlockEntity> blockEntities = List.copyOf(chunk.getBlockEntities().values());
        for (BlockEntity blockEntity : blockEntities) {
            for (Rule rule : RULES) {
                if (blockEntity.getType() == rule.legacyType.get() && blockEntity instanceof LegacyBlockEntity legacyBlockEntity) {
                    migrate(chunk, legacyBlockEntity, rule);
                    break;
                }
            }
        }
    }

    private static void migrate(LevelChunk chunk, LegacyBlockEntity legacyBlockEntity, Rule rule) {
        BlockPos pos = legacyBlockEntity.getBlockPos();
        BlockState state = chunk.getBlockState(pos);
        chunk.removeBlockEntity(pos);

        CompoundTag migratedTag = rule.converter.convert(legacyBlockEntity.getRawData(), state, pos);
        if (migratedTag == null || state.isAir() || !state.hasBlockEntity() || !rule.targetType.get().isValid(state)) {
            chunk.getLevel().blockEntityChanged(pos);
            return;
        }

        populateMetadata(migratedTag, pos, rule.targetType.get());
        BlockEntity migratedBlockEntity = BlockEntity.loadStatic(pos, state, migratedTag, chunk.getLevel().registryAccess());
        if (migratedBlockEntity != null) {
            chunk.setBlockEntity(migratedBlockEntity);
            migratedBlockEntity.setChanged();
        } else {
            chunk.getLevel().blockEntityChanged(pos);
        }
    }

    private static void populateMetadata(CompoundTag tag, BlockPos pos, BlockEntityType<?> type) {
        ResourceLocation typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        tag.putString("id", typeId.toString());
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
    }

    public interface LegacyDataConverter {
        CompoundTag convert(CompoundTag oldTag, BlockState state, BlockPos pos);
    }

    public static final class Builder {
        private final DeferredRegister<BlockEntityType<?>> registry;
        private final String name;
        private ResourceLocation aliasId;
        private final List<Supplier<? extends Block>> validBlocks = new ArrayList<>();
        private Supplier<BlockEntityType<?>> targetType;
        private LegacyDataConverter converter = (oldTag, state, pos) -> oldTag;

        private Builder(DeferredRegister<BlockEntityType<?>> registry, String name) {
            this.registry = registry;
            this.name = name;
        }

        public Builder alias(String oldPath) {
            this.aliasId = ResourceLocation.fromNamespaceAndPath(ShadowsAndPetals.MOD_ID, oldPath);
            return this;
        }

        public Builder alias(String oldNamespace, String oldPath) {
            this.aliasId = ResourceLocation.fromNamespaceAndPath(oldNamespace, oldPath);
            return this;
        }

        @SafeVarargs
        public final Builder validBlocks(Supplier<? extends Block>... blocks) {
            this.validBlocks.addAll(Arrays.asList(blocks));
            return this;
        }

        public Builder target(Supplier<BlockEntityType<?>> targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder convert(LegacyDataConverter converter) {
            this.converter = converter;
            return this;
        }

        public DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyBlockEntity>> register() {
            if (aliasId == null) {
                throw new IllegalStateException("Block entity alias id is required for '" + name + "'");
            }
            if (targetType == null) {
                throw new IllegalStateException("Target block entity type is required for '" + name + "'");
            }
            if (validBlocks.isEmpty()) {
                throw new IllegalStateException("At least one valid legacy block is required for '" + name + "'");
            }

            @SuppressWarnings("unchecked")
            final DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyBlockEntity>>[] legacyTypeRef = new DeferredHolder[1];

            DeferredHolder<BlockEntityType<?>, BlockEntityType<LegacyBlockEntity>> legacyType = registry.register(name, key ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new LegacyBlockEntity(() -> legacyTypeRef[0].get(), pos, state),
                            validBlocks.stream().map(Supplier::get).toArray(Block[]::new)
                    ).build(null)
            );
            legacyTypeRef[0] = legacyType;

            registry.addAlias(aliasId, legacyType.getId());
            BlockEntityAliasRegistry.add(legacyType, targetType, converter);
            return legacyType;
        }
    }

    private record Rule(
            Supplier<BlockEntityType<LegacyBlockEntity>> legacyType,
            Supplier<BlockEntityType<?>> targetType,
            LegacyDataConverter converter
    ) {}
}
