package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;

/** 衣物图片识别结果（AI 视觉识别） */
@Data
public class ClothingRecognitionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 类别：top=上装 bottom=下装 outer=外套 shoes=鞋 accessory=配饰 */
    private String category;
    /** 季节：spring/summer/autumn/winter/all */
    private String season;
    /** 颜色（中文，如 白色 / 灰绿色） */
    private String color;
    /** 建议名称（如 白色T恤） */
    private String name;
    /** 是否识别成功（false 表示识别失败，前端需手动填写） */
    private boolean recognized;
}
