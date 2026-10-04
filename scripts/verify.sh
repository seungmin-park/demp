#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 -m unittest discover -s scripts -p 'test_*.py' -v
python3 scripts/check_agent_contracts.py --probe-violation
./gradlew clean test asciidoctor bootJar --console=plain
python3 scripts/verify_ci.py
