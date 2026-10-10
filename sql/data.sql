-- =====================================================================
-- 个人生活管理软件 · 初始化数据脚本 (DML)
-- 说明：
--   1. sys_dict 为全局字典，预置 season / style / clothing_category 三类枚举。
--   2. 记账分类、笔记分类为「按用户」数据，此处以 user_id = 1 示例；
--      正式使用时改为注册流程中为新用户自动初始化，或按需调整 user_id。
--   3. 可重复执行（先清空再插入）。
-- =====================================================================

USE `life_manager`;

SET NAMES utf8mb4;

-- =====================================================================
-- 一、数据字典（全局）
-- =====================================================================

DELETE FROM `sys_dict` WHERE `dict_type` IN ('season', 'style', 'clothing_category');

-- 季节
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`) VALUES
('season', 'spring', '春',   1),
('season', 'summer', '夏',   2),
('season', 'autumn', '秋',   3),
('season', 'winter', '冬',   4),
('season', 'all',    '四季', 5);

-- 风格
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`) VALUES
('style', 'casual',   '休闲', 1),
('style', 'formal',   '正式', 2),
('style', 'sport',    '运动', 3),
('style', 'business', '商务', 4);

-- 衣物类别
INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort`) VALUES
('clothing_category', 'top',       '上装', 1),
('clothing_category', 'bottom',    '下装', 2),
('clothing_category', 'outer',     '外套', 3),
('clothing_category', 'shoes',     '鞋',   4),
('clothing_category', 'accessory', '配饰', 5);

-- =====================================================================
-- 二、记账分类（user_id = 1 示例）
-- =====================================================================

DELETE FROM `ledger_category` WHERE `user_id` = 1;

-- 支出分类
INSERT INTO `ledger_category` (`user_id`, `name`, `type`, `icon`, `sort`) VALUES
(1, '餐饮', 1, 'food',     1),
(1, '交通', 1, 'traffic',  2),
(1, '购物', 1, 'shopping', 3),
(1, '居住', 1, 'home',     4),
(1, '娱乐', 1, 'game',     5),
(1, '医疗', 1, 'medical',  6);

-- 收入分类
INSERT INTO `ledger_category` (`user_id`, `name`, `type`, `icon`, `sort`) VALUES
(1, '工资', 2, 'salary',  1),
(1, '兼职', 2, 'parttime', 2),
(1, '理财', 2, 'finance', 3);

-- =====================================================================
-- 三、笔记分类（user_id = 1 示例）
-- =====================================================================

DELETE FROM `note_category` WHERE `user_id` = 1;

INSERT INTO `note_category` (`user_id`, `name`, `sort`) VALUES
(1, '工作', 1),
(1, '生活', 2),
(1, '学习', 3),
(1, '灵感', 4);

-- =====================================================================
-- 结束
-- =====================================================================
