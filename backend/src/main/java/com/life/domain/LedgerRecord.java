package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账流水实体（对应表 ledger_record）。
 *
 * <p><b>H6：收支类型不落库</b>，统一由 {@code ledger_category.type} 派生，
 * 避免「流水 type 与分类 type 不一致」的脏数据。因此本表没有 type 字段，
 * 查询时通过 JOIN 分类带出（见下方的非表字段 type）。</p>
 */
@Data
public class LedgerRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long categoryId;
    /** 金额：正数，收支方向由分类 type 决定（DECIMAL(12,2) / BigDecimal） */
    private BigDecimal amount;
    private LocalDateTime recordTime;
    private String remark;
    /** 来源：1=手动记账 2=凭证入账 3=工资自动入账 */
    private Integer sourceType;
    /** 来源单据 ID：凭证入账存 receipt_image.id；工资入账存 YYYYMM */
    private Long sourceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 非表字段：分类名称（JOIN 带出） */
    private String categoryName;
    /** 非表字段：收支类型（由分类派生）1=支出 2=收入 */
    private Integer type;
}
