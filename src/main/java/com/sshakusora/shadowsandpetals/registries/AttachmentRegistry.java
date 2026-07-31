package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.world.excavation.SandExcavationCooldownData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class AttachmentRegistry {
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SandExcavationCooldownData>>
            SAND_EXCAVATION_COOLDOWNS =
            SAPRegistries.ATTACHMENT_TYPES.register(
                    "sand_excavation_cooldowns",
                    () -> AttachmentType.builder(SandExcavationCooldownData::new)
                            .serialize(SandExcavationCooldownData.CODEC, data -> !data.isEmpty())
                            .build()
            );

    private AttachmentRegistry() {
    }

    public static void init() {
    }
}
