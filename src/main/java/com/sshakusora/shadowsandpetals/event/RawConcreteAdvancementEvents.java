package com.sshakusora.shadowsandpetals.event;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.TriggerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class RawConcreteAdvancementEvents {
    private static final Direction[][] SURFACE_AXES = {
            {Direction.EAST, Direction.SOUTH},
            {Direction.EAST, Direction.UP},
            {Direction.SOUTH, Direction.UP}
    };

    private RawConcreteAdvancementEvents() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getPlacedBlock().is(BlockRegistry.RAW_CONCRETE.get())) {
            return;
        }

        if (formsCompleteSurface(serverLevel, event.getPos())) {
            TriggerRegistry.RAW_CONCRETE_3X3_FORMED.get().trigger(player);
        }
    }

    private static boolean formsCompleteSurface(ServerLevel level, BlockPos placedPos) {
        for (Direction[] axes : SURFACE_AXES) {
            if (formsCompleteSurface(level, placedPos, axes[0], axes[1])) {
                return true;
            }
        }
        return false;
    }

    private static boolean formsCompleteSurface(
            ServerLevel level,
            BlockPos placedPos,
            Direction firstAxis,
            Direction secondAxis
    ) {
        boolean[][] rawConcrete = new boolean[5][5];
        BlockPos origin = placedPos
                .relative(firstAxis.getOpposite(), 2)
                .relative(secondAxis.getOpposite(), 2);

        for (int first = 0; first < 5; first++) {
            BlockPos firstPos = origin.relative(firstAxis, first);
            for (int second = 0; second < 5; second++) {
                BlockState state = level.getBlockState(firstPos.relative(secondAxis, second));
                rawConcrete[first][second] = state.is(BlockRegistry.RAW_CONCRETE.get());
            }
        }

        for (int firstOrigin = 0; firstOrigin <= 2; firstOrigin++) {
            for (int secondOrigin = 0; secondOrigin <= 2; secondOrigin++) {
                if (isComplete3x3(rawConcrete, firstOrigin, secondOrigin)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isComplete3x3(boolean[][] rawConcrete, int firstOrigin, int secondOrigin) {
        for (int first = firstOrigin; first < firstOrigin + 3; first++) {
            for (int second = secondOrigin; second < secondOrigin + 3; second++) {
                if (!rawConcrete[first][second]) {
                    return false;
                }
            }
        }
        return true;
    }
}
