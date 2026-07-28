package com.sshakusora.shadowsandpetals.client.interaction;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.client.ClientPickEvent;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID, value = Dist.CLIENT)
public final class IroriGrillTargeting {
    private static final BlockPos[] CANDIDATE_OFFSETS = {
            BlockPos.ZERO,
            new BlockPos(0, -1, 0)
    };

    private IroriGrillTargeting() {
    }

    @SubscribeEvent
    public static void onClientPick(ClientPickEvent event) {
        HitResult current = event.getHitResult();
        HitResult corrected = AdjacentShapeTargeting.correct(
                current,
                event.getCameraEntity(),
                event.getPartialTick(),
                CANDIDATE_OFFSETS,
                IroriGrillTargeting::clipGrillAt
        );
        if (corrected != current) {
            event.setHitResult(corrected);
        }
    }

    private static @Nullable BlockHitResult clipGrillAt(
            Level level,
            BlockPos iroriPos,
            Entity cameraEntity,
            Vec3 from,
            Vec3 to
    ) {
        BlockState state = level.getBlockState(iroriPos);
        if (!(state.getBlock() instanceof IroriBlock) || !state.getValue(IroriBlock.HAS_GRILL)) {
            return null;
        }

        BlockPos immutablePos = iroriPos.immutable();
        return state.getShape(level, immutablePos, CollisionContext.of(cameraEntity))
                .clip(from, to, immutablePos);
    }
}
