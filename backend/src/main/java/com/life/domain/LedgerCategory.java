package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 记账分类实体（对应表 ledger_category） */
@Data
public class LedgerCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String name;
    /** 类型：1=支出 2=收入 */
    private Integer type;
    private String icon;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
