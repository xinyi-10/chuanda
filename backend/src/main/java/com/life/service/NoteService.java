package com.life.service;

import com.life.common.PageResult;
import com.life.domain.Note;

/** 笔记服务 */
public interface NoteService {

    /** 分页查询（可按分类 + 关键词过滤） */
    PageResult<Note> page(Long userId, Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    /** 查询详情（限本人） */
    Note getById(Long id, Long userId);

    /** 新建，返回新笔记 ID */
    Long create(Long userId, Note note);

    /** 编辑 */
    void update(Long userId, Note note);

    /** 逻辑删除 */
    void delete(Long userId, Long id);
}
