package com.sshakusora.shadowsandpetals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.sshakusora.shadowsandpetals.block.decoration.WoodPostBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LanternBlock.class)
public class LanternBlockMixin {
    @ModifyReturnValue(method = "canSurvive", at = @At("RETURN"))
    private boolean shadowsandpetals$allowPostSupport(boolean original, BlockState state, LevelReader level, BlockPos pos) {
        return WoodPostBlock.canSupportHanging(state, level, pos, original);
    }
}
