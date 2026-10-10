-- =====================================================================
-- 个人生活管理软件 · 增量脚本 V3：支付凭证管理 + 固定收入配置
-- 说明：
--   1. receipt_category —— 凭证分类字典（首个页面展示「适用场景 / 特点」）。
--   2. receipt_image    —— 上传的支付凭证图片，含审核、金额、时间与去重指纹。
--   3. income_config    —— 用户固定收入（月工资）配置，一人一条。
--   4. 去重：md5 精确判重 + phash 感知哈希（近似判重）。
--   5. 修改次数：自然月重置，每月最多 2 次；首次填写不计次。
-- =====================================================================

USE `life_manager`;

SET NAMES utf8mb4;

-- =====================================================================
-- 八、支付凭证与固定收入模块
-- =====================================================================

-- 1. 凭证分类字典（全局预置，首个页面用于导览说明）
DROP TABLE IF EXISTS `receipt_category`;
CREATE TABLE `receipt_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(30)  NOT NULL COMMENT '分类编码',
  `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称',
  `scene`       VARCHAR(200) DEFAULT NULL COMMENT '适用场景',
  `feature`     VARCHAR(200) DEFAULT NULL COMMENT '特点说明',
  `keywords`    VARCHAR(300) DEFAULT NULL COMMENT '识别关键词(逗号分隔)',
  `sort`        INT          DEFAULT 0 COMMENT '显示顺序',
  `enabled`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用: 1=启用 0=停用',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证分类字典表';

-- 2. 支付凭证图片表
DROP TABLE IF EXISTS `receipt_image`;
CREATE TABLE `receipt_image` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT        NOT NULL COMMENT '归属用户ID',
  `category_id`  BIGINT        DEFAULT NULL COMMENT '分类ID',
  `file_name`    VARCHAR(255)  NOT NULL COMMENT '原始文件名',
  `file_path`    VARCHAR(500)  NOT NULL COMMENT '存储相对路径',
  `file_size`    BIGINT        DEFAULT NULL COMMENT '文件字节数',
  `md5`          CHAR(32)      NOT NULL COMMENT '内容MD5(精确去重)',
  `phash`        VARCHAR(32)   DEFAULT NULL COMMENT '感知哈希(aHash,近似去重)',
  `audit_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态: 0=待审核 1=通过 2=不通过',
  `audit_msg`    VARCHAR(255)  DEFAULT NULL COMMENT '审核说明',
  `pay_amount`   DECIMAL(12,2) DEFAULT NULL COMMENT '识别出的支付金额',
  `pay_time`     DATETIME      DEFAULT NULL COMMENT '识别出的支付时间',
  `time_source`  TINYINT       DEFAULT NULL COMMENT '时间来源: 1=自动识别 2=手动补录',
  `remark`       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_md5` (`user_id`, `md5`),
  KEY `idx_user_category` (`user_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付凭证图片表';

-- 3. 固定收入配置表（一人一条）
DROP TABLE IF EXISTS `income_config`;
CREATE TABLE `income_config` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           BIGINT        NOT NULL COMMENT '归属用户ID',
  `monthly_amount`    DECIMAL(12,2) NOT NULL COMMENT '每月收入金额',
  `pay_day`           TINYINT       NOT NULL COMMENT '每月发薪日(1-31)',
  `delay_days`        TINYINT       NOT NULL DEFAULT 0 COMMENT '可能延迟天数',
  `modify_count`      TINYINT       NOT NULL DEFAULT 0 COMMENT '当月已修改次数',
  `last_modify_month` VARCHAR(7)    DEFAULT NULL COMMENT '最后修改月份(YYYY-MM,自然月重置依据)',
  `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           TINYINT       NOT NULL DEFAULT 0 COMMENT '删除标记: 0=正常 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定收入配置表';

-- =====================================================================
-- 凭证分类初始化数据
-- =====================================================================

INSERT INTO `receipt_category` (`code`, `name`, `scene`, `feature`, `keywords`, `sort`) VALUES
('salary',  '工资条',   '公司发放的工资明细截图',      '含「工资/实发」字样，金额较大、周期固定', '工资,薪资,实发,发放', 1),
('bank',    '银行转账', '网银 / 手机银行转账成功截图',  '含「转账成功」，带对方账户与流水号',      '转账成功,付款成功,转账', 2),
('payment', '支付平台', '微信 / 支付宝支付成功截图',    '含「支付成功」，带商户名与订单号',        '支付成功,付款成功,已支付,已付款', 3),
('invoice', '电子发票', '增值税电子发票 / 电子票据',    '含发票号、税额、开票日期',                '发票,税额,开票,价税合计', 4),
('ticket',  '收据小票', '线下消费小票 / 手写收据',      '金额通常较小，多数无电子版',              '收据,小票,消费,收款', 5),
('other',   '其他',     '无法归入以上类别时使用',        '兜底分类，需人工确认',                    '', 99);

-- =====================================================================
-- 结束
-- =====================================================================
