package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 笔记分类实体（对应表 note_category） */
@Data
public class NoteCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String name;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 逻辑删除：0=正常 1=已删除 */
    private Integer deleted;

    /** 非表字段：该分类下笔记数量（列表展示用） */
    private Integer noteCount;
}
