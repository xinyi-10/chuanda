-- =====================================================================
-- 个人生活管理软件 · 增量脚本 V6：穿搭推荐（身体数据）
-- 说明：
--   1. 新增 body_profile 表：身高/体重/三围 + 所在城市 + 风格偏好。
--   2. 衣物库 clothing_item、推荐记录 outfit_record、异步任务 async_task
--      已在 schema.sql 中建好，此处不重复创建。
--   3. 所在城市用于获取当地天气；三围用于版型（宽松/修身）建议。
-- =====================================================================

USE `life_manager`;

SET NAMES utf8mb4;

-- =====================================================================
-- 十、穿搭推荐模块
-- =====================================================================

-- 1. 身体数据表（一人一条）
DROP TABLE IF EXISTS `body_profile`;
CREATE TABLE `body_profile` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '归属用户ID',
  `gender`      TINYINT      DEFAULT NULL COMMENT '性别: 1=男 2=女',
  `height`      DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
  `weight`      DECIMAL(5,1) DEFAULT NULL COMMENT '体重(kg)',
  `bust`        DECIMAL(5,1) DEFAULT NULL COMMENT '胸围(cm)',
  `waist`       DECIMAL(5,1) DEFAULT NULL COMMENT '腰围(cm)',
  `hip`         DECIMAL(5,1) DEFAULT NULL COMMENT '臀围(cm)',
  `city`        VARCHAR(50)  DEFAULT NULL COMMENT '所在城市(取天气用)',
  `style_pref`  VARCHAR(30)  DEFAULT NULL COMMENT '风格偏好(字典style: casual/formal/sport/business)',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身体数据表';

-- =====================================================================
-- 结束
-- =====================================================================
