package com.life.dao;

import com.life.domain.IncomeConfig;
import org.apache.ibatis.annotations.Param;

/** 固定收入配置数据访问接口（一人一条） */
public interface IncomeConfigDao {

    /** 查当前用户的收入配置（未填写返回 null） */
    IncomeConfig selectByUser(@Param("userId") Long userId);

    /** 列出所有已配置收入（定时任务遍历用，无用户上下文） */
    java.util.List<IncomeConfig> listAll();

    int insert(IncomeConfig config);

    /** 更新配置并刷新修改次数与月份 */
    int update(IncomeConfig config);

    /** 仅重置当月计数（跨自然月时使用） */
    int resetModifyCount(@Param("id") Long id, @Param("month") String month);
}
