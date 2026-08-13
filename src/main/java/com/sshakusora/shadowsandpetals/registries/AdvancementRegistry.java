package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.advancement.RegisteredAdvancement;
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
                    "Shadows And Petals",
                    "Encounter an item from Shadows And Petals for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "织影落花",
                    "第一次接触到属于织影落花的物品。"
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
                    """
                            Chance bell 'neath the eaves
                            Sweet chimes bless the quiet scene
                            Ask yon winds on high""",
                    "Craft a wind chime for the first time."
            )
            .lang(
                    DatagenLangRegistry.ZH_CN,
                    "无心檐下铃，闲声入耳伴惬景，何用问罡风",
                    "第一次制作出风铃。"
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
                    "And Yet, And Yet",
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
