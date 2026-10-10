package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 衣物（对应表 clothing_item） */
@Data
public class ClothingItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String name;
    /** 类别（字典 clothing_category）：top=上装 bottom=下装 outer=外套 shoes=鞋 accessory=配饰 */
    private String category;
    /** 主色 */
    private String color;
    /** 适用季节（字典 season）：spring/summer/autumn/winter/all */
    private String season;
    /** 风格标签（字典 style）：casual/formal/sport/business */
    private String style;
    private String imageUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 非表字段：类别中文名 */
    private String categoryLabel;
}
