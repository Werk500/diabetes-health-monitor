-- ============================================================
-- 文章运营增强：防止同一用户重复接收同一篇文章
-- 适用数据库：diabetes_monitor
-- 注意：仅在 sys_user_article 尚无 uk_user_article 唯一索引时执行。
-- ============================================================

USE diabetes_monitor;

-- 加唯一索引前先确认没有重复关系。若查询有结果，应先保留每组最小 id，
-- 删除其余重复项后再执行 ALTER TABLE。
SELECT user_id,
       article_id,
       COUNT(*) AS duplicate_count,
       GROUP_CONCAT(id ORDER BY id) AS duplicate_ids
FROM sys_user_article
GROUP BY user_id, article_id
HAVING COUNT(*) > 1;

ALTER TABLE sys_user_article
    ADD UNIQUE KEY uk_user_article (user_id, article_id);
