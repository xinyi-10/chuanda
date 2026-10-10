package com.life.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

/**
 * 金额序列化模块（P0 铁律 H5）。
 *
 * <p>把 BigDecimal 统一序列化为 <b>字符串</b>，避免 JSON 层被解析成浮点数丢精度
 * （如 8000.00 在 JS 里是 number，.toFixed 前可能已失真）。</p>
 *
 * <p>金额链路三处一致：库 {@code DECIMAL(12,2)} + Java {@code BigDecimal} + JSON 字符串。
 * 前端拿到字符串后按需展示为 "8000.00"，参与计算时用字符串安全算法或后端算好。</p>
 */
public class BigDecimalModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public BigDecimalModule() {
        super("BigDecimalAsStringModule");
        // 所有 BigDecimal 字段以字符串形式输出
        addSerializer(BigDecimal.class, ToStringSerializer.instance);
    }
}
