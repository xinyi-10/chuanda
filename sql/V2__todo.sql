-- =====================================================================
-- 个人生活管理软件 · 增量脚本 V2：事务待办表
-- 说明：
--   1. 新增 todo 表，支持「截止时间 / 自定义时长」两种倒计时模式。
--   2. deadline 为统一的目标结束时刻；duration 为初始总秒数（重置时用）。
--   3. 暂停时把剩余秒数快照进 remain_sec，恢复时 deadline = now + remain_sec。
--   4. 状态机：0=进行中 1=已暂停 2=已完成 3=已到期。
-- =====================================================================

USE `life_manager`;

SET NAMES utf8mb4;

-- =====================================================================
-- 七、事务待办模块
-- =====================================================================

-- 1. 事务待办表
DROP TABLE IF EXISTS `todo`;
CREATE TABLE `todo` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT        NOT NULL COMMENT '归属用户ID',
  `title`       VARCHAR(200)  NOT NULL COMMENT '待办标题',
  `content`     VARCHAR(1000) DEFAULT NULL COMMENT '待办内容',
  `deadline`    DATETIME      DEFAULT NULL COMMENT '倒计时目标结束时刻',
  `duration`    INT           DEFAULT NULL COMMENT '初始倒计时总时长(秒)',
  `remain_sec`  INT           DEFAULT NULL COMMENT '暂停时剩余秒数快照',
  `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0=进行中 1=已暂停 2=已完成 3=已到期',
  `priority`    TINYINT       NOT NULL DEFAULT 0 COMMENT '优先级: 0=普通 1=重要 2=紧急',
  `finish_time` DATETIME      DEFAULT NULL COMMENT '完成/到期时间',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务待办表';

-- =====================================================================
-- 结束
-- =====================================================================
