package com.life.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 收入配置视图（含剩余修改次数与预期到账区间） */
@Data
public class IncomeConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private BigDecimal monthlyAmount;
    private Integer payDay;
    private Integer delayDays;

    /** 当月已修改次数 */
    private Integer modifyCount;
    /** 当月剩余可修改次数（自然月，上限 2） */
    private Integer remainModify;
    /** 当前月份，如 2026-10 */
    private String currentMonth;

    /** 预期到账区间描述，如「每月 10 日 ~ 15 日」 */
    private String payDateRange;

    /** 是否已完成首次填写（false 时前端弹首次引导） */
    private Boolean initialized;
}
