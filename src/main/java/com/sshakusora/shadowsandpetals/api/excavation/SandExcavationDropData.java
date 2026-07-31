package com.sshakusora.shadowsandpetals.api.excavation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record SandExcavationDropData(
        SandExcavationDropCategory category,
        int weight,
        int minCount,
        int maxCount
) {
    private static final Codec<SandExcavationDropData> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SandExcavationDropCategory.CODEC.fieldOf("category").forGetter(SandExcavationDropData::category),
            Codec.intRange(1, 10_000).optionalFieldOf("weight", 1).forGetter(SandExcavationDropData::weight),
            Codec.intRange(1, 64).optionalFieldOf("min_count", 1).forGetter(SandExcavationDropData::minCount),
            Codec.intRange(1, 64).optionalFieldOf("max_count", 1).forGetter(SandExcavationDropData::maxCount)
    ).apply(instance, SandExcavationDropData::new));

    public static final Codec<SandExcavationDropData> CODEC = RAW_CODEC.flatXmap(
            SandExcavationDropData::validate,
            SandExcavationDropData::validate
    );

    private static DataResult<SandExcavationDropData> validate(SandExcavationDropData data) {
        if (data.minCount > data.maxCount) {
            return DataResult.error(() -> "min_count must not be greater than max_count");
        }
        return DataResult.success(data);
    }

    public ItemStack createStack(Item item, RandomSource random) {
        int count = minCount == maxCount ? minCount : random.nextInt(minCount, maxCount + 1);
        ItemStack stack = new ItemStack(item);
        stack.setCount(Math.min(count, stack.getMaxStackSize()));
        return stack;
    }
}
