#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -ex

PROJECTS="easer-insights easer-insights-aws-cloudwatch example-http-service-insights"
if [ $# -gt 0 ]; then
  PROJECTS="$*"
fi

GIT_HASH=`git describe --always --dirty --match "NOT A TAG"`
GIT_BRANCH=`git branch --no-color --show-current`

ROOT_DIR="$(pwd)"
for project in ${PROJECTS}; do
  cd ${ROOT_DIR}/${project}
  mvn -Dgit.hash=${GIT_HASH} -Dgit.branch=${GIT_BRANCH} clean install -DskipTests
done
