package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WindChimeBlockEntity extends BlockEntity {
    private static final int IMPULSE_EVENT = 1;
    private static final int SOUND_COOLDOWN_TICKS = 40;
    private static final float BODY_SPRING = 0.055F;
    private static final float BODY_DAMPING = 0.92F;
    private static final float MAIN_SPRING = 0.08F;
    private static final float MAIN_DAMPING = 0.90F;
    private static final float BODY_SOFT_LIMIT = 25.0F;
    private static final float MAIN_SOFT_LIMIT = 35.0F;
    private static final float BODY_LIMIT_STRENGTH = 0.006F;
    private static final float MAIN_LIMIT_STRENGTH = 0.004F;
    private static final float BODY_MAX_VELOCITY = 7.0F;
    private static final float MAIN_MAX_VELOCITY = 10.0F;
    private static final float BODY_Y_DAMPING = 0.97F;
    private static final float MAIN_Y_DAMPING = 0.95F;
    private static final float INTERACTION_BLEND_IN = 0.35F;
    private static final float INTERACTION_BLEND_OUT = 0.06F;
    private static final float MAX_NATURAL_MOTION_SUPPRESSION = 0.75F;
    private static final float REST_ANGLE_EPSILON = 0.01F;
    private static final float REST_VELOCITY_EPSILON = 0.01F;
    private static final float REST_INFLUENCE_EPSILON = 0.001F;

    private float bodyX;
    private float bodyY;
    private float bodyZ;
    private float bodyXOld;
    private float bodyYOld;
    private float bodyZOld;
    private float bodyVelocityX;
    private float bodyVelocityY;
    private float bodyVelocityZ;
    private float mainX;
    private float mainY;
    private float mainZ;
    private float mainXOld;
    private float mainYOld;
    private float mainZOld;
    private float mainVelocityX;
    private float mainVelocityY;
    private float mainVelocityZ;
    private float interactionInfluence;
    private float interactionInfluenceOld;
    private boolean physicsActive;
    private long lastSoundGameTime = -SOUND_COOLDOWN_TICKS;
    private WindChimeColors colors = WindChimeColors.DEFAULT;

    public WindChimeBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.WIND_CHIME.get(), pos, blockState);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WindChimeBlockEntity blockEntity) {
        if (!blockEntity.physicsActive) {
            return;
        }
        blockEntity.savePreviousAngles();
        blockEntity.integratePhysics();
        blockEntity.trySleepPhysics();
    }

    public void broadcastInteractionImpulse(Vec3 hitLocation, Vec3 pushDirection) {
        if (level == null || level.isClientSide()) {
            return;
        }
        Vec3 impulse = calculateAngularImpulse(hitLocation, pushDirection.normalize(), 7.5D);
        level.blockEvent(worldPosition, getBlockState().getBlock(), IMPULSE_EVENT, packImpulse(impulse));
        long gameTime = level.getGameTime();
        if (gameTime - lastSoundGameTime >= SOUND_COOLDOWN_TICKS) {
            playSound(level, 0.8F, 0.95F, 1.05F);
            lastSoundGameTime = gameTime;
        }
    }

    public void playAmbientSound(Level level) {
        long gameTime = level.getGameTime();
        if (gameTime - lastSoundGameTime >= SOUND_COOLDOWN_TICKS) {
            playSound(level, 0.45F, 0.92F, 1.08F);
            lastSoundGameTime = gameTime;
        }
    }

    private void playSound(Level level, float volume, float minPitch, float maxPitch) {
        float pitch = minPitch + level.getRandom().nextFloat() * (maxPitch - minPitch);
        level.playSound(
                null, worldPosition, SoundRegistry.WIND_CHIME.get(), SoundSource.BLOCKS,
                volume, pitch
        );
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == IMPULSE_EVENT) {
            addAngularImpulse(unpackImpulse(type));
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public float getBodyX(float partialTick) {
        return Mth.lerp(partialTick, bodyXOld, bodyX);
    }

    public float getBodyY(float partialTick) {
        return Mth.lerp(partialTick, bodyYOld, bodyY);
    }

    public float getBodyZ(float partialTick) {
        return Mth.lerp(partialTick, bodyZOld, bodyZ);
    }

    public float getMainX(float partialTick) {
        return Mth.lerp(partialTick, mainXOld, mainX);
    }

    public float getMainY(float partialTick) {
        return Mth.lerp(partialTick, mainYOld, mainY);
    }

    public float getMainZ(float partialTick) {
        return Mth.lerp(partialTick, mainZOld, mainZ);
    }

    public float getNaturalMotionWeight(float partialTick) {
        float influence = Mth.lerp(partialTick, interactionInfluenceOld, interactionInfluence);
        float smoothInfluence = influence * influence * (3.0F - 2.0F * influence);
        return 1.0F - smoothInfluence * MAX_NATURAL_MOTION_SUPPRESSION;
    }

    public WindChimeColors getColors() {
        return colors;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        CustomData data = components.get(DataComponents.CUSTOM_DATA);
        colors = data == null ? WindChimeColors.DEFAULT : WindChimeColors.fromTag(data.copyTag());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        CompoundTag tag = new CompoundTag();
        colors.saveToTag(tag);
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        components.set(DataComponents.ITEM_MODEL, colors.itemModelId());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(WindChimeColors.RIBBON_TAG, colors.ribbon().getName());
        output.putString(WindChimeColors.VANE_TAG, colors.vane().getName());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        colors = new WindChimeColors(
                input.getString(WindChimeColors.RIBBON_TAG)
                        .map(name -> DyeColor.byName(name, WindChimeColors.DEFAULT_COLOR))
                        .orElse(WindChimeColors.DEFAULT_COLOR),
                input.getString(WindChimeColors.VANE_TAG)
                        .map(name -> DyeColor.byName(name, WindChimeColors.DEFAULT_COLOR))
                        .orElse(WindChimeColors.DEFAULT_COLOR)
        );
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    private Vec3 calculateAngularImpulse(Vec3 contact, Vec3 force, double strength) {
        Vec3 pivot = Vec3.atLowerCornerOf(worldPosition).add(0.5D, 1.0D, 0.5D);
        Vec3 lever = contact.subtract(pivot);
        Vec3 torque = lever.cross(force).scale(strength);
        return new Vec3(
                Mth.clamp(torque.x, -6.0D, 6.0D),
                Mth.clamp(torque.y * 1.35D, -6.0D, 6.0D),
                Mth.clamp(torque.z, -6.0D, 6.0D)
        );
    }

    private void addAngularImpulse(Vec3 impulse) {
        physicsActive = true;
        bodyVelocityX = Mth.clamp(
                bodyVelocityX + (float) impulse.x, -BODY_MAX_VELOCITY, BODY_MAX_VELOCITY);
        bodyVelocityY = Mth.clamp(
                bodyVelocityY + (float) impulse.y, -BODY_MAX_VELOCITY, BODY_MAX_VELOCITY);
        bodyVelocityZ = Mth.clamp(
                bodyVelocityZ + (float) impulse.z, -BODY_MAX_VELOCITY, BODY_MAX_VELOCITY);
        mainVelocityX = Mth.clamp(
                mainVelocityX + (float) impulse.x * 1.45F, -MAIN_MAX_VELOCITY, MAIN_MAX_VELOCITY);
        mainVelocityY = Mth.clamp(
                mainVelocityY + (float) impulse.y * 1.7F, -MAIN_MAX_VELOCITY, MAIN_MAX_VELOCITY);
        mainVelocityZ = Mth.clamp(
                mainVelocityZ + (float) impulse.z * 1.45F, -MAIN_MAX_VELOCITY, MAIN_MAX_VELOCITY);
        float impulseInfluence = Mth.clamp((float) impulse.length() / 6.0F, 0.0F, 1.0F);
        interactionInfluence = Math.max(interactionInfluence, impulseInfluence);
    }

    private void savePreviousAngles() {
        bodyXOld = bodyX;
        bodyYOld = bodyY;
        bodyZOld = bodyZ;
        mainXOld = mainX;
        mainYOld = mainY;
        mainZOld = mainZ;
        interactionInfluenceOld = interactionInfluence;
    }

    private void integratePhysics() {
        bodyVelocityX = integrateSwingVelocity(
                bodyVelocityX, bodyX, BODY_SPRING, BODY_DAMPING,
                BODY_SOFT_LIMIT, BODY_LIMIT_STRENGTH, BODY_MAX_VELOCITY);
        bodyVelocityY = integrateSwingVelocity(
                bodyVelocityY, bodyY, BODY_SPRING * 0.5F, BODY_Y_DAMPING,
                BODY_SOFT_LIMIT, BODY_LIMIT_STRENGTH, BODY_MAX_VELOCITY);
        bodyVelocityZ = integrateSwingVelocity(
                bodyVelocityZ, bodyZ, BODY_SPRING, BODY_DAMPING,
                BODY_SOFT_LIMIT, BODY_LIMIT_STRENGTH, BODY_MAX_VELOCITY);
        bodyX += bodyVelocityX;
        bodyY += bodyVelocityY;
        bodyZ += bodyVelocityZ;

        mainVelocityX = integrateSwingVelocity(
                mainVelocityX, mainX, MAIN_SPRING, MAIN_DAMPING,
                MAIN_SOFT_LIMIT, MAIN_LIMIT_STRENGTH, MAIN_MAX_VELOCITY);
        mainVelocityY = integrateSwingVelocity(
                mainVelocityY, mainY, MAIN_SPRING * 0.5F, MAIN_Y_DAMPING,
                MAIN_SOFT_LIMIT, MAIN_LIMIT_STRENGTH, MAIN_MAX_VELOCITY);
        mainVelocityZ = integrateSwingVelocity(
                mainVelocityZ, mainZ, MAIN_SPRING, MAIN_DAMPING,
                MAIN_SOFT_LIMIT, MAIN_LIMIT_STRENGTH, MAIN_MAX_VELOCITY);
        mainX += mainVelocityX;
        mainY += mainVelocityY;
        mainZ += mainVelocityZ;

        float targetInfluence = calculateInteractionInfluence();
        float blendSpeed = targetInfluence > interactionInfluence
                ? INTERACTION_BLEND_IN
                : INTERACTION_BLEND_OUT;
        interactionInfluence = Mth.lerp(blendSpeed, interactionInfluence, targetInfluence);
        if (interactionInfluence < 0.001F && targetInfluence == 0.0F) {
            interactionInfluence = 0.0F;
        }
    }

    private void trySleepPhysics() {
        if (!isNearZero(bodyX, REST_ANGLE_EPSILON)
                || !isNearZero(bodyY, REST_ANGLE_EPSILON)
                || !isNearZero(bodyZ, REST_ANGLE_EPSILON)
                || !isNearZero(mainX, REST_ANGLE_EPSILON)
                || !isNearZero(mainY, REST_ANGLE_EPSILON)
                || !isNearZero(mainZ, REST_ANGLE_EPSILON)
                || !isNearZero(bodyVelocityX, REST_VELOCITY_EPSILON)
                || !isNearZero(bodyVelocityY, REST_VELOCITY_EPSILON)
                || !isNearZero(bodyVelocityZ, REST_VELOCITY_EPSILON)
                || !isNearZero(mainVelocityX, REST_VELOCITY_EPSILON)
                || !isNearZero(mainVelocityY, REST_VELOCITY_EPSILON)
                || !isNearZero(mainVelocityZ, REST_VELOCITY_EPSILON)
                || interactionInfluence >= REST_INFLUENCE_EPSILON) {
            return;
        }

        bodyX = bodyY = bodyZ = 0.0F;
        bodyXOld = bodyYOld = bodyZOld = 0.0F;
        bodyVelocityX = bodyVelocityY = bodyVelocityZ = 0.0F;
        mainX = mainY = mainZ = 0.0F;
        mainXOld = mainYOld = mainZOld = 0.0F;
        mainVelocityX = mainVelocityY = mainVelocityZ = 0.0F;
        interactionInfluence = interactionInfluenceOld = 0.0F;
        physicsActive = false;
    }

    private static boolean isNearZero(float value, float epsilon) {
        return Math.abs(value) < epsilon;
    }

    private float calculateInteractionInfluence() {
        float bodyMotion = Math.max(
                Math.max(Math.abs(bodyX), Math.abs(bodyZ)) / BODY_SOFT_LIMIT,
                Math.max(Math.abs(bodyVelocityX), Math.abs(bodyVelocityZ)) / BODY_MAX_VELOCITY
        );
        float mainMotion = Math.max(
                Math.max(Math.abs(mainX), Math.abs(mainZ)) / MAIN_SOFT_LIMIT,
                Math.max(Math.abs(mainVelocityX), Math.abs(mainVelocityZ)) / MAIN_MAX_VELOCITY
        );
        return Mth.clamp(Math.max(bodyMotion, mainMotion), 0.0F, 1.0F);
    }

    private static float integrateSwingVelocity(
            float velocity, float angle, float spring, float damping,
            float softLimit, float limitStrength, float maxVelocity
    ) {
        float excess = Math.max(Math.abs(angle) - softLimit, 0.0F);
        float limitForce = Math.copySign(excess * excess * limitStrength, angle);
        return Mth.clamp(
                (velocity - angle * spring - limitForce) * damping,
                -maxVelocity, maxVelocity
        );
    }

    private static int packImpulse(Vec3 impulse) {
        return packComponent(impulse.x)
                | packComponent(impulse.y) << 10
                | packComponent(impulse.z) << 20;
    }

    private static Vec3 unpackImpulse(int packed) {
        return new Vec3(
                unpackComponent(packed),
                unpackComponent(packed >> 10),
                unpackComponent(packed >> 20)
        );
    }

    private static int packComponent(double value) {
        return Mth.clamp((int) Math.round(value * 64.0D), -511, 511) & 0x3FF;
    }

    private static double unpackComponent(int packed) {
        int value = packed & 0x3FF;
        if (value >= 512) {
            value -= 1024;
        }
        return value / 64.0D;
    }
}
