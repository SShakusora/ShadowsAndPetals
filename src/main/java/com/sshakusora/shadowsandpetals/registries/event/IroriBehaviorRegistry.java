package com.sshakusora.shadowsandpetals.registries.event;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.irori.*;
import com.sshakusora.shadowsandpetals.registries.BlockTagRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.OptionalInt;

/** Registers the built-in behavior rules exposed through the public Irori API. */
public final class IroriBehaviorRegistry {
    private static final int MIN_ASH_BONE_MEAL_DROPS = 1;
    private static final int MAX_ASH_BONE_MEAL_DROPS = 3;

    private IroriBehaviorRegistry() {
    }

    public static void register(RegisterIroriBehaviorsEvent event) {
        event.registerGrillRule(
                ShadowsAndPetals.asResource("block_tag_requires_grill"),
                (irori, content) -> content instanceof IroriContent.BlockContent blockContent
                        && blockContent.state().is(BlockTagRegistry.REQUIRES_IRORI_GRILL)
                        ? GrillRequirement.REQUIRE
                        : GrillRequirement.PASS
        );
        event.registerFuelRule(
                ShadowsAndPetals.asResource("vanilla_fuel"),
                Integer.MIN_VALUE,
                (stack, level) -> {
                    int burnTime = stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
                    return burnTime > 0 ? OptionalInt.of(burnTime) : OptionalInt.empty();
                }
        );
        event.registerIgnitionBehavior(
                ShadowsAndPetals.asResource("flint_and_steel"),
                itemIgnitionBehavior(Items.FLINT_AND_STEEL, SoundEvents.FLINTANDSTEEL_USE, true)
        );
        event.registerIgnitionBehavior(
                ShadowsAndPetals.asResource("fire_charge"),
                itemIgnitionBehavior(Items.FIRE_CHARGE, SoundEvents.FIRECHARGE_USE, false)
        );
        event.registerAshDropProvider(
                ShadowsAndPetals.asResource("bone_meal_ash"),
                context -> {
                    int count = MIN_ASH_BONE_MEAL_DROPS + context.random().nextInt(
                            MAX_ASH_BONE_MEAL_DROPS - MIN_ASH_BONE_MEAL_DROPS + 1
                    );
                    return List.of(new ItemStack(Items.BONE_MEAL, count));
                }
        );
    }

    private static IroriIgnitionBehavior itemIgnitionBehavior(
            Item item,
            SoundEvent sound,
            boolean damageItem
    ) {
        return new IroriIgnitionBehavior() {
            @Override
            public boolean matches(ItemStack stack) {
                return stack.is(item);
            }

            @Override
            public void onIgnited(IroriIgnitionContext context) {
                context.level().playSound(
                        null,
                        context.interactionPos(),
                        sound,
                        SoundSource.BLOCKS,
                        1.0F,
                        context.level().getRandom().nextFloat() * 0.4F + 0.8F
                );
                context.level().gameEvent(context.player(), GameEvent.BLOCK_CHANGE, context.interactionPos());
                if (damageItem) {
                    context.stack().hurtAndBreak(1, context.player(), context.hand().asEquipmentSlot());
                } else if (!context.player().isCreative()) {
                    context.stack().shrink(1);
                }
            }
        };
    }
}
