package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 穿搭推荐记录（对应表 outfit_record） */
@Data
public class OutfitRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 场合 */
    private String occasion;
    /** 天气快照 */
    private String weather;
    /** 推荐衣物快照 JSON（id+name+category 冗余，避免衣物删除后变死 ID） */
    private String itemsJson;
    /** 推荐建议文本 */
    private String advice;
    /** 来源：1=AI 2=规则兜底 */
    private Integer source;
    private LocalDateTime createTime;
}
