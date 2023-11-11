/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.matteobertozzi.easerinsights.metrics.collectors;

import io.github.matteobertozzi.rednaco.time.TimeUtil;

public interface CollectorCounter extends CollectorGauge {
  // 00 inc
  // 01 dec
  // 10 add <value>
  // 11 set <value>
  void add(long timestamp, long delta);
  void set(long timestamp, long value);

  default void inc() { add(TimeUtil.currentEpochMillis(), 1); }
  default void dec() { add(TimeUtil.currentEpochMillis(), -1); }
  default void inc(final long timestamp) { add(timestamp, 1); }
  default void dec(final long timestamp) { add(timestamp, -1); }
  default void add(final long delta) { add(TimeUtil.currentEpochMillis(), delta); }
  default void set(final long value) { set(TimeUtil.currentEpochMillis(), value); }

  @Override default void sample(final long timestamp, final long value) { set(timestamp, value); }
}
