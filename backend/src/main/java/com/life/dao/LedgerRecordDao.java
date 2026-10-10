package com.life.dao;

import com.life.domain.LedgerRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 记账流水数据访问接口（对应 mapper/LedgerRecordMapper.xml） */
public interface LedgerRecordDao {

    /** 分页查询（type 由分类派生，故按分类的 type 过滤） */
    List<LedgerRecord> listByPage(@Param("userId") Long userId,
                                  @Param("type") Integer type,
                                  @Param("categoryId") Long categoryId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    long countByCondition(@Param("userId") Long userId,
                          @Param("type") Integer type,
                          @Param("categoryId") Long categoryId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    LedgerRecord selectById(@Param("id") Long id, @Param("userId") Long userId);

    /** 按来源查（幂等判断）：同一凭证 / 同一月份工资只能入账一次 */
    LedgerRecord selectBySource(@Param("userId") Long userId,
                                @Param("sourceType") Integer sourceType,
                                @Param("sourceId") Long sourceId);

    int insert(LedgerRecord record);

    int update(LedgerRecord record);

    int logicDelete(@Param("id") Long id, @Param("userId") Long userId);
}
