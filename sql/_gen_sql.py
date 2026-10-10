# -*- coding: utf-8 -*-
"""生成 schema.sql / data.sql —— 严格复刻 woye_gl 项目 property_management.sql 的建表风格"""
import os

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
BAR = "-- " + "=" * 69

def block(title):
    return f"{BAR}\n-- {title}\n{BAR}"

def render_table(no, name, title, comment, fields, keys, notes=None):
    w1 = max(len("`" + f[0] + "`") for f in fields) + 1
    w2 = max(len(f[1]) for f in fields) + 1
    lines = [f"-- {no}. {title}"]
    if notes:
        for n in notes:
            lines.append(f"--    {n}")
    lines.append(f"DROP TABLE IF EXISTS `{name}`;")
    lines.append(f"CREATE TABLE `{name}` (")
    for fn, ft, fc, fm in fields:
        cell = ("`" + fn + "`").ljust(w1)
        typ = ft.ljust(w2)
        lines.append(f"  {cell}{typ}{fc} COMMENT '{fm}',")
    for i, k in enumerate(keys):
        lines.append(f"  {k}" + ("," if i < len(keys) - 1 else ""))
    lines.append(f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{comment}';")
    return "\n".join(lines)

# ============================ 表定义 ============================
tables = []

# ---- 一、用户与鉴权模块 ----
tables.append(("group", "一、用户与鉴权模块"))
tables.append(("table", dict(
    no=1, name="sys_user", title="用户表", comment="用户表",
    fields=[
        ("id",              "BIGINT",       "NOT NULL AUTO_INCREMENT", "主键"),
        ("username",        "VARCHAR(50)",  "NOT NULL",                "登录账号"),
        ("password",        "VARCHAR(100)", "NOT NULL",                "密码(BCrypt加密)"),
        ("nickname",        "VARCHAR(50)",  "DEFAULT NULL",            "昵称"),
        ("email",           "VARCHAR(100)", "DEFAULT NULL",            "邮箱"),
        ("phone",           "VARCHAR(20)",  "DEFAULT NULL",            "手机号"),
        ("avatar",          "VARCHAR(255)", "DEFAULT NULL",            "头像URL"),
        ("status",          "TINYINT",      "NOT NULL DEFAULT 1",      "状态: 1=启用 0=禁用"),
        ("last_login_time", "DATETIME",     "DEFAULT NULL",            "最后登录时间"),
        ("create_time",     "DATETIME",     "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        ("update_time",     "DATETIME",     "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        ("deleted",         "TINYINT",      "NOT NULL DEFAULT 0",      "删除标记: 0=正常 1=已删除"),
    ],
    keys=["PRIMARY KEY (`id`)", "UNIQUE KEY `uk_username` (`username`)", "KEY `idx_phone` (`phone`)"],
)))

# ---- 二、字典模块 ----
tables.append(("group", "二、数据字典模块"))
tables.append(("table", dict(
    no=2, name="sys_dict", title="数据字典表", comment="数据字典表",
    notes=["统一维护 season / style / clothing_category 等枚举，杜绝自由文本"],
    fields=[
        ("id",          "BIGINT",      "NOT NULL AUTO_INCREMENT", "主键"),
        ("dict_type",   "VARCHAR(30)", "NOT NULL",                "字典类型(season/style/clothing_category)"),
        ("dict_code",   "VARCHAR(30)", "NOT NULL",                "字典编码(如 summer/casual/top)"),
        ("dict_label",  "VARCHAR(50)", "NOT NULL",                "字典标签(如 夏/休闲/上装)"),
        ("sort",        "INT",         "DEFAULT 0",               "显示顺序"),
        ("create_time", "DATETIME",    "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ],
    keys=["PRIMARY KEY (`id`)", "UNIQUE KEY `uk_type_code` (`dict_type`, `dict_code`)"],
)))

# ---- 三、笔记模块 ----
tables.append(("group", "三、笔记模块"))
tables.append(("table", dict(
    no=3, name="note_category", title="笔记分类表", comment="笔记分类表",
    fields=[
        ("id",          "BIGINT",      "NOT NULL AUTO_INCREMENT", "主键"),
        ("user_id",     "BIGINT",      "NOT NULL",                "归属用户ID"),
        ("name",        "VARCHAR(50)", "NOT NULL",                "分类名称"),
        ("sort",        "INT",         "DEFAULT 0",               "显示顺序"),
        ("create_time", "DATETIME",    "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        ("update_time", "DATETIME",    "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        ("deleted",     "TINYINT",     "NOT NULL DEFAULT 0",      "删除标记: 0=正常 1=已删除"),
    ],
    keys=["PRIMARY KEY (`id`)",
          "UNIQUE KEY `uk_user_name` (`user_id`, `name`, `deleted`)",
          "KEY `idx_user_id` (`user_id`)"],
)))
tables.append(("table", dict(
    no=4, name="note", title="笔记表", comment="笔记表",
    notes=["搜索方案：先用 LIKE 模糊匹配；单表数据量超过阈值后改 FULLTEXT（需 ngram 解析器）"],
    fields=[
        ("id",          "BIGINT",       "NOT NULL AUTO_INCREMENT", "主键"),
        ("user_id",     "BIGINT",       "NOT NULL",                "归属用户ID"),
        ("category_id", "BIGINT",       "DEFAULT NULL",            "分类ID(NULL=未分类)"),
        ("title",       "VARCHAR(200)", "NOT NULL",                "标题"),
        ("content",     "TEXT",         "DEFAULT NULL",            "正文内容"),
        ("create_time", "DATETIME",     "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        ("update_time", "DATETIME",     "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        ("deleted",     "TINYINT",      "NOT NULL DEFAULT 0",      "删除标记: 0=正常 1=已删除"),
    ],
    keys=["PRIMARY KEY (`id`)",
          "KEY `idx_user_category` (`user_id`, `category_id`)",
          "KEY `idx_title` (`title`)"],
)))

# ---- 四、记账模块 ----
tables.append(("group", "四、记账模块"))
tables.append(("table", dict(
    no=5, name="ledger_category", title="记账分类表", comment="记账分类表",
    fields=[
        ("id",          "BIGINT",      "NOT NULL AUTO_INCREMENT", "主键"),
        ("user_id",     "BIGINT",      "NOT NULL",                "归属用户ID"),
        ("name",        "VARCHAR(50)", "NOT NULL",                "分类名称(餐饮/交通/工资…)"),
        ("type",        "TINYINT",     "NOT NULL",                "类型: 1=支出 2=收入"),
        ("icon",        "VARCHAR(50)", "DEFAULT NULL",            "图标标识"),
        ("sort",        "INT",         "DEFAULT 0",               "显示顺序"),
        ("create_time", "DATETIME",    "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        ("update_time", "DATETIME",    "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        ("deleted",     "TINYINT",     "NOT NULL DEFAULT 0",      "删除标记: 0=正常 1=已删除"),
    ],
    keys=["PRIMARY KEY (`id`)",
          "UNIQUE KEY `uk_user_name_type` (`user_id`, `name`, `type`, `deleted`)",
          "KEY `idx_user_type` (`user_id`, `type`)"],
)))
tables.append(("table", dict(
    no=6, name="ledger_record", title="记账流水表", comment="记账流水表",
    notes=["收支类型不冗余存储，统一由 ledger_category.type 派生（审核改进 H6）",
           "应用层写入时强校验「流水所属分类的 type」，避免收入流水挂到支出分类"],
    fields=[
        ("id",          "BIGINT",        "NOT NULL AUTO_INCREMENT", "主键"),
        ("user_id",     "BIGINT",        "NOT NULL",                "归属用户ID"),
        ("category_id", "BIGINT",        "NOT NULL",                "分类ID(收支类型由分类派生)"),
        ("amount",      "DECIMAL(12,2)", "NOT NULL",                "金额(正数,收支由分类type判定)"),
        ("record_time", "DATETIME",      "NOT NULL",                "发生时间"),
        ("remark",      "VARCHAR(255)",  "DEFAULT NULL",            "备注"),
        ("create_time", "DATETIME",      "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        ("update_time", "DATETIME",      "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        ("deleted",     "TINYINT",       "NOT NULL DEFAULT 0",      "删除标记: 0=正常 1=已删除"),
    ],
    keys=["PRIMARY KEY (`id`)",
          "KEY `idx_user_time` (`user_id`, `record_time`)",
          "KEY `idx_user_category` (`user_id`, `category_id`)"],
)))

# ---- 五、穿搭推荐模块 ----
tables.append(("group", "五、穿搭推荐模块"))
tables.append(("table", dict(
    no=7, name="clothing_item", title="衣物表", comment="衣物表",
    notes=["category / season / style 取值受 sys_dict 字典约束（审核改进 L5）"],
    fields=[
        ("id",          "BIGINT",       "NOT NULL AUTO_INCREMENT", "主键"),
        ("user_id",     "BIGINT",       "NOT NULL",                "归属用户ID"),
        ("name",        "VARCHAR(100)", "NOT NULL",                "衣物名称"),
        ("category",    "VARCHAR(30)",  "NOT NULL",                "类别(字典: clothing_category)"),
        ("color",       "VARCHAR(30)",  "DEFAULT NULL",            "主色"),
        ("season",      "VARCHAR(30)",  "DEFAULT NULL",            "适用季节(字典: season)"),
        ("style",       "VARCHAR(30)",  "DEFAULT NULL",            "风格标签(字典: style)"),
        ("image_url",   "VARCHAR(255)", "DEFAULT NULL",            "图片URL"),
        ("create_time", "DATETIME",     "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        ("update_time", "DATETIME",     "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
        ("deleted",     "TINYINT",      "NOT NULL DEFAULT 0",      "删除标记: 0=正常 1=已删除"),
    ],
    keys=["PRIMARY KEY (`id`)",
          "KEY `idx_user_category` (`user_id`, `category`)"],
)))
tables.append(("table", dict(
    no=8, name="outfit_record", title="穿搭推荐记录表", comment="穿搭推荐记录表",
    notes=["items_json 存快照(id+name+category 冗余)，避免衣物删除后历史出现死ID（审核改进 M3）"],
    fields=[
        ("id",          "BIGINT",       "NOT NULL AUTO_INCREMENT", "主键"),
        ("user_id",     "BIGINT",       "NOT NULL",                "归属用户ID"),
        ("occasion",    "VARCHAR(50)",  "DEFAULT NULL",            "场合"),
        ("weather",     "VARCHAR(100)", "DEFAULT NULL",            "天气快照"),
        ("items_json",  "TEXT",         "DEFAULT NULL",            "推荐衣物快照(JSON,含id/name/category)"),
        ("advice",      "TEXT",         "DEFAULT NULL",            "推荐建议文本"),
        ("source",      "TINYINT",      "NOT NULL DEFAULT 1",      "来源: 1=AI 2=规则兜底"),
        ("create_time", "DATETIME",     "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
    ],
    keys=["PRIMARY KEY (`id`)",
          "KEY `idx_user_time` (`user_id`, `create_time`)"],
)))

# ---- 六、异步任务模块 ----
tables.append(("group", "六、异步任务模块"))
tables.append(("table", dict(
    no=9, name="async_task", title="异步任务表", comment="异步任务表",
    notes=["状态机含终态：4=超时 5=取消（审核改进 L4），杜绝「永久执行中」",
           "timeout_at 供定时补偿任务扫描 status=1 且已超时的任务"],
    fields=[
        ("id",          "BIGINT",       "NOT NULL AUTO_INCREMENT", "主键(即 taskId)"),
        ("user_id",     "BIGINT",       "NOT NULL",                "归属用户ID"),
        ("type",        "VARCHAR(30)",  "NOT NULL",                "任务类型(如 OUTFIT_RECOMMEND)"),
        ("status",      "TINYINT",      "NOT NULL DEFAULT 0",      "状态: 0=排队 1=执行中 2=成功 3=失败 4=超时 5=取消"),
        ("result_ref",  "BIGINT",       "DEFAULT NULL",            "结果关联ID(如 outfit_record.id)"),
        ("error_msg",   "VARCHAR(500)", "DEFAULT NULL",            "失败原因"),
        ("timeout_at",  "DATETIME",     "DEFAULT NULL",            "超时时间点(定时任务据此判超时)"),
        ("create_time", "DATETIME",     "DEFAULT CURRENT_TIMESTAMP", "创建时间"),
        ("finish_time", "DATETIME",     "DEFAULT NULL",            "完成时间(含超时/取消终态)"),
    ],
    keys=["PRIMARY KEY (`id`)",
          "KEY `idx_user_time` (`user_id`, `create_time`)",
          "KEY `idx_status_timeout` (`status`, `timeout_at`)"],
)))

# ============================ 组装 schema.sql ============================
HEADER = f"""{BAR}
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
{BAR}

CREATE DATABASE IF NOT EXISTS `life_manager` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `life_manager`;

SET NAMES utf8mb4;
"""

parts = [HEADER]
for item in tables:
    if item[0] == "group":
        parts.append("\n" + block(item[1]) + "\n")
    else:
        t = item[1]
        parts.append(render_table(t["no"], t["name"], t["title"], t["comment"],
                                  t["fields"], t["keys"], t.get("notes")) + "\n")

parts.append("\n" + block("结束") + "\n")
schema = "\n".join(parts)
with open(os.path.join(OUT_DIR, "schema.sql"), "w", encoding="utf-8") as fp:
    fp.write(schema)

# ============================ 组装 data.sql ============================
DATA = f"""{BAR}
-- 个人生活管理软件 · 初始化数据脚本 (DML)
-- 说明：
--   1. sys_dict 为全局字典，预置 season / style / clothing_category 三类枚举。
--   2. 记账分类、笔记分类为「按用户」数据，此处以 user_id = 1 示例；
--      正式使用时改为注册流程中为新用户自动初始化，或按需调整 user_id。
--   3. 可重复执行（先清空再插入）。
{BAR}

USE `life_manager`;

SET NAMES utf8mb4;

{BAR}
-- 一、数据字典（全局）
{BAR}

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

{BAR}
-- 二、记账分类（user_id = 1 示例）
{BAR}

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

{BAR}
-- 三、笔记分类（user_id = 1 示例）
{BAR}

DELETE FROM `note_category` WHERE `user_id` = 1;

INSERT INTO `note_category` (`user_id`, `name`, `sort`) VALUES
(1, '工作', 1),
(1, '生活', 2),
(1, '学习', 3),
(1, '灵感', 4);

{BAR}
-- 结束
{BAR}
"""
with open(os.path.join(OUT_DIR, "data.sql"), "w", encoding="utf-8") as fp:
    fp.write(DATA)

print("OK -> schema.sql / data.sql")
