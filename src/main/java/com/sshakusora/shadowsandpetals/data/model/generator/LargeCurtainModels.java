package com.sshakusora.shadowsandpetals.data.model.generator;

import com.sshakusora.shadowsandpetals.data.model.BlockModelContext;
import com.sshakusora.shadowsandpetals.data.model.ItemModelContext;
import com.sshakusora.shadowsandpetals.data.model.SAPBlockModelGenerator;
import com.sshakusora.shadowsandpetals.data.model.SAPItemModelGenerator;

/** 1.21.1 compatibility callbacks for the 26.x model generator. */
public final class LargeCurtainModels {
    private LargeCurtainModels() {}
    public static void block(BlockModelContext context, SAPBlockModelGenerator generator) {}
    public static void block(Object... ignored) {}
}
