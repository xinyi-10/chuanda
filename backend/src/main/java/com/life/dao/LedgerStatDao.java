package com.life.dao;

import com.life.domain.vo.CategoryStatVO;
import com.life.domain.vo.TrendStatVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 记账统计数据访问接口（对应 mapper/LedgerStatMapper.xml） */
public interface LedgerStatDao {

    /** 区间内收入/支出合计，返回 { totalIncome, totalExpense } */
    Map<String, Object> sumRange(@Param("userId") Long userId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /** 区间内按分类聚合（分类占比） */
    List<CategoryStatVO> statByCategory(@Param("userId") Long userId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    /** 月度趋势（start 起至今，按月分组） */
    List<TrendStatVO> statTrend(@Param("userId") Long userId,
                                @Param("start") LocalDateTime start);
}
