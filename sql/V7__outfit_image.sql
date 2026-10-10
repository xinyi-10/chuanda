-- =====================================================================
-- 个人生活管理软件 · 增量脚本 V7：穿搭效果图（AI 生图）
-- 说明：
--   1. 新增 outfit_image 表：记录每次「AI 生图穿搭效果」的任务与结果。
--   2. status 字段自承载任务状态机（0=生成中 1=成功 2=失败 3=超时），
--      由独立 @Component 异步生成器 + 超时补偿定时任务共同驱动，
--      效果图表即任务载体，无需额外引入 async_task 表。
--   3. image_key 指向 RustFS 对象存储（复用凭证/衣物的存储链路）。
-- =====================================================================

USE `life_manager`;

SET NAMES utf8mb4;

-- =====================================================================
-- 十一、穿搭效果图模块
-- =====================================================================

-- 1. 穿搭效果图表
DROP TABLE IF EXISTS `outfit_image`;
CREATE TABLE `outfit_image` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键(即生图任务ID)',
  `user_id`          BIGINT       NOT NULL COMMENT '归属用户ID',
  `outfit_record_id` BIGINT       DEFAULT NULL COMMENT '关联推荐记录ID',
  `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=生成中 1=成功 2=失败 3=超时',
  `image_key`        VARCHAR(255) DEFAULT NULL COMMENT '效果图存储key(RustFS)',
  `prompt`           TEXT         DEFAULT NULL COMMENT '生成用提示词(复盘/重试)',
  `error_msg`        VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finish_time`      DATETIME     DEFAULT NULL COMMENT '完成时间(含失败/超时)',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='穿搭效果图表';

-- =====================================================================
-- 结束
-- =====================================================================
