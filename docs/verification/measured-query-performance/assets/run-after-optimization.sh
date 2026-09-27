#!/bin/bash
set -eu
base=/Users/seungmin/Desktop/repo/archive/demp/.worktrees/measured-query-performance/backend
cd "$base"
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-25.36.205 ./gradlew -I benchmarks/benchmark.init.gradle queryBenchmark -PbenchContract=true -PbenchLabel=current --console=plain > /tmp/demp-t103-current-contract.log 2>&1
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-25.36.205 ./gradlew -I benchmarks/benchmark.init.gradle queryBenchmark -PbenchContract=true -PbenchLabel=current-control --console=plain > /tmp/demp-t103-control-contract.log 2>&1
cd ../baseline
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-11.74.15/zulu-11.jdk/Contents/Home ./gradlew -I "$base/benchmarks/benchmark.init.gradle" queryBenchmark -PbenchContract=true -PbenchLabel=baseline --console=plain > /tmp/demp-t103-baseline-contract.log 2>&1
cd "$base"
python3 -u benchmarks/run_comparison.py --baseline ../baseline --current . --java11 /Users/seungmin/.asdf/installs/java/zulu-11.74.15/zulu-11.jdk/Contents/Home --java25 /Users/seungmin/.asdf/installs/java/zulu-25.36.205 --output docs/verification/measured-query-performance/formal --labels current-optimized
