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

    public static final DatagenLangRegistry.TranslationKey JEI_IRORI_INFO_1 = DatagenLangRegistry
            .key("jei.shadowsandpetals.irori.info_1")
            .en_us("A traditional Japanese irori (hearth) that connects with adjacent irori blocks.")
            .zh_cn("传统的日式围炉，相邻放置可相互连接。");

    public static final DatagenLangRegistry.TranslationKey JEI_IRORI_INFO_2 = DatagenLangRegistry
            .key("jei.shadowsandpetals.irori.info_2")
            .en_us("Drop burnable items (wood, etc.) into the center, then use Flint and Steel or a Fire Charge to ignite.")
            .zh_cn("将可燃物（木头等）丢入中央，再用打火石或火焰弹点燃。");

    public static final DatagenLangRegistry.TranslationKey JEI_IRORI_INFO_3 = DatagenLangRegistry
            .key("jei.shadowsandpetals.irori.info_3")
            .en_us("Burns fuel over time. Depleted fuel leaves ash, which can be collected as Bone Meal.")
            .zh_cn("燃料会随着时间燃烧殆尽，留下的灰烬可被收集为骨粉。");

    public static final DatagenLangRegistry.TranslationKey JEI_CHISEL_INFO_1 = DatagenLangRegistry
            .key("jei.shadowsandpetals.chisel.info_1")
            .en_us("Place in your off-hand, then right-click Stone with a Hammer to carve rockeries.")
            .zh_cn("放置在副手，用锤子右击石头可雕刻出假山。");

    public static final DatagenLangRegistry.TranslationKey JEI_HAMMER_INFO_1 = DatagenLangRegistry
            .key("jei.shadowsandpetals.hammer.info_1")
            .en_us("Right-click Stone while holding a Chisel in your off-hand to carve rockery blocks.")
            .zh_cn("副手持凿子时，右击石头可雕刻出假山。");

    private BuiltinLanguageKeys() {}

    public static void bootstrap() {}
}
