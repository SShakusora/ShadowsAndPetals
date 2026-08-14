package com.sshakusora.shadowsandpetals.block.decoration;

import com.sshakusora.shadowsandpetals.registries.TriggerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

final class LampToggle {
    static InteractionResult toggle(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BooleanProperty litProperty
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean lit = !state.getValue(litProperty);
        if (!level.setBlock(pos, state.setValue(litProperty, lit), Block.UPDATE_ALL)) {
            return InteractionResult.SUCCESS;
        }

        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, lit ? 0.6F : 0.5F);
        if (lit && player instanceof ServerPlayer serverPlayer) {
            TriggerRegistry.LAMP_LIT.get().trigger(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private LampToggle() {
    }
}
