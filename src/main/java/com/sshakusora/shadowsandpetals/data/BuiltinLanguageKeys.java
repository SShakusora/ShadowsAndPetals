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

    private BuiltinLanguageKeys() {}

    public static void bootstrap() {}
}
