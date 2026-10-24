package com.sshakusora.shadowsandpetals.block.decoration;

import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Block item whose display name reflects the content stored on the stack.
 */
public class BoxItem extends BlockItem {
    public BoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (BoxContents.isDoubleItem(stack)) {
            ItemStack bottom = BoxContents.contentStack(stack);
            ItemStack top = BoxContents.topContentStack(stack);
            if (!bottom.isEmpty() && !top.isEmpty()) {
                return Component.translatable(
                        BuiltinLanguageKeys.BOX_DOUBLE_NAME.key(),
                        bottom.getHoverName(),
                        top.getHoverName()
                );
            }
        }

        ItemStack content = BoxContents.contentStack(stack);
        if (content.isEmpty()) {
            return super.getName(stack);
        }

        return Component.translatable(
                BuiltinLanguageKeys.BOX_CONTENT_NAME.key(),
                content.getHoverName()
        );
    }
}
