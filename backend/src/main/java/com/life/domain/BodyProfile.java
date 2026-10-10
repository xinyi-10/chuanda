package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 身体数据（对应表 body_profile，一人一条） */
@Data
public class BodyProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 性别：1=男 2=女 */
    private Integer gender;
    /** 身高 cm */
    private BigDecimal height;
    /** 体重 kg */
    private BigDecimal weight;
    /** 胸围 cm */
    private BigDecimal bust;
    /** 腰围 cm */
    private BigDecimal waist;
    /** 臀围 cm */
    private BigDecimal hip;
    /** 所在城市（取天气用） */
    private String city;
    /** 风格偏好：casual/formal/sport/business */
    private String stylePref;
    /** 角色形象参考图ID（关联 outfit_image.id，用于 AI 生图图生图锁定人物） */
    private Long avatarImageId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 非表字段：BMI（由身高体重计算） */
    private BigDecimal bmi;
    /** 非表字段：体型描述（偏瘦/标准/偏胖） */
    private String bodyType;
    /** 非表字段：版型建议 */
    private String bodyTip;
}
