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
import io.github.matteobertozzi.rednaco.collections.arrays.ArrayUtil;
import io.github.matteobertozzi.rednaco.time.TimeRange;
import io.github.matteobertozzi.rednaco.time.TimeRange.ResetTimeRangeSlots;
import io.github.matteobertozzi.rednaco.time.TimeRange.UpdateTimeRangeSlot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

class TimeRangeCounterImplSt implements TimeRangeCounter, ResetTimeRangeSlots, UpdateTimeRangeSlot {
  private final TimeRange timeRange;
  private final long[] counters;
  private long newValue;
  private boolean updateValue;

  public TimeRangeCounterImplSt(final long maxInterval, final long window, final TimeUnit unit) {
    this.timeRange = new TimeRange(Math.toIntExact(unit.toMillis(window)), 0);

    final int trcCount = (int) Math.ceil(unit.toMillis(maxInterval) / (float) timeRange.window());
    this.counters = new long[trcCount];
  }

  @Override
  public void add(final long timestamp, final long delta) {
    this.newValue = delta;
    this.updateValue = true;
    timeRange.update(timestamp, counters.length, this, this);
  }

  @Override
  public void set(final long timestamp, final long value) {
    this.newValue = value;
    this.updateValue = false;
    timeRange.update(timestamp, counters.length, this, this);
  }

  @Override
  public void updateTimeRangeSlot(final int slotIndex) {
    if (updateValue) {
      counters[slotIndex] += newValue;
    } else {
      counters[slotIndex] = newValue;
    }
  }

  @Override
  public void resetTimeRangeSlots(final int fromIndex, final int toIndex) {
    Arrays.fill(counters, fromIndex, toIndex, 0);
  }

  @Override
  public TimeRangeCounterSnapshot dataSnapshot() {
    final long now = TimeUtil.currentEpochMillis();
    timeRange.update(now, counters.length, this);

    final long[] data = new long[timeRange.size(counters.length)];
    timeRange.copy(counters.length, data.length,
      (dstIndex, srcFromIndex, srcToIndex) -> ArrayUtil.copy(data, dstIndex, counters, srcFromIndex, srcToIndex)
    );
    return new TimeRangeCounterSnapshot(timeRange.lastInterval(), timeRange.window(), data);
  }
}
