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

JDK_FLAGS="${JDK_FLAGS} -XX:+UnlockExperimentalVMOptions"
#JDK_FLAGS="${JDK_FLAGS} -XX:+UseShenandoahGC -XX:ShenandoahMinFreeThreshold=40 -XX:ShenandoahInitFreeThreshold=20 -XX:ShenandoahUncommitDelay=1000 -XX:ShenandoahGuaranteedGCInterval=10000"
JDK_FLAGS="${JDK_FLAGS} -XX:+UseZGC -XX:ZUncommitDelay=120"

EXAMPLES_VERSION="1.0.0"
java ${JDK_FLAGS} -cp target/example-http-service-insights-${EXAMPLES_VERSION}.jar:target/lib/* io.github.matteobertozzi.easerinsights.demo.DemoMain
