#!/bin/bash
# Runs one of the tools: dev-tools/run.sh LoadRct|GenAll <arguments>   (run build.sh first)
# Xtext's INFO log lines and the SLF4J/log4j notices are filtered out.
HERE="$(cd "$(dirname "$0")" && pwd)"
TOOL="$1"; shift
java -cp "$(cat "$HERE/build/classpath.txt"):$HERE/build/classes" "$TOOL" "$@" 2>&1 | grep -v -E " INFO |^log4j|SLF4J"
exit ${PIPESTATUS[0]}
