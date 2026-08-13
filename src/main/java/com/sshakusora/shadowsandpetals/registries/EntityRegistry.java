package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import com.sshakusora.shadowsandpetals.entity.SeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

public class EntityRegistry {
    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT = SAPRegistries
            .<SeatEntity>entity("seat", MobCategory.MISC)
            .factory(SeatEntity::new)
            .dimensions(0.01F, 0.01F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .summonable(false)
            .lang(DatagenLangRegistry.ZH_CN, "座位")
            .register();

    public static void init() {}
}
