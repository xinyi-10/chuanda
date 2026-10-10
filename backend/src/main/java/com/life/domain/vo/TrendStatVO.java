package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 月度趋势项 */
@Data
public class TrendStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 月份，如 2026-08 */
    private String month;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
}
