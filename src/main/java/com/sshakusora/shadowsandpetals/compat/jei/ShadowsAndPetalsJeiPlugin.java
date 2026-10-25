package com.sshakusora.shadowsandpetals.compat.jei;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Teaches JEI that boxes holding different contents are different ingredients. Without this,
 * every recipe whose result is a box collapses into the same ingredient and pressing R on one
 * box lists the recipes of all box variants.
 */
@JeiPlugin
public final class ShadowsAndPetalsJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ShadowsAndPetals.asResource("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(
                BlockRegistry.BOX.get().asItem(),
                BoxContentSubtypeInterpreter.INSTANCE
        );
    }

    private enum BoxContentSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
        INSTANCE;

        @Nullable
        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            String content = BoxContents.contentId(stack);
            String topContent = BoxContents.topContentId(stack);
            if (content == null && topContent == null) {
                return null;
            }

            return (content != null ? content : "") + '|' + (topContent != null ? topContent : "");
        }

        @SuppressWarnings("deprecation")
        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            Object data = getSubtypeData(stack, context);
            return data == null ? "" : data.toString();
        }
    }
}
