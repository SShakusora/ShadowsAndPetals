package com.sshakusora.shadowsandpetals.block.decoration;

import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        ItemStack content = BoxContents.contentStack(stack);
        if (content.isEmpty() || content.is(Items.PORKCHOP)) {
            return super.getName(stack);
        }

        return Component.translatable(
                BuiltinLanguageKeys.RAW_PORKCHOP_BOX_CONTENT_NAME.key(),
                content.getHoverName()
        );
    }
}
