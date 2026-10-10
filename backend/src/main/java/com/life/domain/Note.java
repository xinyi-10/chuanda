package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 笔记实体（对应表 note） */
@Data
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 分类 ID，null 表示未分类 */
    private Long categoryId;
    private String title;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0=正常 1=已删除 */
    private Integer deleted;

    /** 非表字段：分类名称（联表查询用于列表展示） */
    private String categoryName;
}
