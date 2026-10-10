package com.life.dao;

import com.life.domain.OutfitRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 穿搭推荐记录数据访问接口 */
public interface OutfitRecordDao {

    int insert(OutfitRecord record);

    List<OutfitRecord> listByUser(@Param("userId") Long userId,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    long countByUser(@Param("userId") Long userId);

    OutfitRecord selectById(@Param("id") Long id, @Param("userId") Long userId);
}
