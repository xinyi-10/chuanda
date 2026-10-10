package com.life.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 事务待办实体（对应表 todo），带倒计时能力 */
@Data
public class Todo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String title;
    private String content;

    /** 倒计时目标结束时刻（截止时间 / 开始时间+时长 统一落到这里） */
    private LocalDateTime deadline;
    /** 初始倒计时总时长（秒），重置时使用 */
    private Integer duration;
    /** 暂停时的剩余秒数快照 */
    private Integer remainSec;

    /** 状态：0=进行中 1=已暂停 2=已完成 3=已到期 */
    private Integer status;
    /** 优先级：0=普通 1=重要 2=紧急 */
    private Integer priority;

    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 非表字段：服务端计算的剩余秒数（列表回传，便于前端首屏渲染） */
    private Long remainSeconds;
}
