package com.sshakusora.shadowsandpetals.entity;

import com.sshakusora.shadowsandpetals.block.decoration.AbstractSeatBlock;
import com.sshakusora.shadowsandpetals.registries.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

        SeatEntity seat = EntityRegistry.SEAT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (seat == null) {
            return null;
        }

        seat.snapTo(pos.getX() + 0.5D, pos.getY() + seatHeight, pos.getZ() + 0.5D, 0.0F, 0.0F);
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
        if (level().isClientSide()) {
            return;
        }

        if (!(level().getBlockState(blockPosition()).getBlock() instanceof AbstractSeatBlock)) {
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
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction direction = passenger.getDirection();
        if (direction.getAxis() == Direction.Axis.Y) {
            direction = Direction.NORTH;
        }

        int[][] offsets = DismountHelper.offsetsForDirection(direction);
        BlockPos blockPos = blockPosition();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for (Pose pose : passenger.getDismountPoses()) {
            AABB bounds = passenger.getLocalBoundsForPose(pose);

            for (int[] offset : offsets) {
                mutableBlockPos.set(blockPos.getX() + offset[0], blockPos.getY(), blockPos.getZ() + offset[1]);
                double floorHeight = level().getBlockFloorHeight(mutableBlockPos);
                if (!DismountHelper.isBlockFloorValid(floorHeight)) {
                    continue;
                }

                Vec3 dismountLocation = Vec3.upFromBottomCenterOf(mutableBlockPos, floorHeight);
                if (DismountHelper.canDismountTo(level(), passenger, bounds.move(dismountLocation))) {
                    passenger.setPose(pose);
                    return dismountLocation;
                }
            }
        }

        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }
}
