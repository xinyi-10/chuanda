-- =====================================================================
-- 个人生活管理软件 · MySQL 建表脚本 (DDL)
-- 技术栈：Java + SSM(Spring/SpringMVC/MyBatis) + MySQL
-- 字符集：utf8mb4  /  引擎：InnoDB
--
-- 说明：
--   1. 所有主键使用 BIGINT 自增。
--   2. 金额统一 DECIMAL(12,2)，禁止 FLOAT/DOUBLE。
--   3. 通用字段：user_id / create_time / update_time / deleted（逻辑删除 0/1）。
--   4. 未使用物理外键（采用逻辑关联 + 索引），由应用层保证完整性，
--      这是 SSM 项目的常见做法，便于逻辑删除与水平扩展。
--   5. 所有业务表均带 user_id，配合 MyBatis 拦截器实现数据隔离（P0 铁律）。
--   6. 枚举取值统一走 sys_dict 字典表，种子数据见同目录 data.sql。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `life_manager` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `life_manager`;

SET NAMES utf8mb4;


-- =====================================================================
-- 一、用户与鉴权模块
-- =====================================================================

-- 1. 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`        VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname`        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用 0=禁用',
  `last_login_time` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


-- =====================================================================
-- 二、数据字典模块
-- =====================================================================

-- 2. 数据字典表
--    统一维护 season / style / clothing_category 等枚举，杜绝自由文本
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type`   VARCHAR(30) NOT NULL COMMENT '字典类型(season/style/clothing_category)',
  `dict_code`   VARCHAR(30) NOT NULL COMMENT '字典编码(如 summer/casual/top)',
  `dict_label`  VARCHAR(50) NOT NULL COMMENT '字典标签(如 夏/休闲/上装)',
  `sort`        INT         DEFAULT 0 COMMENT '显示顺序',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`dict_type`, `dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';


-- =====================================================================
-- 三、笔记模块
-- =====================================================================

-- 3. 笔记分类表
DROP TABLE IF EXISTS `note_category`;
CREATE TABLE `note_category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT      NOT NULL COMMENT '归属用户ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort`        INT         DEFAULT 0 COMMENT '显示顺序',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_id`, `name`, `deleted`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记分类表';

-- 4. 笔记表
--    搜索方案：先用 LIKE 模糊匹配；单表数据量超过阈值后改 FULLTEXT（需 ngram 解析器）
DROP TABLE IF EXISTS `note`;
CREATE TABLE `note` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '归属用户ID',
  `category_id` BIGINT       DEFAULT NULL COMMENT '分类ID(NULL=未分类)',
  `title`       VARCHAR(200) NOT NULL COMMENT '标题',
  `content`     TEXT         DEFAULT NULL COMMENT '正文内容',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_category` (`user_id`, `category_id`),
  KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';


-- =====================================================================
-- 四、记账模块
-- =====================================================================

-- 5. 记账分类表
DROP TABLE IF EXISTS `ledger_category`;
CREATE TABLE `ledger_category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT      NOT NULL COMMENT '归属用户ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '分类名称(餐饮/交通/工资…)',
  `type`        TINYINT     NOT NULL COMMENT '类型: 1=支出 2=收入',
  `icon`        VARCHAR(50) DEFAULT NULL COMMENT '图标标识',
  `sort`        INT         DEFAULT 0 COMMENT '显示顺序',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name_type` (`user_id`, `name`, `type`, `deleted`),
  KEY `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账分类表';

-- 6. 记账流水表
--    收支类型不冗余存储，统一由 ledger_category.type 派生（审核改进 H6）
--    应用层写入时强校验「流水所属分类的 type」，避免收入流水挂到支出分类
DROP TABLE IF EXISTS `ledger_record`;
CREATE TABLE `ledger_record` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT        NOT NULL COMMENT '归属用户ID',
  `category_id` BIGINT        NOT NULL COMMENT '分类ID(收支类型由分类派生)',
  `amount`      DECIMAL(12,2) NOT NULL COMMENT '金额(正数,收支由分类type判定)',
  `record_time` DATETIME      NOT NULL COMMENT '发生时间',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `record_time`),
  KEY `idx_user_category` (`user_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账流水表';


-- =====================================================================
-- 五、穿搭推荐模块
-- =====================================================================

-- 7. 衣物表
--    category / season / style 取值受 sys_dict 字典约束（审核改进 L5）
DROP TABLE IF EXISTS `clothing_item`;
CREATE TABLE `clothing_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '归属用户ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '衣物名称',
  `category`    VARCHAR(30)  NOT NULL COMMENT '类别(字典: clothing_category)',
  `color`       VARCHAR(30)  DEFAULT NULL COMMENT '主色',
  `season`      VARCHAR(30)  DEFAULT NULL COMMENT '适用季节(字典: season)',
  `style`       VARCHAR(30)  DEFAULT NULL COMMENT '风格标签(字典: style)',
  `image_url`   VARCHAR(255) DEFAULT NULL COMMENT '图片URL',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_category` (`user_id`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣物表';

-- 8. 穿搭推荐记录表
--    items_json 存快照(id+name+category 冗余)，避免衣物删除后历史出现死ID（审核改进 M3）
DROP TABLE IF EXISTS `outfit_record`;
CREATE TABLE `outfit_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '归属用户ID',
  `occasion`    VARCHAR(50)  DEFAULT NULL COMMENT '场合',
  `weather`     VARCHAR(100) DEFAULT NULL COMMENT '天气快照',
  `items_json`  TEXT         DEFAULT NULL COMMENT '推荐衣物快照(JSON,含id/name/category)',
  `advice`      TEXT         DEFAULT NULL COMMENT '推荐建议文本',
  `source`      TINYINT      NOT NULL DEFAULT 1 COMMENT '来源: 1=AI 2=规则兜底',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='穿搭推荐记录表';


-- =====================================================================
-- 六、异步任务模块
-- =====================================================================

-- 9. 异步任务表
--    状态机含终态：4=超时 5=取消（审核改进 L4），杜绝「永久执行中」
--    timeout_at 供定时补偿任务扫描 status=1 且已超时的任务
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键(即 taskId)',
  `user_id`     BIGINT       NOT NULL COMMENT '归属用户ID',
  `type`        VARCHAR(30)  NOT NULL COMMENT '任务类型(如 OUTFIT_RECOMMEND)',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=排队 1=执行中 2=成功 3=失败 4=超时 5=取消',
  `result_ref`  BIGINT       DEFAULT NULL COMMENT '结果关联ID(如 outfit_record.id)',
  `error_msg`   VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `timeout_at`  DATETIME     DEFAULT NULL COMMENT '超时时间点(定时任务据此判超时)',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finish_time` DATETIME     DEFAULT NULL COMMENT '完成时间(含超时/取消终态)',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_status_timeout` (`status`, `timeout_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';


-- =====================================================================
-- 结束
-- =====================================================================
