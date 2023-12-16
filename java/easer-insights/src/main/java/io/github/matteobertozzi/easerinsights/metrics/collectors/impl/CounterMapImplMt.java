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

package io.github.matteobertozzi.easerinsights.metrics.collectors.impl;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import io.github.matteobertozzi.easerinsights.metrics.collectors.CounterMap;

class CounterMapImplMt implements CounterMap {
  private final ConcurrentHashMap<String, LongAdder> counterMap = new ConcurrentHashMap<>();

  public CounterMapImplMt() {
    super();
  }

  @Override
  public void add(final String key, final long timestamp, final long value) {
    counterMap.computeIfAbsent(key, k -> new LongAdder()).add(value);
  }

  @Override
  public void set(final String key, final long timestamp, final long value) {
    final LongAdder adder = counterMap.computeIfAbsent(key, k -> new LongAdder());
    adder.reset();
    adder.add(value);
  }

  @Override
  public CounterMapSnapshot dataSnapshot() {
    final String[] keys = new String[counterMap.size()];
    final long[] values = new long[keys.length];

    int index = 0;
    for (final Entry<String, LongAdder> entry: counterMap.entrySet()) {
      keys[index] = entry.getKey();
      values[index] = entry.getValue().sum();
      index++;
    }
    return CounterMapSnapshot.ofUnsorted(keys, values);
  }
}
