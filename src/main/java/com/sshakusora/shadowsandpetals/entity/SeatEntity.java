package com.sshakusora.shadowsandpetals.entity;

import com.sshakusora.shadowsandpetals.block.decoration.CafeChairBlock;
import com.sshakusora.shadowsandpetals.registries.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class SeatEntity extends Entity {
    private static final double SEARCH_INFLATE = 0.2D;

    public SeatEntity(EntityType<? extends SeatEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Nullable
    public static SeatEntity getOrCreate(Level level, BlockPos pos, double seatHeight) {
        SeatEntity existingSeat = findSeat(level, pos);
        if (existingSeat != null) {
            return existingSeat;
        }

        SeatEntity seat = EntityRegistry.SEAT.get().create(level);
        if (seat == null) {
            return null;
        }

        seat.moveTo(pos.getX() + 0.5D, pos.getY() + seatHeight, pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(seat);
        return seat;
    }

    @Nullable
    public static SeatEntity findSeat(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(SeatEntity.class, new AABB(pos).inflate(SEARCH_INFLATE), seat -> seat.blockPosition().equals(pos))
                .stream()
                .findFirst()
                .orElse(null);
    }

    public boolean canBeSatOn() {
        return isAlive() && getPassengers().isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (!(level().getBlockState(blockPosition()).getBlock() instanceof CafeChairBlock)) {
            discard();
            return;
        }

        if (getPassengers().isEmpty()) {
            discard();
        }
    }

    @Override
    protected boolean canRide(Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }
}
