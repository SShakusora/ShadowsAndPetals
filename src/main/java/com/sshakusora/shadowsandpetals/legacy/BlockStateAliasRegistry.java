package com.sshakusora.shadowsandpetals.legacy;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class BlockStateAliasRegistry {
    private static final List<Rule> RULES = new ArrayList<>();

    private BlockStateAliasRegistry() {}

    public static void add(
            Supplier<? extends Block> legacyBlock,
            Supplier<BlockState> targetState,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {
        RULES.add(new Rule(legacyBlock, targetState, converter));
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || chunk.getLevel().isClientSide() || RULES.isEmpty()) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;

                for (int y = minY; y < maxY; y++) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = chunk.getBlockState(pos);
                    BlockState replacement = getReplacement(state);
                    if (replacement != null && replacement != state) {
                        chunk.setBlockState(pos, replacement, false);
                    }
                }
            }
        }
    }

    @Nullable
    private static BlockState getReplacement(BlockState state) {
        for (Rule rule : RULES) {
            if (!state.is(rule.legacyBlock.get())) {
                continue;
            }

            BlockState replacement = rule.converter.apply(state, rule.targetState.get());
            if (replacement != null) {
                return replacement;
            }
        }

        return null;
    }

    private record Rule(
            Supplier<? extends Block> legacyBlock,
            Supplier<BlockState> targetState,
            BiFunction<BlockState, BlockState, BlockState> converter
    ) {}
}
