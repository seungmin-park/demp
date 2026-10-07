---
name: verify-demp
description: Verify DEMP backend reaction persistence, versioned official docs, and visible local user flow.
---

# Verify DEMP

For employment type changes, also follow [the storage and migration record](../../../docs/plans/separate-employment-type.md): verify all four enum values, unknown/null, independent recruitment audience, education clearing, response DTOs, legacy rows and missing-column startup refusal. Run the owned MySQL rehearsal only against isolated fixture databases; its result is not proof that production SQL was applied.

Read [feature map](../../../docs/engineering/feature-map.md), [architecture](../../../docs/engineering/architecture.md), [official docs](../../../docs/engineering/official-docs.md) and the repository `AGENTS.md`. The backend worktree is this skill's Git root. The frontend is a **separate Git repository**; use its own `.agents/skills/verify-dempfrontend/SKILL.md` and report its result separately.

## Doctor

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

The common verify also runs the existing loopback API script and committed reaction switch/cancel/requery against a fresh JAR, random port and isolated in-memory H2. It compares generated, packaged and authenticated served documentation. See [CI boundaries and delivery](../../../docs/engineering/ci-and-delivery.md). Check `build/verification/runtime.json`, `server.log`, `local-flow.log` and nonzero XML with zero failures/errors/skips. The manifest requires 55 named suites, not a fixed total test count. This API check does not replace the visible browser path below. If cmux is unavailable, explain the actual terminal/API scope before running the common verify.

Use the caller's `CMUX_WORKSPACE_ID`, `CMUX_SURFACE_ID`, and `cmux identify --json`. Reuse the current workspace helper terminal or create exactly one pane to the caller's right (`--focus false`); target every send with `--workspace` and `--surface`. Run this backend worktree's `bootRun` or its freshly built `bootJar` with `SPRING_PROFILES_ACTIVE=local`, an isolated `jdbc:h2:mem:<unique>;MODE=MySQL;DB_CLOSE_DELAY=-1`, `PORT=18081`, and `AWS_EC2_METADATA_DISABLED=true`. Run the separate frontend worktree's Vite server in the helper terminal at port 5051 with `DEV_API_TARGET=http://127.0.0.1:18081`; configure the backend `APP_CORS_ALLOWED_ORIGINS=http://127.0.0.1:5051` if direct browser API calls need it. Confirm both process command lines/build paths and a ready HTTP response before using the browser. Do not treat another server's responding port as this worktree.

In a browser surface in the **same caller workspace**, log in with local-only `local-member / password`, open `/questions/-1`, click question recommend and answer dislike, reload, assert counts and `aria-pressed`, switch/cancel question vote, reload and assert zero while answer vote remains. Distinguish this real Spring/H2 path from Playwright's API fixtures. Keep the visible pane; stop only processes started here if cleanup is needed. Record exact command, exit, DOM assertions, version/HEAD, and log/report paths.
