package com.life.dao;

import com.life.domain.LedgerCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 记账分类数据访问接口（对应 mapper/LedgerCategoryMapper.xml） */
public interface LedgerCategoryDao {

    /** 本人分类（type 为 null 查全部） */
    List<LedgerCategory> listByUser(@Param("userId") Long userId, @Param("type") Integer type);

    LedgerCategory selectById(@Param("id") Long id, @Param("userId") Long userId);

    /** 同类型下重名校验 */
    LedgerCategory selectByName(@Param("userId") Long userId,
                                @Param("name") String name,
                                @Param("type") Integer type);

    int insert(LedgerCategory category);

    int update(LedgerCategory category);

    int logicDelete(@Param("id") Long id, @Param("userId") Long userId);

    /** 统计分类下流水数（删除保护） */
    long countRecords(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    /** 统计该用户已有分类数（判断是否需要初始化默认分类） */
    long countByUser(@Param("userId") Long userId);
}
