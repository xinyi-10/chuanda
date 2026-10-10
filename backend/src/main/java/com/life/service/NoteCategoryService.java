package com.life.service;

import com.life.domain.NoteCategory;

import java.util.List;

/** 笔记分类服务 */
public interface NoteCategoryService {

    /** 本人全部分类（含笔记数统计） */
    List<NoteCategory> list(Long userId);

    /** 新建分类，返回新分类 ID */
    Long create(Long userId, String name, Integer sort);

    /** 重命名 / 改排序 */
    void update(Long userId, Long id, String name, Integer sort);

    /** 删除（分类下有笔记时拒绝，抛 CATEGORY_IN_USE） */
    void delete(Long userId, Long id);
}
