package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.ItemModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import com.sshakusora.shadowsandpetals.data.model.SAPItemModelGenerator;

/** 1.21.1 compatibility callbacks for the wind-chime model. */
public final class WindChimeModels {
    private WindChimeModels() {}
    public static void block(BlockModelContext context, SAPBlockModelGenerator generator) {}
    public static void block(Object... ignored) {}
    public static void item(ItemModelContext context, SAPItemModelGenerator generator) {}
    public static void item(Object... ignored) {}
}
