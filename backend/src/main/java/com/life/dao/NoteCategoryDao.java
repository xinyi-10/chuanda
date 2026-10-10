package com.life.dao;

import com.life.domain.NoteCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 笔记分类数据访问接口（对应 mapper/NoteCategoryMapper.xml） */
public interface NoteCategoryDao {

    /** 查询本人全部分类（按 sort 升序） */
    List<NoteCategory> listByUser(@Param("userId") Long userId);

    /** 按主键查询（限本人） */
    NoteCategory selectById(@Param("id") Long id, @Param("userId") Long userId);

    /** 按名称查询（重名校验） */
    NoteCategory selectByName(@Param("userId") Long userId, @Param("name") String name);

    /** 新增 */
    int insert(NoteCategory category);

    /** 重命名 / 改排序 */
    int update(NoteCategory category);

    /** 逻辑删除 */
    int logicDelete(@Param("id") Long id, @Param("userId") Long userId);
}
