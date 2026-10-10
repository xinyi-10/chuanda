package com.life.dao;

import com.life.domain.Note;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 笔记数据访问接口（对应 mapper/NoteMapper.xml）。
 *
 * <p>所有方法显式传 userId，配合 UserIsolationInterceptor 保证数据隔离。</p>
 */
public interface NoteDao {

    /** 分页查询（可按分类、关键词过滤） */
    List<Note> listByPage(@Param("userId") Long userId,
                          @Param("categoryId") Long categoryId,
                          @Param("keyword") String keyword,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);

    /** 条件计数（与 listByPage 条件保持一致） */
    long countByCondition(@Param("userId") Long userId,
                          @Param("categoryId") Long categoryId,
                          @Param("keyword") String keyword);

    /** 按主键查询（限本人） */
    Note selectById(@Param("id") Long id, @Param("userId") Long userId);

    /** 新增 */
    int insert(Note note);

    /** 编辑 */
    int update(Note note);

    /** 逻辑删除 */
    int logicDelete(@Param("id") Long id, @Param("userId") Long userId);

    /** 统计某分类下的有效笔记数（分类删除保护用） */
    long countByCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId);
}
