package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the item represented by a box. A missing content value keeps the legacy porkchop default.
 */
public class BoxBlockEntity extends BlockEntity {
    @Nullable
    private String contentRaw;

    public BoxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.RAW_PORKCHOP_BOX.get(), pos, state);
    }

    public ItemStack contentStack() {
        return BoxContents.resolveContent(this.contentRaw);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.contentRaw = tag.contains(BoxContents.CONTENT_TAG, Tag.TAG_STRING)
                ? tag.getString(BoxContents.CONTENT_TAG)
                : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.contentRaw != null) {
            tag.putString(BoxContents.CONTENT_TAG, this.contentRaw);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (this.contentRaw == null) {
            return;
        }

        CompoundTag tag = new CompoundTag();
        tag.putString(BoxContents.CONTENT_TAG, this.contentRaw);
        addEntityType(tag, this.getType());
        builder.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return;
        }

        CompoundTag tag = data.copyTag();
        if (tag.contains(BoxContents.CONTENT_TAG, Tag.TAG_STRING)) {
            this.contentRaw = tag.getString(BoxContents.CONTENT_TAG);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
