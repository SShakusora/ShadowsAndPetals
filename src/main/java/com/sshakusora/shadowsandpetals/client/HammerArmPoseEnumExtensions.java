package com.sshakusora.shadowsandpetals.client;

import net.minecraft.client.model.HumanoidModel;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class HammerArmPoseEnumExtensions {
    public static final EnumProxy<HumanoidModel.ArmPose> SHADOWSANDPETALS_HAMMER_AND_CHISEL = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            false,
            true,
            (IArmPoseTransformer) HammerClientExtensions::applyThirdPersonHammerPose);

    private HammerArmPoseEnumExtensions() {}

    public static HumanoidModel.ArmPose getHammerAndChiselPose() {
        return SHADOWSANDPETALS_HAMMER_AND_CHISEL.getValue();
    }
}
