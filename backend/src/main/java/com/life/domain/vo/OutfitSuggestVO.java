package com.life.domain.vo;

import com.life.domain.ClothingItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 穿搭推荐结果 */
@Data
public class OutfitSuggestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 推荐记录 ID */
    private Long recordId;

    /** 天气情况 */
    private WeatherInfo weather;

    /** 推荐选中的衣物（含名称/类别，快照） */
    private List<ClothingItem> items;

    /** 推荐理由（逐条） */
    private List<String> reasons;

    /** 建议文案 */
    private String advice;

    /** 来源：1=AI(LLM) 2=规则引擎 */
    private Integer source;

    /** 体型提示（BMI / 版型建议） */
    private String bodyTip;
}
