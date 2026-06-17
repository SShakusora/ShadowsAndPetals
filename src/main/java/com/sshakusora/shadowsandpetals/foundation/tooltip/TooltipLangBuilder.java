package com.sshakusora.shadowsandpetals.foundation.tooltip;

import com.sshakusora.shadowsandpetals.data.DatagenLangRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for registering tooltip localisation keys into
 * {@link DatagenLangRegistry} during data generation.
 * <p>
 * Usage in a builder chain:
 * <pre>{@code
 * TooltipLangBuilder.of("block.shadowsandpetals.cafe_chair")
 *     .summary("A _cozy_ wooden _chair_.", "一把_舒适_的木_椅_。")
 *     .behaviour("When right-clicked", "右键点击时",
 *                "_Sit_ on the chair.", "在椅子上_坐下_。")
 *     .register();
 * }</pre>
 */
public final class TooltipLangBuilder {
    private final String prefix;
    private final Map<String, String> entries = new LinkedHashMap<>();
    private int behaviourIdx = 1;
    private int actionIdx = 1;

    private TooltipLangBuilder(String prefix) {
        this.prefix = prefix;
    }

    public static TooltipLangBuilder of(String translationKeyPrefix) {
        return new TooltipLangBuilder(translationKeyPrefix);
    }

    /**
     * Register the summary line. Underscore-wrapped text is highlighted.
     */
    public TooltipLangBuilder summary(String en_us, String zh_cn) {
        entries.put(DatagenLangRegistry.DEFAULT_LOCALE + "::" + prefix + ".summary", en_us);
        entries.put(DatagenLangRegistry.ZH_CN + "::" + prefix + ".summary", zh_cn);
        return this;
    }

    /**
     * Register a condition / behaviour pair.
     */
    public TooltipLangBuilder behaviour(
            String conditionEn, String conditionZh,
            String behaviourEn, String behaviourZh
    ) {
        String condKey = prefix + ".condition" + behaviourIdx;
        String behKey = prefix + ".behaviour" + behaviourIdx;
        entries.put(DatagenLangRegistry.DEFAULT_LOCALE + "::" + condKey, conditionEn);
        entries.put(DatagenLangRegistry.ZH_CN + "::" + condKey, conditionZh);
        entries.put(DatagenLangRegistry.DEFAULT_LOCALE + "::" + behKey, behaviourEn);
        entries.put(DatagenLangRegistry.ZH_CN + "::" + behKey, behaviourZh);
        behaviourIdx++;
        return this;
    }

    /**
     * Register a control / action pair (shown on Ctrl).
     */
    public TooltipLangBuilder action(
            String controlEn, String controlZh,
            String actionEn, String actionZh
    ) {
        String ctrlKey = prefix + ".control" + actionIdx;
        String actKey = prefix + ".action" + actionIdx;
        entries.put(DatagenLangRegistry.DEFAULT_LOCALE + "::" + ctrlKey, controlEn);
        entries.put(DatagenLangRegistry.ZH_CN + "::" + ctrlKey, controlZh);
        entries.put(DatagenLangRegistry.DEFAULT_LOCALE + "::" + actKey, actionEn);
        entries.put(DatagenLangRegistry.ZH_CN + "::" + actKey, actionZh);
        actionIdx++;
        return this;
    }

    /**
     * Flush all collected entries to {@link DatagenLangRegistry}.
     */
    public void register() {
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String[] parts = e.getKey().split("::", 2);
            DatagenLangRegistry.add(parts[0], parts[1], e.getValue());
        }
    }
}
