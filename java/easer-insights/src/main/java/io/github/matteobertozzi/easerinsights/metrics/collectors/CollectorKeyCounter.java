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

public interface CollectorKeyCounter {
  void add(String key, long timestamp, long delta);
  void set(String key, long timestamp, long value);

  default void inc(final String key) { add(key, TimeUtil.currentEpochMillis(), 1); }
  default void dec(final String key) { add(key, TimeUtil.currentEpochMillis(), -1); }
  default void inc(final String key, final long timestamp) { add(key, timestamp, 1); }
  default void dec(final String key, final long timestamp) { add(key, timestamp, -1); }
  default void add(final String key, final long delta) { add(key, TimeUtil.currentEpochMillis(), delta); }
  default void set(final String key, final long value) { set(key, TimeUtil.currentEpochMillis(), value); }
}
