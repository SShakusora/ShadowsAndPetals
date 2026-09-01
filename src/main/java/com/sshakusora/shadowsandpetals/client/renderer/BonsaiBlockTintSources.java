package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import com.sshakusora.shadowsandpetals.client.model.bonsai.BonsaiTreeGeometryCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
/** Tint sources used by chunk-rendered bonsai tree quads. */
public final class BonsaiBlockTintSources {
    public static final BlockTintSource TRUNK = new LayerTintSource(true);
    public static final BlockTintSource LEAVES = new LayerTintSource(false);

    private BonsaiBlockTintSources() {
    }

    private record LayerTintSource(boolean trunk) implements BlockTintSource {

        @Override
        public int color(BlockState state) {
            return 0xFFFFFFFF;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            BonsaiBlockEntity.RenderData data = level.getModelData(pos)
                    .get(BonsaiBlockEntity.RENDER_DATA);
            if (data == null || !data.planted()) {
                return 0xFFFFFFFF;
            }

            Identifier blockId = trunk ? data.trunkBlockId() : data.leavesBlockId();
            if (blockId == null || (!trunk && data.dead())) {
                return 0xFFFFFFFF;
            }
            Block block = BuiltInRegistries.BLOCK.getValue(blockId);
            if (block == Blocks.AIR) {
                return 0xFFFFFFFF;
            }

            BlockState targetState = block.defaultBlockState();
            int targetTintIndex = BonsaiTreeGeometryCache.getTargetTintIndex(blockId);
            if (targetTintIndex < 0) {
                return 0xFFFFFFFF;
            }
            BlockTintSource source = Minecraft.getInstance().getBlockColors()
                    .getTintSource(targetState, targetTintIndex);
            if (source == null) {
                return 0xFFFFFFFF;
            }
            int tint = source.colorInWorld(targetState, level, pos);
            return tint == -1 ? 0xFFFFFFFF : tint;
        }
    }
}
