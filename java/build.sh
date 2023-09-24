#!/usr/bin/env bash
set -ex

PROJECTS="easer-insights easer-insights-aws-cloudwatch example-http-service-insights"

GIT_HASH=`git describe --always --dirty --match "NOT A TAG"`
GIT_BRANCH=`git branch --no-color --show-current`

ROOT_DIR="$(pwd)"
for project in ${PROJECTS}; do
  cd ${ROOT_DIR}/${project}
  mvn -Dgit.hash=${GIT_HASH} -Dgit.branch=${GIT_BRANCH} clean install -DskipTests
done
