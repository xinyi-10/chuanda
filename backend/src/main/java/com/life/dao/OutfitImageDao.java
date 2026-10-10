package com.life.dao;

import com.life.domain.OutfitImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 穿搭效果图数据访问接口 */
public interface OutfitImageDao {

    int insert(OutfitImage image);

    /** 更新状态与结果（成功写 image_key，失败/超时写 error_msg，均置 finish_time） */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("imageKey") String imageKey,
                     @Param("errorMsg") String errorMsg);

    OutfitImage selectById(@Param("id") Long id, @Param("userId") Long userId);

    List<OutfitImage> listByUser(@Param("userId") Long userId,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    long countByUser(@Param("userId") Long userId);

    /** 超时补偿：扫描「生成中」且已超过 N 分钟的任务 */
    List<OutfitImage> listTimeout(@Param("minutes") Integer minutes);
}
