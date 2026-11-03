-- Enforce the login-name uniqueness that Member.username declares but ddl-auto=validate never creates.
-- Before applying: stop writes, back up, and confirm
--   SELECT username, COUNT(*) FROM member GROUP BY username HAVING COUNT(*) > 1;
-- returns no rows. Duplicate accounts are never merged automatically: this statement fails until an
-- operator resolves them one by one. Skip it if SHOW INDEX FROM member already lists a unique username index.
ALTER TABLE member ADD CONSTRAINT uk_member_username UNIQUE (username);
