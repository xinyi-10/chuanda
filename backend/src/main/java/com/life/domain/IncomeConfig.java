package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 固定收入配置（对应表 income_config），一人一条 */
@Data
public class IncomeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 每月收入金额 */
    private BigDecimal monthlyAmount;
    /** 每月发薪日（1-31） */
    private Integer payDay;
    /** 可能延迟天数 */
    private Integer delayDays;
    /** 当月已修改次数 */
    private Integer modifyCount;
    /** 最后修改月份（YYYY-MM），自然月重置依据 */
    private String lastModifyMonth;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
