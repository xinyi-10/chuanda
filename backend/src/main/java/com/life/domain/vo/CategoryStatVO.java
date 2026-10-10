package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 分类统计项（用于分类占比） */
@Data
public class CategoryStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String categoryName;
    /** 1=支出 2=收入 */
    private Integer type;
    private BigDecimal amount;
    /** 占比（百分数，保留 1 位小数，如 35.9） */
    private BigDecimal percent;
}
