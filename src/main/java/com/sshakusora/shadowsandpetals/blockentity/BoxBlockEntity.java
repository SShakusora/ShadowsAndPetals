package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.block.decoration.BoxBlock;
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
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the item represented by a box. A half box keeps its content in {@code Content}; a
 * full box additionally stores the upper half in {@code TopContent}. A missing content value
 * means the box is empty.
 */
public class BoxBlockEntity extends BlockEntity {
    @Nullable
    private String contentRaw;
    @Nullable
    private String topContentRaw;
    private boolean primaryIsTop;

    /**
     * Set while an existing half is being turned into a full box. During that window the
     * vanilla block item merges the incoming stack data into this entity before applying
     * implicit components, so the old half's data must not be overwritten.
     */
    private boolean mergePending;

    public BoxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BOX.get(), pos, state);
    }

    public ItemStack contentStack() {
        return BoxContents.resolveContent(this.contentRaw);
    }

    public ItemStack topContentStack() {
        return BoxContents.resolveContent(this.topContentRaw);
    }

    @Override
    @Deprecated
    public void setBlockState(BlockState state) {
        BlockState previous = this.getBlockState();
        if (previous.hasProperty(BoxBlock.TYPE) && state.hasProperty(BoxBlock.TYPE)) {
            SlabType previousType = previous.getValue(BoxBlock.TYPE);
            if (previousType != SlabType.DOUBLE && state.getValue(BoxBlock.TYPE) == SlabType.DOUBLE) {
                this.mergePending = true;
                this.primaryIsTop = previousType == SlabType.TOP;
            }
        }

        super.setBlockState(state);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (this.mergePending && !tag.contains(BoxContents.TOP_CONTENT_TAG, Tag.TAG_STRING)) {
            // The incoming stack's component is read in applyImplicitComponents; keep the
            // existing half intact because the item tag was merged on top of its data.
            return;
        }

        this.mergePending = false;
        this.contentRaw = tag.contains(BoxContents.CONTENT_TAG, Tag.TAG_STRING)
                ? tag.getString(BoxContents.CONTENT_TAG)
                : null;
        this.topContentRaw = tag.contains(BoxContents.TOP_CONTENT_TAG, Tag.TAG_STRING)
                ? tag.getString(BoxContents.TOP_CONTENT_TAG)
                : null;
        this.primaryIsTop = tag.getBoolean(BoxContents.PRIMARY_IS_TOP_TAG);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.mergePending) {
            // Serialize the pre-merge half so the incoming tag merge cannot clobber its data.
            if (this.contentRaw != null) {
                tag.putString(BoxContents.CONTENT_TAG, this.contentRaw);
            }
            if (this.primaryIsTop) {
                tag.putBoolean(BoxContents.PRIMARY_IS_TOP_TAG, true);
            }
            return;
        }

        if (this.contentRaw != null) {
            tag.putString(BoxContents.CONTENT_TAG, this.contentRaw);
        }
        if (this.isDoubleState()) {
            tag.putString(BoxContents.TOP_CONTENT_TAG,
                    this.topContentRaw != null ? this.topContentRaw : BoxContents.EMPTY_CONTENT_ID);
        } else if (this.topContentRaw != null) {
            tag.putString(BoxContents.TOP_CONTENT_TAG, this.topContentRaw);
        }
        if (this.primaryIsTop) {
            tag.putBoolean(BoxContents.PRIMARY_IS_TOP_TAG, true);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        SlabType type = this.slabType();
        if (type == null) {
            return;
        }

        CompoundTag tag = new CompoundTag();
        boolean hasData = false;
        if (this.contentRaw != null) {
            tag.putString(BoxContents.CONTENT_TAG, this.contentRaw);
            hasData = true;
        }
        if (type == SlabType.DOUBLE) {
            // Always present so a full box item stays full even when a half is empty.
            tag.putString(BoxContents.TOP_CONTENT_TAG,
                    this.topContentRaw != null ? this.topContentRaw : BoxContents.EMPTY_CONTENT_ID);
            hasData = true;
        } else if (type == SlabType.TOP) {
            tag.putBoolean(BoxContents.PRIMARY_IS_TOP_TAG, true);
            hasData = true;
        }

        if (!hasData) {
            return;
        }

        addEntityType(tag, this.getType());
        builder.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data != null ? data.copyTag() : null;
        SlabType type = this.slabType();
        boolean hasTopTag = tag != null && tag.contains(BoxContents.TOP_CONTENT_TAG, Tag.TAG_STRING);

        if (this.mergePending && !hasTopTag) {
            String incoming = contentTag(tag);
            if (this.primaryIsTop) {
                this.topContentRaw = this.contentRaw;
                this.contentRaw = incoming;
            } else {
                this.topContentRaw = incoming;
            }
            this.primaryIsTop = false;
            this.mergePending = false;
            this.setChanged();
            return;
        }

        if (type == SlabType.DOUBLE) {
            this.contentRaw = contentTag(tag);
            this.topContentRaw = tag != null && tag.contains(BoxContents.TOP_CONTENT_TAG, Tag.TAG_STRING)
                    ? tag.getString(BoxContents.TOP_CONTENT_TAG)
                    : null;
            this.primaryIsTop = false;
        } else if (type != null) {
            this.contentRaw = contentTag(tag);
            this.topContentRaw = null;
            this.primaryIsTop = type == SlabType.TOP;
        }

        this.mergePending = false;
        this.setChanged();
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

    @Nullable
    private SlabType slabType() {
        BlockState state = this.getBlockState();
        return state.hasProperty(BoxBlock.TYPE) ? state.getValue(BoxBlock.TYPE) : null;
    }

    private boolean isDoubleState() {
        return this.slabType() == SlabType.DOUBLE;
    }

    @Nullable
    private static String contentTag(@Nullable CompoundTag tag) {
        return tag != null && tag.contains(BoxContents.CONTENT_TAG, Tag.TAG_STRING)
                ? tag.getString(BoxContents.CONTENT_TAG)
                : null;
    }
}
