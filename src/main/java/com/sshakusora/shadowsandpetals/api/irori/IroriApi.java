package com.sshakusora.shadowsandpetals.api.irori;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Public registration and query entry point for Irori mechanisms.
 *
 * <p>Mods should normally register rules from {@link RegisterIroriBehaviorsEvent}. Direct
 * registration is also supported during mod initialization. Registered rules are published as an
 * immutable snapshot, making queries lock-free on render and level threads.
 */
public final class IroriApi {
    private static final Map<Identifier, IroriGrillRule> GRILL_RULES = new LinkedHashMap<>();
    private static final Map<Identifier, RegisteredFuelRule> FUEL_RULES = new LinkedHashMap<>();
    private static final Map<Identifier, RegisteredIgnitionBehavior> IGNITION_BEHAVIORS = new LinkedHashMap<>();
    private static final Map<Identifier, IroriAshDropProvider> ASH_DROP_PROVIDERS = new LinkedHashMap<>();
    private static volatile List<RegisteredGrillRule> grillRuleSnapshot = List.of();
    private static volatile List<RegisteredFuelRule> fuelRuleSnapshot = List.of();
    private static volatile List<RegisteredIgnitionBehavior> ignitionBehaviorSnapshot = List.of();
    private static volatile List<RegisteredAshDropProvider> ashDropProviderSnapshot = List.of();

    private IroriApi() {
    }

    /**
     * Registers a grill requirement rule.
     *
     * @throws IllegalArgumentException if {@code id} has already been registered
     */
    public static synchronized void registerGrillRule(Identifier id, IroriGrillRule rule) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rule, "rule");
        if (GRILL_RULES.putIfAbsent(id, rule) != null) {
            throw new IllegalArgumentException("Duplicate Irori grill rule id: " + id);
        }

        List<RegisteredGrillRule> snapshot = new ArrayList<>(GRILL_RULES.size());
        GRILL_RULES.forEach((ruleId, registeredRule) -> snapshot.add(new RegisteredGrillRule(ruleId, registeredRule)));
        grillRuleSnapshot = List.copyOf(snapshot);
    }

    public static void registerFuelRule(Identifier id, IroriFuelRule rule) {
        registerFuelRule(id, 0, rule);
    }

    /**
     * Registers a fuel rule. Higher-priority rules are evaluated first; equal priorities preserve
     * registration order.
     */
    public static synchronized void registerFuelRule(Identifier id, int priority, IroriFuelRule rule) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rule, "rule");
        RegisteredFuelRule registered = new RegisteredFuelRule(id, priority, rule);
        if (FUEL_RULES.putIfAbsent(id, registered) != null) {
            throw new IllegalArgumentException("Duplicate Irori fuel rule id: " + id);
        }

        List<RegisteredFuelRule> snapshot = new ArrayList<>(FUEL_RULES.values());
        snapshot.sort(Comparator.comparingInt(RegisteredFuelRule::priority).reversed());
        fuelRuleSnapshot = List.copyOf(snapshot);
    }

    public static void registerIgnitionBehavior(Identifier id, IroriIgnitionBehavior behavior) {
        registerIgnitionBehavior(id, 0, behavior);
    }

    /**
     * Registers an ignition behavior. Higher-priority matching behaviors take precedence.
     */
    public static synchronized void registerIgnitionBehavior(
            Identifier id,
            int priority,
            IroriIgnitionBehavior behavior
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(behavior, "behavior");
        RegisteredIgnitionBehavior registered = new RegisteredIgnitionBehavior(id, priority, behavior);
        if (IGNITION_BEHAVIORS.putIfAbsent(id, registered) != null) {
            throw new IllegalArgumentException("Duplicate Irori ignition behavior id: " + id);
        }

        List<RegisteredIgnitionBehavior> snapshot = new ArrayList<>(IGNITION_BEHAVIORS.values());
        snapshot.sort(Comparator.comparingInt(RegisteredIgnitionBehavior::priority).reversed());
        ignitionBehaviorSnapshot = List.copyOf(snapshot);
    }

    /** Registers an additive ash-drop provider. */
    public static synchronized void registerAshDropProvider(Identifier id, IroriAshDropProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (ASH_DROP_PROVIDERS.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("Duplicate Irori ash-drop provider id: " + id);
        }

        List<RegisteredAshDropProvider> snapshot = new ArrayList<>(ASH_DROP_PROVIDERS.size());
        ASH_DROP_PROVIDERS.forEach((providerId, registeredProvider) ->
                snapshot.add(new RegisteredAshDropProvider(providerId, registeredProvider)));
        ashDropProviderSnapshot = List.copyOf(snapshot);
    }

    /**
     * Returns whether any registered rule requires a grill for any current surface content.
     */
    public static boolean requiresGrill(IroriView irori) {
        Objects.requireNonNull(irori, "irori");
        List<RegisteredGrillRule> rules = grillRuleSnapshot;
        for (IroriContent content : irori.surfaceContents()) {
            for (RegisteredGrillRule registered : rules) {
                GrillRequirement requirement = Objects.requireNonNull(
                        registered.rule().evaluate(irori, content),
                        () -> "Irori grill rule " + registered.id() + " returned null"
                );
                if (requirement == GrillRequirement.REQUIRE) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns the burn time selected by the first matching fuel rule, or zero when none match. */
    public static int getFuelBurnTime(ItemStack stack, Level level) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(level, "level");
        if (stack.isEmpty()) {
            return 0;
        }

        for (RegisteredFuelRule registered : fuelRuleSnapshot) {
            OptionalInt burnTime = Objects.requireNonNull(
                    registered.rule().getBurnTime(stack, level),
                    () -> "Irori fuel rule " + registered.id() + " returned null"
            );
            if (burnTime.isEmpty()) {
                continue;
            }
            int value = burnTime.getAsInt();
            if (value <= 0) {
                throw new IllegalStateException(
                        "Irori fuel rule " + registered.id() + " returned non-positive burn time " + value
                );
            }
            return value;
        }
        return 0;
    }

    /** Returns the highest-priority ignition behavior matching the supplied stack. */
    public static Optional<IroriIgnitionBehavior> findIgnitionBehavior(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        for (RegisteredIgnitionBehavior registered : ignitionBehaviorSnapshot) {
            if (registered.behavior().matches(stack)) {
                return Optional.of(registered.behavior());
            }
        }
        return Optional.empty();
    }

    /** Collects defensive copies of all drops contributed by registered ash-drop providers. */
    public static List<ItemStack> getAshDrops(IroriAshDropContext context) {
        Objects.requireNonNull(context, "context");
        List<ItemStack> drops = new ArrayList<>();
        for (RegisteredAshDropProvider registered : ashDropProviderSnapshot) {
            List<ItemStack> contributed = Objects.requireNonNull(
                    registered.provider().getDrops(context),
                    () -> "Irori ash-drop provider " + registered.id() + " returned null"
            );
            for (ItemStack stack : contributed) {
                Objects.requireNonNull(stack, () ->
                        "Irori ash-drop provider " + registered.id() + " returned a null stack");
                if (!stack.isEmpty()) {
                    drops.add(stack.copy());
                }
            }
        }
        return List.copyOf(drops);
    }

    /**
     * Returns the registered grill rule ids in evaluation order.
     */
    public static List<Identifier> registeredGrillRuleIds() {
        return grillRuleSnapshot.stream().map(RegisteredGrillRule::id).toList();
    }

    public static List<Identifier> registeredFuelRuleIds() {
        return fuelRuleSnapshot.stream().map(RegisteredFuelRule::id).toList();
    }

    public static List<Identifier> registeredIgnitionBehaviorIds() {
        return ignitionBehaviorSnapshot.stream().map(RegisteredIgnitionBehavior::id).toList();
    }

    public static List<Identifier> registeredAshDropProviderIds() {
        return ashDropProviderSnapshot.stream().map(RegisteredAshDropProvider::id).toList();
    }

    private record RegisteredGrillRule(Identifier id, IroriGrillRule rule) {
    }

    private record RegisteredFuelRule(Identifier id, int priority, IroriFuelRule rule) {
    }

    private record RegisteredIgnitionBehavior(
            Identifier id,
            int priority,
            IroriIgnitionBehavior behavior
    ) {
    }

    private record RegisteredAshDropProvider(Identifier id, IroriAshDropProvider provider) {
    }
}
