-- =====================================================================
-- 个人生活管理软件 · 增量脚本 V8：角色形象参考图（图生图 / 角色一致性）
-- 说明：
--   1. body_profile 新增 avatar_image_id 字段，指向某张「穿搭效果图」
--      （outfit_image.id），作为 AI 生图的「角色形象参考图」。
--   2. 生图时若 avatar_image_id 非空，则走 MiniMax 图生图（subject_reference），
--      保证每次生成的是「同一个角色换衣服」；为空则走文生图。
-- =====================================================================

USE `life_manager`;

SET NAMES utf8mb4;

ALTER TABLE `body_profile`
  ADD COLUMN `avatar_image_id` BIGINT DEFAULT NULL COMMENT '角色形象参考图ID(关联outfit_image.id)' AFTER `style_pref`;

-- =====================================================================
-- 结束
-- =====================================================================
