package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.advancement.RegisteredAdvancement;
import com.sshakusora.shadowsandpetals.block.WoodBlockList;
import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.RecipeCraftedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/** Gameplay advancements declared through the fluent registration builder. */
public final class AdvancementRegistry {
    private static final ResourceKey<Recipe<?>> WIND_CHIME_RECIPE = ResourceKey.create(
            Registries.RECIPE,
            ShadowsAndPetals.asResource("wind_chime")
    );

    public static final RegisteredAdvancement ROOT = SAPRegistries
            .advancement("root")
            .icon(BlockRegistry.MAPLE_SET.sapling())
            .background(ShadowsAndPetals.asResource("block/maple_planks"))
            .criterion("mod_item", registries -> InventoryChangeTrigger.TriggerInstance.hasItems(
                    ItemPredicate.Builder.item().of(
                            registries.lookupOrThrow(Registries.ITEM),
                            ItemTagRegistry.MOD_ITEMS
                    )
            ))
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "“Shadows and Petals: A Collection of Verse”",
                    "Discover the world of Shadows And Petals."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "『织影落花集』",
                    "接触织影落花的世界。"
            )
            .register();

    public static final RegisteredAdvancement WIND_CHIME = SAPRegistries
            .advancement("crafting/wind_chime")
            .parent(ROOT)
            .icon(BlockRegistry.WIND_CHIME)
            .criterion(
                    "crafted_wind_chime",
                    () -> RecipeCraftedTrigger.TriggerInstance.craftedItem(WIND_CHIME_RECIPE)
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "Chance bell ’neath the eaves, Sweet chimes bless the quiet scene, Ask yon winds on high",
                    "Craft a wind chime for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "无心檐下铃，闲声入耳伴惬景，何用问罡风",
                    "第一次制作出风铃。"
            )
            .register();

    public static final RegisteredAdvancement RAW_CONCRETE_3X3_FORMED = SAPRegistries
            .advancement("building/raw_concrete_3x3_formed")
            .parent(ROOT)
            .icon(BlockRegistry.RAW_CONCRETE)
            .criterion(
                    "formed_raw_concrete_surface",
                    () -> TriggerRegistry.RAW_CONCRETE_3X3_FORMED.get().rawConcrete3x3Formed()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "Silver feigned the moon, Bright stone now reveals its form, Lo that ancient light",
                    "Complete a 3x3 surface of raw concrete for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "银块古为月，清水微明形始现，原是千里烛",
                    "第一次用清水混凝土组成完整的 3×3 整体。"
            )
            .register();

    public static final RegisteredAdvancement IRORI_2X2_FORMED = SAPRegistries
            .advancement("building/irori_2x2_formed")
            .parent(ROOT)
            .icon(BlockRegistry.IRORI)
            .criterion(
                    "formed_irori_2x2",
                    () -> TriggerRegistry.IRORI_2X2_FORMED.get().irori2x2Formed()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "Lone hearth glimmers faint, Kindred flames meet as old friends, Joined we heed no cold",
                    "Complete a 2x2 Irori for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "孤炉火微明，相逢一时为旧客，归一不觉寒",
                    "第一次使用日式围炉组成完整的 2×2 大围炉。"
            )
            .register();

    public static final RegisteredAdvancement ROCKERY_CARVED = SAPRegistries
            .advancement("crafting/rockery_carved")
            .parent(ROOT)
            .icon(BlockRegistry.ROCKERY_1x1x1)
            .criterion(
                    "carved_rockery",
                    () -> TriggerRegistry.ROCKERY_CARVED.get().rockeryCarved()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "Far peaks flee mine eyes, Chisels sing through wind and rain, I rise a lone mount",
                    "Carve any rockery with a chisel and hammer for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "山远目无极，凿声入石风雨过，我亦为孤峰",
                    "第一次使用凿子与锤子雕刻出任意石山。"
            )
            .register();

    public static final RegisteredAdvancement LAMP_LIT = SAPRegistries
            .advancement("decorations/lamp_lit")
            .parent(ROOT)
            .icon(BlockRegistry.BEDROOM_LAMP)
            .criterion(
                    "lit_lamp",
                    () -> TriggerRegistry.LAMP_LIT.get().lampLit()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "First lamp shines on me, Thy bright and dim ways unknown, Hast thou final dusk?",
                    "Turn on any lamp for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "一灯初照我，此光幽明未熟知，君有暮时否",
                    "第一次打开任意灯具。"
            )
            .register();

    public static final RegisteredAdvancement VANITY_DRAWER_OPENED = SAPRegistries
            .advancement("decorations/vanity_drawer_opened")
            .parent(ROOT)
            .icon(BlockRegistry.VANITIES.get(WoodBlockList.WoodType.MAPLE))
            .criterion(
                    "opened_vanity_drawer",
                    () -> TriggerRegistry.VANITY_DRAWER_OPENED.get().vanityDrawerOpened()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "First I ope the glass, Mine image seems an old friend, When knew I this face?",
                    "Open a vanity drawer for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "初开妆台柜，镜影映身似旧客，我何曾识人",
                    "第一次打开梳妆台的抽屉。"
            )
            .register();

    public static final RegisteredAdvancement SEAFOOD_EXCAVATED = SAPRegistries
            .advancement("nature/seafood_excavated")
            .parent(ROOT)
            .icon(ItemRegistry.CLAM)
            .criterion(
                    "excavated_seafood",
                    () -> TriggerRegistry.SEAFOOD_EXCAVATED.get().seafoodExcavated()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "Idly I rake sand, A clam wakes freedom fading, My grace its sorrow",
                    "Excavate seafood with a harrow for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "闲耙水边沙，蛤蜊渐露失独身，我幸彼何哀",
                    "第一次使用耙子挖出海产。"
            )
            .register();

    public static final RegisteredAdvancement GRAVEL_EXCAVATED = SAPRegistries
            .advancement("nature/gravel_excavated")
            .parent(ROOT)
            .icon(ItemRegistry.HARROW)
            .criterion(
                    "excavated_gravel",
                    () -> TriggerRegistry.GRAVEL_EXCAVATED.get().gravelExcavated()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "Idle sands lie still, Raked they dream of sea and mount, Now their meaning wakes",
                    "Use a harrow to excavate gravel for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "碎砂本无用，耙过成海又是山，安知其为何",
                    "第一次使用耙子挖掘沙砾。"
            )
            .register();

    public static final RegisteredAdvancement SHISHI_ODOSHI_FLUID_POURED = SAPRegistries
            .advancement("nature/shishi_odoshi_fluid_poured")
            .parent(ROOT)
            .icon(BlockRegistry.SHISHI_ODOSHI)
            .criterion(
                    "fluid_poured",
                    () -> TriggerRegistry.SHISHI_ODOSHI_FLUID_POURED.get().fluidPoured()
            )
            .lang(
                    DatagenLangRegistry.DEFAULT_LOCALE,
                    "I know yon timeless realm, No new sound shall stir the stars, And yet… and yet…",
                    "Make a shishi-odoshi pour fluid for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "我知这世间，再无新声惊日月，然而，然而",
                    "第一次让添水成功流出流体。"
            )
            .register();

    private AdvancementRegistry() {
    }

    public static void init() {
    }
}
