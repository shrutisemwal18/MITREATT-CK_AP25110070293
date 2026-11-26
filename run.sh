#!/bin/bash
# macOS / Linux: compile and run
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/*.java && java -cp out Main
