package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 穿搭效果图（对应表 outfit_image，一条记录即一次生图任务） */
@Data
public class OutfitImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（即生图任务 ID） */
    private Long id;
    private Long userId;
    /** 关联推荐记录 ID */
    private Long outfitRecordId;
    /** 状态：0=生成中 1=成功 2=失败 3=超时 */
    private Integer status;
    /** 效果图存储 key（RustFS） */
    private String imageKey;
    /** 生成用提示词（复盘/重试） */
    private String prompt;
    /** 失败原因 */
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
