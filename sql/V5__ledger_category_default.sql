-- =====================================================================
-- 个人生活管理软件 · 增量脚本 V5：补齐默认记账分类
-- 说明：
--   分类是按用户存储的（ledger_category.user_id），新用户注册时为空，
--   会导致「选不到分类 → 账单无法添加」。
--   本脚本为**所有尚无分类的用户**补齐一套默认收支分类（幂等）。
--
--   注：后端已加「懒加载兜底」（首次查询分类时自动补齐），
--       本脚本用于批量修正历史数据，二者互不冲突。
-- =====================================================================

USE `life_manager`;

SET NAMES utf8mb4;

-- 为每个无分类的用户，按模板批量插入 12 项支出 + 6 项收入
INSERT INTO `ledger_category`
    (`user_id`, `name`, `type`, `icon`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT u.id, t.name, t.type, t.icon, t.sort, NOW(), NOW(), 0
FROM `sys_user` u
CROSS JOIN (
    /* ===== 支出（type=1）===== */
    SELECT '餐饮'     AS name, 1 AS type, 'food'          AS icon, 1  AS sort
    UNION ALL SELECT '交通',     1, 'traffic',        2
    UNION ALL SELECT '购物',     1, 'shopping',       3
    UNION ALL SELECT '车票',     1, 'ticket',         4
    UNION ALL SELECT '居住',     1, 'home',           5
    UNION ALL SELECT '通讯',     1, 'communication',  6
    UNION ALL SELECT '娱乐',     1, 'entertainment',  7
    UNION ALL SELECT '医疗',     1, 'medical',        8
    UNION ALL SELECT '教育',     1, 'education',      9
    UNION ALL SELECT '服饰',     1, 'clothing',      10
    UNION ALL SELECT '人情',     1, 'social',        11
    UNION ALL SELECT '其他支出', 1, 'other_expense', 12
    /* ===== 收入（type=2）===== */
    UNION ALL SELECT '工资',     2, 'salary',         1
    UNION ALL SELECT '奖金',     2, 'bonus',          2
    UNION ALL SELECT '兼职',     2, 'parttime',       3
    UNION ALL SELECT '理财',     2, 'finance',        4
    UNION ALL SELECT '红包',     2, 'redpacket',      5
    UNION ALL SELECT '其他收入', 2, 'other_income',   6
) t
WHERE u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `ledger_category` c
      WHERE c.user_id = u.id
        AND c.name = t.name
        AND c.type = t.type
        AND c.deleted = 0
  );

-- =====================================================================
-- 结束
-- =====================================================================
