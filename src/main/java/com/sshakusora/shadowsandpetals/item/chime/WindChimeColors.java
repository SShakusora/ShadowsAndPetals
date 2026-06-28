package com.sshakusora.shadowsandpetals.item.chime;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record WindChimeColors(DyeColor ribbon, DyeColor vane) {
    public static final DyeColor DEFAULT_COLOR = DyeColor.PINK;
    public static final WindChimeColors DEFAULT = new WindChimeColors(DEFAULT_COLOR, DEFAULT_COLOR);
    public static final String RIBBON_TAG = "wind_chime_ribbon";
    public static final String VANE_TAG = "wind_chime_vane";

    public WindChimeColors {
        if (ribbon == null) {
            ribbon = DEFAULT_COLOR;
        }
        if (vane == null) {
            vane = DEFAULT_COLOR;
        }
    }

    public WindChimeColors withRibbon(DyeColor color) {
        return new WindChimeColors(color, vane);
    }

    public WindChimeColors withVane(DyeColor color) {
        return new WindChimeColors(ribbon, color);
    }

    public Identifier itemModelId() {
        return ShadowsAndPetals.asResource("wind_chime");
    }

    public static Identifier blockBodyModelId(DyeColor ribbon) {
        return ShadowsAndPetals.asResource("block/wind_chimes/body_" + ribbon.getName());
    }

    public static Identifier blockMainRibbonModelId(DyeColor ribbon) {
        return ShadowsAndPetals.asResource("block/wind_chimes/main_ribbon_" + ribbon.getName());
    }

    public static Identifier blockVaneModelId(DyeColor vane) {
        return ShadowsAndPetals.asResource("block/wind_chimes/vane_" + vane.getName());
    }

    public static Identifier itemBodyModelId() {
        return ShadowsAndPetals.asResource("item/wind_chime/body");
    }

    public static Identifier itemRibbonModelId(DyeColor ribbon) {
        return ShadowsAndPetals.asResource("item/wind_chime/ribbon_" + ribbon.getName());
    }

    public static Identifier itemVaneModelId(DyeColor vane) {
        return ShadowsAndPetals.asResource("item/wind_chime/vane_" + vane.getName());
    }

    public static WindChimeColors fromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? DEFAULT : fromTag(data.copyTag());
    }

    public static WindChimeColors fromTag(CompoundTag tag) {
        DyeColor ribbon = tag.getString(RIBBON_TAG)
                .map(name -> DyeColor.byName(name, DEFAULT_COLOR))
                .orElse(DEFAULT_COLOR);
        DyeColor vane = tag.getString(VANE_TAG)
                .map(name -> DyeColor.byName(name, DEFAULT_COLOR))
                .orElse(DEFAULT_COLOR);
        return new WindChimeColors(ribbon, vane);
    }

    public void applyToStack(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        saveToTag(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_MODEL, itemModelId());
    }

    public void saveToTag(CompoundTag tag) {
        tag.putString(RIBBON_TAG, ribbon.getName());
        tag.putString(VANE_TAG, vane.getName());
    }
}
