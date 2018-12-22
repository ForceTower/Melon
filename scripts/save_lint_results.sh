#!/bin/sh

RESULTS_DIR="ci_results/lint/"
mkdir -p $RESULTS_DIR

LINT_FILES=`find . -type f -regex ".*/build/reports/lint-results\..*" | grep -v third_party`

for file in $LINT_FILES
do
  rename 's/\/(.*)\/build\/reports\/lint-results/\/$1\/build\/reports\/lint-results-$1/' $file
done

find . -type f -regex ".*/build/reports/lint-results-.*\..*" -exec cp {} $RESULTS_DIR \;
