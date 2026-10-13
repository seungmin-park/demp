---
name: verify-demp
description: Verify DEMP backend reaction persistence, versioned official docs, and visible local user flow.
---

# Verify DEMP

For employment type changes, also follow [the storage and migration record](../../../docs/plans/separate-employment-type.md): verify all four enum values, unknown/null, independent recruitment audience, education clearing, response DTOs, legacy rows and missing-column startup refusal. Run the owned MySQL rehearsal only against isolated fixture databases; its result is not proof that production SQL was applied.

Read [feature map](../../../docs/engineering/feature-map.md), [architecture](../../../docs/engineering/architecture.md), [official docs](../../../docs/engineering/official-docs.md) and the repository `AGENTS.md`. The backend worktree is this skill's Git root. The frontend is a **separate Git repository**; use its own `.agents/skills/verify-dempfrontend/SKILL.md` and report its result separately.

## Doctor

백엔드 CD 변경은 `scripts/test_prepare_backend_cd.py`, `scripts/test_backend_cd_workflow.py`와 filesystem 종료 설정 검사를 포함한 공용 verify를 실행한다. workflow mapping 검사에 Ruby 표준 YAML parser가 필요하다. 기본 비활성 workflow를 테스트 통과만으로 운영 활성화했다고 기록하지 않는다. 배포 저장소의 실제 두 JVM/MySQL/Nginx 결과와 VM 적용은 별도 근거로 확인한다.

```sh
git status --short
git rev-parse HEAD
cat .tool-versions
python3 scripts/check_agent_contracts.py
export JAVA_HOME="$(asdf where java)/Contents/Home"
./gradlew --version
```

`JAVA_HOME` must be the `.tool-versions` Java 25 installation. Check the launcher JVM printed by Gradle; this machine's default JVM can be 21 even though the project selects Java 25. The guard checks local manifest/document versions and direct reaction repository writers; it does not contact documentation servers.

## Fast feedback and full backend verification

```sh
./gradlew test --tests '*ContentReaction*' --console=plain
bash scripts/verify.sh
```

Check `build/test-results/test` for nonzero test count, zero failures/errors/skips, and command exit 0. `build/docs/asciidoc/index.html` must exist. The focused service tests exercise committed state; MVC/REST Docs verify HTTP. `python3 scripts/check_agent_contracts.py --probe-violation` demonstrates the forbidden writer detection without leaving a file.

## Visible local path

For question view counts, enter detail and reload in the current cmux browser: each successful detail request adds one, including author views. Editing uses `recordView=false` and list GET remains pure. Confirm stored hits with a fresh pure detail/list API read; check concurrent service requests and authentication failure in the real DB tests. Record the backend JAR/source identity; frontend CI/CD success does not prove a backend deployment.

The common verify also runs the existing loopback API script and committed reaction switch/cancel/requery against a fresh JAR, random port and isolated in-memory H2. It compares generated, packaged and authenticated served documentation. See [CI boundaries and delivery](../../../docs/engineering/ci-and-delivery.md). Check `build/verification/runtime.json`, `server.log`, `local-flow.log` and nonzero XML with zero failures/errors/skips. Read the current required-suite manifest rather than relying on a fixed historical count. This API check does not replace the visible browser path below. If cmux is unavailable, explain the actual terminal/API scope before running the common verify.

## Announcement draft and recruitment contracts

`bash scripts/verify.sh` also invokes `scripts/verify_announcement_contract.py` through the fresh-JAR runtime verifier. Inspect `build/verification/announcement-contract.json`: actual HTTP create/read/update/delete, incomplete draft nullability, full detail/summary keys and types, role conditions, salary period, source alias conflict, rejected update preservation, publication validation, secondary-role search and annual-salary exclusion. JSON/text/empty response kinds follow the existing routes; content type, cache control and nosniff are unconditional assertions. Login is real; invalid credentials, a correctly signed expired token, unauthenticated and ordinary-user permissions are checked. Three mutated copies of a real response must be rejected by the same contract validator. They are validator probes, not mocked integration requests.

The runtime owns an isolated H2 process and a generated JWT signing key. Its extra seed grants the existing local fixture admin authority only in that process. Created announcements are deleted in finally; the test member disappears when the process and in-memory DB are destroyed. No S3 request occurs in this text-only contract flow. Timing is recorded without inventing an SLA. Existing `ApiSecurityTest`/login protection suites cover remaining auth/rate-limit contracts; no refresh endpoint is assumed.

`AnnouncementDraftRecoveryTest` is required in the XML manifest. Removing its report must fail `check_test_reports`; a nonzero total alone is insufficient. MVC/REST Docs tests still use mocked services and are reported separately from this real HTTP/H2 flow.

For visible acceptance, reuse the caller's cmux pane: admin creates a draft with missing fields, saves/reopens, attempts invalid publication, corrects and publishes. Confirm seconds, monthly pay, roles and reference links after fresh reads. In a separate synthetic record set a future start and near deadline: leave detail and OPEN-filtered list open; assert UPCOMING→OPEN→CLOSED and actual card entry/exit, with unchanged stored dates/manual flag during automatic transitions. Then test manual close/unclose explicitly. Browser badge timer is 1 second; list refresh is 30 seconds/visibility return, and failure preserves prior results. This verifies registered dates, not external employer changes. See [acceptance evidence](../../../docs/verification/real-posting-registration/improvements-20261013.md).

Use the caller's `CMUX_WORKSPACE_ID`, `CMUX_SURFACE_ID`, and `cmux identify --json`. Reuse the current workspace helper terminal or create exactly one pane to the caller's right (`--focus false`); target every send with `--workspace` and `--surface`. Run this backend worktree's `bootRun` or its freshly built `bootJar` with `SPRING_PROFILES_ACTIVE=local`, an isolated `jdbc:h2:mem:<unique>;MODE=MySQL;DB_CLOSE_DELAY=-1`, `PORT=18081`, and `AWS_EC2_METADATA_DISABLED=true`. Run the separate frontend worktree's Vite server in the helper terminal at port 5051 with `DEV_API_TARGET=http://127.0.0.1:18081`; configure the backend `APP_CORS_ALLOWED_ORIGINS=http://127.0.0.1:5051` if direct browser API calls need it. Confirm both process command lines/build paths and a ready HTTP response before using the browser. Do not treat another server's responding port as this worktree.

In a browser surface in the **same caller workspace**, log in with local-only `local-member / password`, open `/questions/-1`, click question recommend and answer dislike, reload, assert counts and `aria-pressed`, switch/cancel question vote, reload and assert zero while answer vote remains. Distinguish this real Spring/H2 path from Playwright's API fixtures. Keep the visible pane; stop only processes started here if cleanup is needed. Record exact command, exit, DOM assertions, version/HEAD, and log/report paths.

## Member self service

Inspect `build/verification/member-contract.json` from the existing full verify: real register/login, own profile schema, nickname update/requery, attempted ID/role injection ignored, invalid update preservation, current password failure, byte limit, successful change and old-token rejection/new login. The isolated process owns and destroys all member fixtures. MemberAccountServiceTest verifies committed failure counts and shared lockout. In the caller cmux browser save/reload nickname, fail then correct current password, change password and sign in again. Never put passwords in draft storage; nickname drafts are per login ID. Review credentialVersion against the current DB principal, not token role claims. Apply additive SQL with backup/restore rehearsal before merge-triggered production CD.

## Technology CHECK compatibility

When adding or importing technologies, inspect the actual MySQL language CHECK; Hibernate validate does not prove its allowed values. Keep `announcement-technologies-check.sql` aligned with Language via the required TechnologyCheckMigrationTest. A new empty Hibernate schema rehearsal cannot establish compatibility with a populated production CHECK. Rehearse the exact SQL on a restored owned DB: legacy CPP rejection, every current value accepted in a rolled-back transaction, unknown value still rejected, original row values unchanged. Never drop tags to bypass the constraint or drop the check without replacing it.
