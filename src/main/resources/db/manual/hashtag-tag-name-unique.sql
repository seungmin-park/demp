-- Merge legacy duplicate tag names, then enforce the uniqueness Hashtag.tagName declares.
-- Before applying: stop writes and back up. Name comparison follows the column collation, the same rule
-- the unique index uses. Skip the ALTER if SHOW INDEX FROM hashtag already lists a unique tag_name index.

-- 1. Point every question relation at the smallest hashtag_id of its name.
UPDATE question_hashtag
SET hashtag_id = (
    SELECT MIN(keeper.hashtag_id)
    FROM hashtag keeper
    JOIN hashtag linked ON keeper.tag_name = linked.tag_name
    WHERE linked.hashtag_id = question_hashtag.hashtag_id)
WHERE hashtag_id IN (SELECT hashtag_id FROM hashtag);

-- 2. Keep one relation per (question, hashtag) pair created by step 1. Other questions are untouched.
DELETE FROM question_hashtag
WHERE question_id IS NOT NULL AND hashtag_id IS NOT NULL
  AND id NOT IN (
    SELECT kept_id FROM (
        SELECT MIN(id) AS kept_id FROM question_hashtag
        WHERE question_id IS NOT NULL AND hashtag_id IS NOT NULL
        GROUP BY question_id, hashtag_id) kept_relations);

-- 3. Remove the now unreferenced non-representative rows.
DELETE FROM hashtag
WHERE hashtag_id NOT IN (
    SELECT kept_id FROM (SELECT MIN(hashtag_id) AS kept_id FROM hashtag GROUP BY tag_name) kept_tags);

ALTER TABLE hashtag ADD CONSTRAINT uk_hashtag_tag_name UNIQUE (tag_name);
