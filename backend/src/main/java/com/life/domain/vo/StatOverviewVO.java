package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** 收支总览（某个月） */
@Data
public class StatOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 统计月份，如 2026-10 */
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    /** 结余 = 收入 - 支出 */
    private BigDecimal balance;
    /** 分类占比明细 */
    private List<CategoryStatVO> byCategory;
}
