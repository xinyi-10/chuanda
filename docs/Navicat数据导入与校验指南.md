# Navicat 数据导入与校验指南

> 目标系统：MySQL 8 的 `life_manager` 库（阶段一 `sql/schema.sql` 建立的库）
> 适用：把 Navicat 中的源数据完整、无损地迁入本系统，并验证可用
> 结论先行：**首选「SQL 转储 → 运行 SQL 文件」，次选「数据传输」，表数据零散时才用 CSV/Excel**

---

## 一、先明确三件事

| 项 | 说明 |
|---|---|
| 目标库 | `life_manager`（若改名，同步改导入语句与连接配置） |
| 目标字符集 | **必须 utf8mb4**，否则中文、emoji 乱码（这是最高频的坑） |
| 目标表结构 | 以 `sql/schema.sql` 为准；源数据字段与之不一致时需先做**字段映射** |

---

## 二、支持的文件格式

### 2.1 Navicat 导入向导支持的格式

| 格式 | 扩展名 | 适用场景 | 推荐度 |
|---|---|---|---|
| SQL 脚本 | `.sql` | 结构 + 数据一体，跨库/跨服务器迁移首选 | ★★★ |
| CSV | `.csv` | 单表纯数据，Excel 友好，通用性最好 | ★★ |
| Excel | `.xlsx` / `.xls` | 手工整理过的数据 | ★★ |
| 分隔文本 | `.txt` | 自定义分隔符（如 `\t`、`\|`） | ★ |
| JSON | `.json` | 接口/程序导出的数据 | ★ |
| XML | `.xml` | 老系统交换格式 | ★ |
| DBF | `.dbf` | 早期 dBase/FoxPro 数据 | ★ |

### 2.2 MySQL 命令行原生的格式

| 格式 | 方式 | 说明 |
|---|---|---|
| `.sql` | `mysql < file.sql` 或 `source file.sql` | 最稳，结构数据一起 |
| `.csv` | `LOAD DATA LOCAL INFILE` | 大批量快，但需注意分隔符/编码 |

> **结论**：要「结构 + 数据」一起迁，用 `.sql`；只有单表数据要灌，用 `.csv` / Excel。

---

## 三、三条导入路径（按你的数据形态选一条）

### 路径 A｜SQL 转储导入（推荐 ⭐）

**适用**：源表结构与目标一致，或整库迁移。

**步骤：**

1. Navicat 连上**源**连接 → 右键库（或表）→ **转储 SQL 文件** → 选 **结构和数据**。
2. 保存为 `.sql`，编码选 **UTF-8**。
3. Navicat 连上**目标** `life_manager` → 右键连接/库 → **运行 SQL 文件** → 选刚导出的 `.sql` → 开始执行。
4. 观察日志无不报错即成功。

**等价命令行（更快）：**

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p life_manager < "D:/path/to/dump.sql"
```

### 路径 B｜Navicat 数据传输 / 数据同步

**适用**：源库和目标库都能在 Navicat 里连上（同实例跨库，或跨服务器）。

**步骤：**

1. 菜单 **工具 → 数据传输**。
2. 选**源**连接+库 → 选**目标**连接+库（`life_manager`）。
3. 勾选要迁的表 → 高级选项：**遇错继续 / 遇重复跳过** 按需勾选。
4. 点 **开始**。

> 增量场景用 **工具 → 数据同步 / 结构同步**，可先「比对」再「同步」，比全量导入安全。

### 路径 C｜CSV / Excel 导入（手工整理的数据）

**适用**：只有单表数据、且是表格形态。

**步骤：**

1. 准备文件：**首行为列名**，与目标表字段对应；编码 **UTF-8（含 BOM 更稳）**。
2. 目标表右键 → **导入向导** → 选 `.csv` / `.xlsx`。
3. 设置：分隔符（`,`）、**首行作为字段名**、编码 UTF-8。
4. **字段映射**：源列 → 目标列一一对应（这一步最容易错，务必逐列核对）。
5. 选择导入模式：**追加 / 更新 / 覆盖**，然后开始。

### 路径 D｜命令行批量（自动化 / 可重复）

```bash
# 1) 先建库建表（干净环境）
mysql -h 127.0.0.1 -P 3306 -u root -p < "D:/AI/workbuddy/ChuanDa/sql/schema.sql"

# 2) 导入种子数据（字典 + 默认分类）
mysql -h 127.0.0.1 -P 3306 -u root -p life_manager < "D:/AI/workbuddy/ChuanDa/sql/data.sql"

# 3) 导入你自己的业务数据（Navicat 转储或手工整理的 .sql）
mysql -h 127.0.0.1 -P 3306 -u root -p life_manager < "D:/path/to/your_data.sql"
```

或在 mysql 会话内：

```sql
USE life_manager;
source D:/path/to/your_data.sql;
```

---

## 四、导入后校验（6 步闭环，逐项打勾）

### ① 结构校验

```sql
USE life_manager;
SHOW TABLES;                                  -- 应看到 9 张表
SHOW CREATE TABLE ledger_record\G             -- 核对字段/类型/COMMENT/索引
```

核对要点：字段名与 `schema.sql` 一致、`amount` 是 `DECIMAL(12,2)`、通用字段齐全、`uk_`/`idx_` 索引在位。

### ② 行数比对（源 ↔ 目标）

```sql
SELECT 'note' AS tbl, COUNT(*) AS cnt FROM note
UNION ALL SELECT 'note_category',  COUNT(*) FROM note_category
UNION ALL SELECT 'ledger_record',  COUNT(*) FROM ledger_record
UNION ALL SELECT 'ledger_category',COUNT(*) FROM ledger_category
UNION ALL SELECT 'clothing_item',  COUNT(*) FROM clothing_item
UNION ALL SELECT 'outfit_record',  COUNT(*) FROM outfit_record
UNION ALL SELECT 'async_task',     COUNT(*) FROM async_task
UNION ALL SELECT 'sys_user',       COUNT(*) FROM sys_user
UNION ALL SELECT 'sys_dict',       COUNT(*) FROM sys_dict;
```

> 也可用 Navicat **工具 → 表数据对比**，直接给出差异行。

### ③ 编码校验（中文 / emoji 不乱码）

```sql
SHOW VARIABLES LIKE 'character_set_database';   -- 应为 utf8mb4
SELECT id, title FROM note WHERE title LIKE '%测试%' LIMIT 5;
```

### ④ 业务完整性校验（必查）

```sql
-- 孤儿数据：引用了不存在的分类
SELECT COUNT(*) AS orphan_note FROM note n
  LEFT JOIN note_category c ON n.category_id = c.id
  WHERE n.category_id IS NOT NULL AND c.id IS NULL AND n.deleted = 0;

-- 金额汇总（源库与目标库应对得上）
SELECT SUM(amount) AS total_expense FROM ledger_record r
  JOIN ledger_category c ON r.category_id = c.id
  WHERE c.type = 1 AND r.deleted = 0;

-- 必填字段是否为空
SELECT COUNT(*) AS bad_rows FROM ledger_record
  WHERE user_id IS NULL OR amount IS NULL OR record_time IS NULL;
```

### ⑤ 抽样人工比对

从源库和目标库各随机抽 5–10 条，**逐字段**对照（尤其中文文本、金额、时间）。

```sql
SELECT * FROM ledger_record ORDER BY RAND() LIMIT 10;
```

### ⑥ 应用层验证

启动 SSM 后端 → 调接口看真实返回：

```bash
curl "http://localhost:8080/api/ledger/stat/overview?month=2026-10" -H "Authorization: Bearer <token>"
```

返回的 `totalIncome / totalExpense / balance` 与 SQL 汇总一致，即链路打通。

---

## 五、常见坑与对策

| # | 坑 | 现象 | 对策 |
|---|---|---|---|
| 1 | 字符集不对 | 中文/emoji 变 `???` 或乱码 | 建库 `utf8mb4`；导出、导入都选 UTF-8 |
| 2 | 日期格式歧义 | `2/4/2026` 被解析成 4 月 2 日或报错 | 导入时显式指定 `yyyy-MM-dd HH:mm:ss` |
| 3 | 主键冲突 | `Duplicate entry '1' for key 'PRIMARY'` | 目标先 `TRUNCATE`，或导出时**去掉自增 id** |
| 4 | 金额失真 | `0.1` 变 `0.0999999` | 目标列用 `DECIMAL`；源数据避免科学计数/浮点 |
| 5 | `LOAD DATA` 被拒 | `ERROR 1290 secure_file_priv` | 用 `LOAD DATA LOCAL INFILE` 或改 MySQL 配置 |
| 6 | 大小写敏感 | Windows 能跑、Linux 报表不存在 | 表名统一小写（本项目已全小写） |
| 7 | 自增 ID 跳号 | 导入后 id 不连续 | 正常现象，不影响使用 |
| 8 | 时区偏移 | 时间差 8 小时 | 连接串加 `serverTimezone=Asia/Shanghai` |

---

## 六、给你的最短路径（针对本项目）

```bash
# ① 建库建表
mysql -u root -p < "D:/AI/workbuddy/ChuanDa/sql/schema.sql"
# ② 种子数据（字典 + 默认分类）
mysql -u root -p life_manager < "D:/AI/workbuddy/ChuanDa/sql/data.sql"
# ③ 你的 Navicat 数据（先转储为 sql，再执行）
mysql -u root -p life_manager < "D:/path/to/navicat_dump.sql"
# ④ 校验
mysql -u root -p life_manager -e "SELECT COUNT(*) FROM ledger_record; SHOW CREATE TABLE ledger_record\G"
```

---

*本指南对应阶段一「数据库搭建」的落地环节，可与其他 docs 一并归档。*
