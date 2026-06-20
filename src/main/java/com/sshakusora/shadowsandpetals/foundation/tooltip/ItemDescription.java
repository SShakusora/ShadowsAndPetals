package com.sshakusora.shadowsandpetals.foundation.tooltip;

import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.ChatFormatting.GRAY;

/**
 * Three-state item tooltip description driven by localisation keys.
 * <p>
 * Reads keys in the form {@code {itemDescriptionId}.tooltip.summary},
 * {@code .conditionN} / {@code .behaviourN}, and {@code .controlN} / {@code .actionN}
 * and exposes them as three line-sets selected by keyboard modifiers:
 * <ul>
 *   <li><b>default</b> — brief "Hold [Shift/Ctrl]" hints</li>
 *   <li><b>Shift held</b> — summary text + condition / behaviour pairs</li>
 *   <li><b>Ctrl held</b> — condition / action pairs</li>
 * </ul>
 * <p>
 * Summary text supports inline highlighting: segments wrapped in underscores
 * ({@code _highlighted_}) are rendered in the highlight style.
 */
public record ItemDescription(List<Component> baseline, List<Component> onShift, List<Component> onCtrl) {

    private static final Style PRIMARY = Style.EMPTY.withColor(TextColor.parseColor("#61C3C7").getOrThrow());
    private static final Style HIGHLIGHT = Style.EMPTY.withColor(TextColor.parseColor("#B8FFE0").getOrThrow());

    /**
     * Resolve the correct line set for the current keyboard state.
     */
    public List<Component> currentLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasShiftDown()) {
            return onShift.isEmpty() ? baseline : onShift;
        }
        if (mc.hasControlDown()) {
            return onCtrl.isEmpty() ? baseline : onCtrl;
        }
        return baseline;
    }

    /**
     * Try to build an ItemDescription from translation keys for the given item.
     * Returns {@code null} when no tooltip data exists for this item.
     */
    @Nullable
    public static ItemDescription of(Item item) {
        String key = item.getDescriptionId() + ".tooltip";
        if (!I18n.exists(key + ".summary")) {
            return null;
        }
        return new Builder(key).build();
    }

    static class Builder {
        private final String key;
        private final List<String> summaries = new ArrayList<>();
        private final List<String[]> behaviours = new ArrayList<>(); // [condition, behaviour]
        private final List<String[]> actions = new ArrayList<>();     // [control, action]

        Builder(String key) {
            this.key = key;
        }

        Builder addSummary(String text) {
            summaries.add(text);
            return this;
        }

        Builder addBehaviour(String condition, String behaviour) {
            behaviours.add(new String[]{condition, behaviour});
            return this;
        }

        Builder addAction(String control, String action) {
            actions.add(new String[]{control, action});
            return this;
        }

        ItemDescription build() {
            // Read from I18n
            if (I18n.exists(key + ".summary")) {
                addSummary(I18n.get(key + ".summary"));
            }
            for (int i = 1; i < 100; i++) {
                String ck = key + ".condition" + i;
                String bk = key + ".behaviour" + i;
                if (!I18n.exists(ck)) break;
                addBehaviour(I18n.get(ck), I18n.get(bk));
            }
            for (int i = 1; i < 100; i++) {
                String ck = key + ".control" + i;
                String ak = key + ".action" + i;
                if (!I18n.exists(ck)) break;
                addAction(I18n.get(ck), I18n.get(ak));
            }

            List<Component> base = new ArrayList<>();
            List<Component> shift = new ArrayList<>();
            List<Component> ctrl = new ArrayList<>();

            for (String s : summaries) {
                shift.addAll(TooltipHelper.cutTextComponent(Component.literal(s), PRIMARY, HIGHLIGHT));
            }
            if (!behaviours.isEmpty()) {
                shift.add(CommonComponents.EMPTY);
            }
            for (String[] pair : behaviours) {
                shift.add(Component.literal(pair[0]).withStyle(GRAY));
                shift.addAll(TooltipHelper.cutTextComponent(Component.literal(pair[1]), PRIMARY, HIGHLIGHT, 1));
            }

            for (String[] pair : actions) {
                ctrl.add(Component.literal(pair[0]).withStyle(GRAY));
                ctrl.addAll(TooltipHelper.cutTextComponent(Component.literal(pair[1]), PRIMARY, HIGHLIGHT, 1));
            }

            boolean hasDesc = !shift.isEmpty();
            boolean hasCtrl = !ctrl.isEmpty();

            if (hasDesc || hasCtrl) {
                MutableComponent keyShiftText = Component.translatable(BuiltinLanguageKeys.TOOLTIP_HOLD_KEY_SHIFT.key());
                MutableComponent keyCtrlText = Component.translatable(BuiltinLanguageKeys.TOOLTIP_HOLD_KEY_CTRL.key());

                // Build hint prefixes for all three line sets
                for (List<Component> target : List.of(base, shift, ctrl)) {
                    boolean isShift = target == shift;
                    boolean isCtrl = target == ctrl;

                    if (hasDesc) {
                        target.addFirst(TooltipHelper.buildHint(
                                BuiltinLanguageKeys.TOOLTIP_HOLD_FOR_DESCRIPTION.key(),
                                keyShiftText, isShift));
                    }
                    if (hasCtrl) {
                        target.addFirst(TooltipHelper.buildHint(
                                BuiltinLanguageKeys.TOOLTIP_HOLD_FOR_CONTROLS.key(),
                                keyCtrlText, isCtrl));
                    }

                    if (isShift || isCtrl) {
                        int gapIndex = hasDesc && hasCtrl ? 2 : 1;
                        if (gapIndex < target.size()) {
                            target.add(gapIndex, CommonComponents.EMPTY);
                        }
                    }
                }
            }

            if (!hasDesc) {
                ctrl.clear();
                shift.addAll(base);
            }
            if (!hasCtrl) {
                ctrl.clear();
                ctrl.addAll(base);
            }

            return new ItemDescription(List.copyOf(base), List.copyOf(shift), List.copyOf(ctrl));
        }

    }

    /**
     * A {@link TooltipModifier} that injects an {@link ItemDescription} into
     * an item's tooltip. The description is lazily built from I18n and
     * re-validated when the game locale changes.
     * <p>
     * Uses a {@link Supplier} for the item to support registration before the
     * actual {@link Item} instance is available (e.g. during
     * {@code DeferredRegister} builder chains).
     */
    public static class Modifier implements TooltipModifier {
        private final Supplier<Item> itemSupplier;
        private Item cachedItem;
        private String cachedLocale;
        @Nullable
        private ItemDescription description;

        public Modifier(Item item) {
            this(() -> item);
        }

        public Modifier(Supplier<Item> itemSupplier) {
            this.itemSupplier = itemSupplier;
        }

        @Override
        public void modify(ItemTooltipEvent event) {
            if (cachedItem == null) {
                cachedItem = itemSupplier.get();
                if (cachedItem == null) return;
            }
            ensureFresh();
            if (description == null) {
                return;
            }
            event.getToolTip().addAll(1, description.currentLines());
        }

        private void ensureFresh() {
            String locale = Minecraft.getInstance().getLanguageManager().getSelected();
            if (!locale.equals(cachedLocale)) {
                cachedLocale = locale;
                description = ItemDescription.of(cachedItem);
            }
        }
    }
}
