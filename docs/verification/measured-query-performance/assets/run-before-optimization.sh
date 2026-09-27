#!/bin/bash
set -eu
cd /Users/seungmin/Desktop/repo/archive/demp/.worktrees/measured-query-performance/backend
common=(--baseline ../baseline --current . --java11 /Users/seungmin/.asdf/installs/java/zulu-11.74.15/zulu-11.jdk/Contents/Home --java25 /Users/seungmin/.asdf/installs/java/zulu-25.36.205 --output docs/verification/measured-query-performance/formal)
python3 -u benchmarks/run_comparison.py "${common[@]}" --labels baseline --sizes 1000,10000
python3 -u benchmarks/run_comparison.py "${common[@]}" --labels current --sizes 1000,10000,100000
python3 -u benchmarks/run_comparison.py "${common[@]}" --labels current-control --sizes 1000,10000
