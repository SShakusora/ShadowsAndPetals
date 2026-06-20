package com.sshakusora.shadowsandpetals.data;

public final class BuiltinLanguageKeys {
    public static final DatagenLangRegistry.TranslationKey CAFE_CHAIR_DYEABLE_TOOLTIP = DatagenLangRegistry
            .key("jade.shadowsandpetals.cafe_chair.dyeable")
            .en_us("Dyeable")
            .zh_cn("可染色");

    public static final DatagenLangRegistry.TranslationKey CAFE_CHAIR_DYE_HINT_PREFIX = DatagenLangRegistry
            .key("message.shadowsandpetals.cafe_chair.dye_hint_prefix")
            .en_us("%s can be dyed to ")
            .zh_cn("%s 可被染成 ");

    public static final DatagenLangRegistry.TranslationKey VANITY_CONTAINER_NAME = DatagenLangRegistry
            .key("container.shadowsandpetals.vanity")
            .en_us("Vanity")
            .zh_cn("梳妆台");

    public static final DatagenLangRegistry.TranslationKey JADE_CAFE_CHAIR_DYEABLE_CONFIG = DatagenLangRegistry
            .key("config.jade.plugin_shadowsandpetals.jade.cafe_chair_dyeable")
            .en_us("Show if cafe chairs are dyeable")
            .zh_cn("显示咖啡椅是否可染色");

    public static final DatagenLangRegistry.TranslationKey JADE_IRORI_BURN_TIME_CONFIG = DatagenLangRegistry
            .key("config.jade.plugin_shadowsandpetals.jade.irori_burn_time")
            .en_us("Show irori burn time")
            .zh_cn("显示围炉燃烧时间");

    public static final DatagenLangRegistry.TranslationKey JADE_IRORI_BURNING = DatagenLangRegistry
            .key("jade.shadowsandpetals.irori.burning")
            .en_us("Burning")
            .zh_cn("燃烧中");

    public static final DatagenLangRegistry.TranslationKey JADE_IRORI_BURNED_OUT = DatagenLangRegistry
            .key("jade.shadowsandpetals.irori.burned_out")
            .en_us("Burned out")
            .zh_cn("已燃尽");

    public static final DatagenLangRegistry.TranslationKey JADE_ROCKERY_HAMMERING = DatagenLangRegistry
            .key("jade.shadowsandpetals.rockery.hammering")
            .en_us("Hammering...")
            .zh_cn("锤击中…");

    public static final DatagenLangRegistry.TranslationKey JADE_ROCKERY_HAMMER_CONFIG = DatagenLangRegistry
            .key("config.jade.plugin_shadowsandpetals.jade.rockery_hammer_progress")
            .en_us("Show rockery hammering progress")
            .zh_cn("显示石山锤击进度");

    public static final DatagenLangRegistry.TranslationKey ROCKERY_DIMENSIONS_LABEL = DatagenLangRegistry
            .key("tooltip.shadowsandpetals.rockery.dimensions_label")
            .en_us("Dimensions: ")
            .zh_cn("尺寸: ");

    public static final DatagenLangRegistry.TranslationKey TOOLTIP_HOLD_FOR_DESCRIPTION = DatagenLangRegistry
            .key("tooltip.shadowsandpetals.holdForDescription")
            .en_us("Hold [%s] for Description")
            .zh_cn("按住 [%s] 显示描述");

    public static final DatagenLangRegistry.TranslationKey TOOLTIP_HOLD_FOR_CONTROLS = DatagenLangRegistry
            .key("tooltip.shadowsandpetals.holdForControls")
            .en_us("Hold [%s] for Controls")
            .zh_cn("按住 [%s] 显示操作");

    public static final DatagenLangRegistry.TranslationKey ROCKERY_HOLD_FOR_PREVIEW = DatagenLangRegistry
            .key("tooltip.shadowsandpetals.rockery.holdForPreview")
            .en_us("Hold [%s] for Preview")
            .zh_cn("按住 [%s] 显示预览");

    public static final DatagenLangRegistry.TranslationKey TOOLTIP_HOLD_KEY_SHIFT = DatagenLangRegistry
            .key("tooltip.shadowsandpetals.holdKey.shift")
            .en_us("Shift")
            .zh_cn("Shift");

    public static final DatagenLangRegistry.TranslationKey TOOLTIP_HOLD_KEY_CTRL = DatagenLangRegistry
            .key("tooltip.shadowsandpetals.holdKey.ctrl")
            .en_us("Ctrl")
            .zh_cn("Ctrl");

    private BuiltinLanguageKeys() {}

    public static void bootstrap() {}
}
