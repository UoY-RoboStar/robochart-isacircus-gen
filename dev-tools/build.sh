#!/bin/bash
# Compiles RctLoader (plugin source) and the tools in src/ against the jars of an installed RoboTool.
# usage: dev-tools/build.sh [RoboTool dir]   (default ~/tools/robotool-1.3.2026102101)
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
ROBOTOOL="${1:-$HOME/tools/robotool-1.3.2026102101}"
P="$ROBOTOOL/plugins"
mkdir -p "$HERE/build/classes"
ls "$P"/*.jar | grep -E "/(org\.eclipse\.xtext|org\.eclipse\.xtend|com\.google\.(inject|guava)|org\.aopalliance|javax\.inject|jakarta\.inject|org\.antlr\.runtime_3|org\.eclipse\.emf\.(common|ecore)|org\.apache\.log4j|org\.eclipse\.equinox\.common|org\.eclipse\.core\.runtime|io\.github\.classgraph|org\.objectweb\.asm|org\.eclipse\.osgi_|circus\.robocalc\.robochart_|circus\.robocalc\.robochart\.textual_|org\.eclipse\.epsilon\.(common|eol\.engine|egl\.engine|erl\.engine|emc\.emf)_|org\.apache\.commons\.lang3|org\.eclipse\.emf\.mwe|org\.slf4j|ch\.qos)" \
  | grep -v -E "\.ui[._]|\.ide[._]|source|\.tests" | tr '\n' ':' > "$HERE/build/classpath.txt"
javac -d "$HERE/build/classes" -cp "$(cat "$HERE/build/classpath.txt")" \
  "$HERE/../circus.robocalc.robochart.epsilon.isacircus/src/circus/robocalc/robochart/epsilon/isacircus/loader/"*.java \
  "$HERE"/src/*.java
echo "built into $HERE/build/classes"
