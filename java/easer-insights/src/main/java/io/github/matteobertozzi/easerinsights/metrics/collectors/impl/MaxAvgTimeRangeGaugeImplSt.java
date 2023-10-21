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

import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.util.ArrayUtil;
import io.github.matteobertozzi.easerinsights.util.TimeRange;
import io.github.matteobertozzi.easerinsights.util.TimeUtil;

public class MaxAvgTimeRangeGaugeImplSt implements MaxAvgTimeRangeGauge {
  private final TimeRange timeRange;
  private final long[] ring;

  public MaxAvgTimeRangeGaugeImplSt(final long maxInterval, final long window, final TimeUnit unit) {
    this.timeRange = new TimeRange(Math.toIntExact(unit.toMillis(window)), 0);

    final int trcCount = (int) Math.ceil(unit.toMillis(maxInterval) / (float) timeRange.window());
    this.ring = new long[trcCount * 3]; // count|sum|max|...
  }

  @Override
  public void sample(final long timestamp, final long value) {
    timeRange.update(timestamp, ring.length / 3, this::resetSlots, slotIndex -> {
      final int ringIndex = slotIndex * 3;
      ring[ringIndex]++;
      ring[ringIndex + 1] += value;
      ring[ringIndex + 2] = Math.max(ring[ringIndex + 2], value);
    });
  }

  private void resetSlots(final int fromIndex, final int toIndex) {
    Arrays.fill(ring, fromIndex * 3, toIndex * 3, 0);
  }

  @Override
  public MaxAvgTimeRangeGaugeSnapshot dataSnapshot() {
    final long now = TimeUtil.currentEpochMillis();
    timeRange.update(now, ring.length / 3, this::resetSlots);

    final int length = timeRange.size(ring.length / 3);
    final long[] count = new long[length];
    final long[] sum = new long[length];
    final long[] max = new long[length];
    timeRange.copy(ring.length / 3, length, (dstIndex, srcFromIndex, srcToIndex) -> {
      final int copyLength = srcToIndex - srcFromIndex;
      final int ringIndex = srcFromIndex * 3;
      ArrayUtil.copyStride(count, dstIndex, ring, ringIndex, copyLength, 3);
      ArrayUtil.copyStride(sum, dstIndex, ring, ringIndex + 1, copyLength, 3);
      ArrayUtil.copyStride(max, dstIndex, ring, ringIndex + 2, copyLength, 3);
    });
    return new MaxAvgTimeRangeGaugeSnapshot(timeRange.lastInterval(), timeRange.window(), count, sum, max);
  }
}
