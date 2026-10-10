package com.life.service;

import com.life.common.PageResult;
import com.life.domain.OutfitImage;

/** 穿搭效果图（AI 生图）服务 */
public interface OutfitImageService {

    /** 触发生成效果图（异步执行），返回任务 id（outfit_image.id） */
    Long generate(Long userId, Long outfitRecordId, String occasion);

    /** 查询生成状态 */
    OutfitImage status(Long userId, Long imageId);

    /** 历史效果图 */
    PageResult<OutfitImage> history(Long userId, Integer pageNum, Integer pageSize);

    /** 读取效果图字节（供前端展示；无权限或不存在返回 null） */
    byte[] readImage(Long userId, Long imageId);

    /** 把某张效果图设为「我的形象」（角色参考图），后续生图走图生图锁定人物 */
    void setAvatar(Long userId, Long imageId);
}
