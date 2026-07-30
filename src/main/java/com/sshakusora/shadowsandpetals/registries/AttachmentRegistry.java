package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.world.clam.ClamHarvestData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class AttachmentRegistry {
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ClamHarvestData>> CLAM_HARVEST =
            SAPRegistries.ATTACHMENT_TYPES.register(
                    "clam_harvest",
                    () -> AttachmentType.builder(ClamHarvestData::new)
                            .serialize(ClamHarvestData.CODEC, data -> !data.isEmpty())
                            .build()
            );

    private AttachmentRegistry() {
    }

    public static void init() {
    }
}
