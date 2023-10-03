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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.easerinsights.util.ArrayUtil;
import io.github.matteobertozzi.easerinsights.util.TimeRange;
import io.github.matteobertozzi.easerinsights.util.TimeUtil;

public class TimeRangeCounterImplSt implements TimeRangeCounter {
  private final TimeRange timeRange;
  private final long[] counters;

  public TimeRangeCounterImplSt(final long maxInterval, final long window, final TimeUnit unit) {
    this.timeRange = new TimeRange(Math.toIntExact(unit.toMillis(window)), 0);

    final int trcCount = (int) Math.ceil(unit.toMillis(maxInterval) / (float) timeRange.window());
    this.counters = new long[trcCount];
  }

  @Override
  public void add(final long timestamp, final long delta) {
    timeRange.update(timestamp, counters.length, this::resetSlots, slotIndex -> counters[slotIndex] += delta);
  }

  @Override
  public void set(final long timestamp, final long value) {
    timeRange.update(timestamp, counters.length, this::resetSlots, slotIndex -> counters[slotIndex] = value);
  }

  private void resetSlots(final int fromIndex, final int toIndex) {
    Arrays.fill(counters, fromIndex, toIndex, 0);
  }

  @Override
  public TimeRangeCounterSnapshot dataSnapshot() {
    final long now = TimeUtil.currentEpochMillis();
    timeRange.update(now, counters.length, this::resetSlots);

    final long[] data = new long[timeRange.size(counters.length)];
    timeRange.copy(counters.length, data.length,
      (dstIndex, srcFromIndex, srcToIndex) -> ArrayUtil.copy(data, dstIndex, counters, srcFromIndex, srcToIndex)
    );
    return new TimeRangeCounterSnapshot(timeRange.lastInterval(), timeRange.window(), data);
  }
}
