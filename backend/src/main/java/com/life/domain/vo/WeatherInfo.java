package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 天气信息 */
@Data
public class WeatherInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String city;
    /** 气温 ℃ */
    private BigDecimal temp;
    /** 体感温度 ℃ */
    private BigDecimal feelsLike;
    /** 天气状况：晴 / 多云 / 阴 / 小雨 / 雪 … */
    private String condition;
    /** 湿度 % */
    private Integer humidity;
    /** 风力描述 */
    private String wind;
    /** 是否来自真实天气 API；false 表示已降级为季节策略 */
    private boolean fromApi;
    /** 季节提示：春/夏/秋/冬 */
    private String season;
}
