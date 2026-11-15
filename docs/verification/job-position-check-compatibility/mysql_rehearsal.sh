#!/usr/bin/env bash
# Rehearse db/manual/announcement-job-position-check.sql on a throwaway MySQL.
# Usage: mysql_rehearsal.sh <mysql socket>. Writes only demp_jobpos_* databases and drops them.
set -euo pipefail
SOCKET=${1:?mysql socket path}
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
MIGRATION="$ROOT/src/main/resources/db/manual/announcement-job-position-check.sql"
LEGACY="'BACKEND','FRONTEND','FULLSTACK','ANDROID','IOS','MACHINE_LEARNING','AI','DATA_ENGINEER','MOBILE_GAME','GAME_CLIENT','GAME_SERVER','SYSTEM_NETWORK','SYSTEM_SOFTWARE','DEVOPS','INTERNET_SECURITY','EMBEDDED_SOFTWARE','ROBOTIC_MIDDLEWARE','QA','IOT','APPLICATION_PROGRAM','BLOCK_CHAIN'"
CURRENT=$(sed -n 's/.*CHECK (job_position IN (\(.*\)));$/\1/p' "$MIGRATION")
run() { mysql -uroot --socket="$SOCKET" -N -B "$@"; }
pass() { echo "PASS: $*"; }
fail() { echo "FAIL: $*"; exit 1; }
expect_check_violation() {
    local db=$1 sql=$2 out
    if out=$(run "$db" -e "$sql" 2>&1); then fail "accepted: $sql"; fi
    [[ $out == *"ERROR 3819"* ]] || fail "unexpected error for $sql: $out"
}
checks() { run "$1" -e "SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE LIKE '%\`job_position\`%' FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME WHERE tc.TABLE_SCHEMA = '$1' AND tc.TABLE_NAME = 'announcement' ORDER BY 1"; }
digest() { run "$1" -e "SELECT COALESCE(SHA2(GROUP_CONCAT(CONCAT_WS(':', id, title, announcement_type, IFNULL(job_position, 'NULL')) ORDER BY id SEPARATOR '|'), 256), 'empty') FROM announcement"; }

# Hibernate inlined one CHECK per enum column; its number follows column order.
legacy_db() {
    local db=$1 order=$2 type_col position_col
    type_col="announcement_type VARCHAR(255) CHECK (announcement_type IN ('EMP','EDU'))"
    position_col="job_position VARCHAR(255) CHECK (job_position IN ($LEGACY))"
    run -e "DROP DATABASE IF EXISTS $db; CREATE DATABASE $db"
    if [[ $order == type-first ]]; then cols="$type_col, $position_col"; else cols="$position_col, $type_col"; fi
    run "$db" -e "CREATE TABLE announcement (id BIGINT NOT NULL PRIMARY KEY, title VARCHAR(255), $cols);
        INSERT INTO announcement (id, title, announcement_type, job_position) VALUES (1, '기존 백엔드', 'EMP', 'BACKEND'), (2, '분야 미정 초안', 'EMP', NULL), (3, '블록체인', 'EDU', 'BLOCK_CHAIN')"
}

verify_expanded() {
    local db=$1 before=$2
    run "$db" -e "START TRANSACTION; INSERT INTO announcement (id, title, announcement_type, job_position) SELECT 100 + seq, 'probe', 'EMP', value FROM JSON_TABLE(JSON_ARRAY($CURRENT), '\$[*]' COLUMNS (seq FOR ORDINALITY, value VARCHAR(255) PATH '\$')) probe; SELECT COUNT(*) FROM announcement WHERE title = 'probe'; ROLLBACK" | grep -qx 31 \
        || fail "$db: all current positions should be accepted"
    pass "$db: all 31 current positions accepted (rolled back)"
    expect_check_violation "$db" "INSERT INTO announcement (id, title, announcement_type, job_position) VALUES (900, 'x', 'EMP', 'INVALID_POSITION')"
    pass "$db: unknown position still rejected with 3819"
    run "$db" -e "INSERT INTO announcement (id, title, announcement_type, job_position) VALUES (901, 'x', 'EMP', NULL); DELETE FROM announcement WHERE id = 901"
    pass "$db: NULL position (draft) still accepted"
    expect_check_violation "$db" "INSERT INTO announcement (id, title, announcement_type, job_position) VALUES (902, 'x', 'XYZ', 'SRE')"
    pass "$db: announcement_type CHECK preserved"
    [[ $(digest "$db") == "$before" ]] || fail "$db: stored values changed"
    pass "$db: stored values digest unchanged"
    [[ $(checks "$db" | awk '$2 == 1' | cut -f1) == announcement_job_position_chk ]] || fail "$db: expected exactly one job_position CHECK"
    pass "$db: exactly one job_position CHECK (announcement_job_position_chk)"
}

for order in type-first position-first; do
    db=demp_jobpos_${order//-/_}
    legacy_db "$db" "$order"
    echo "-- $db before: $(checks "$db" | tr '\t\n' '= ')"
    expect_check_violation "$db" "INSERT INTO announcement (id, title, announcement_type, job_position) VALUES (4, '새 분야', 'EMP', 'SRE')"
    pass "$db: Red reproduced, SRE rejected with 3819 before migration"
    before=$(digest "$db")
    run "$db" < "$MIGRATION"
    verify_expanded "$db" "$before"
    run "$db" < "$MIGRATION"
    verify_expanded "$db" "$before"
    pass "$db: re-running the migration is safe"
    run -e "DROP DATABASE $db"
done

db=demp_jobpos_without_check
run -e "DROP DATABASE IF EXISTS $db; CREATE DATABASE $db"
run "$db" -e "CREATE TABLE announcement (id BIGINT NOT NULL PRIMARY KEY, title VARCHAR(255), announcement_type VARCHAR(255) CHECK (announcement_type IN ('EMP','EDU')), job_position VARCHAR(255));
    INSERT INTO announcement VALUES (1, '기존', 'EMP', 'SRE')"
before=$(digest "$db")
run "$db" < "$MIGRATION"
verify_expanded "$db" "$before"
run -e "DROP DATABASE $db"
echo "ALL PASS ($(run -e 'SELECT VERSION()'))"
