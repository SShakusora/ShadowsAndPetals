package com.sshakusora.shadowsandpetals.compat.jade;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.world.excavation.SandExcavationCooldownData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public final class SandExcavationCooldownServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final SandExcavationCooldownServerDataProvider INSTANCE =
            new SandExcavationCooldownServerDataProvider();
    static final String COOLDOWN_END_TICK_KEY = "SandExcavationCooldownEndTick";
    static final String COOLDOWN_DURATION_KEY = "SandExcavationCooldownDuration";

    private SandExcavationCooldownServerDataProvider() {
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getLevel() instanceof ServerLevel level) || !accessor.getBlockState().is(Blocks.SAND)) {
            return;
        }

        SandExcavationCooldownData.Cooldown cooldown = SandExcavationCooldownData.getCooldown(
                level,
                accessor.getPosition()
        );
        if (cooldown.endTick() > level.getGameTime() && cooldown.durationTicks() > 0L) {
            data.putLong(COOLDOWN_END_TICK_KEY, cooldown.endTick());
            data.putLong(COOLDOWN_DURATION_KEY, cooldown.durationTicks());
        }
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return accessor.getBlockState().is(Blocks.SAND);
    }

    @Override
    public Identifier getUid() {
        return ShadowsAndPetals.asResource("jade.sand_excavation_cooldown");
    }
}
