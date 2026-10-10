package com.life.dao;

import com.life.domain.ClothingItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 衣物库数据访问接口 */
public interface ClothingItemDao {

    /** 本人衣物（可按类别 / 季节 / 风格过滤，均可为空） */
    List<ClothingItem> listByUser(@Param("userId") Long userId,
                                  @Param("category") String category,
                                  @Param("season") String season,
                                  @Param("style") String style);

    ClothingItem selectById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(ClothingItem item);

    int update(ClothingItem item);

    int logicDelete(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser(@Param("userId") Long userId);
}
