package com.sshakusora.shadowsandpetals.item.harrow;

import net.minecraft.client.model.HumanoidModel;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class HarrowArmPoseEnumExtensions {
    public static final EnumProxy<HumanoidModel.ArmPose> SHADOWSANDPETALS_HARROW_DIGGING = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            false,
            false,
            (IArmPoseTransformer) HarrowClientExtensions::applyThirdPersonHarrowPose
    );

    private HarrowArmPoseEnumExtensions() {
    }

    public static HumanoidModel.ArmPose getHarrowDiggingPose() {
        return SHADOWSANDPETALS_HARROW_DIGGING.getValue();
    }
}
