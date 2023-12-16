package io.github.matteobertozzi.easerinsights.metrics.collectors.impl;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.rednaco.collections.arrays.ArrayUtil;
import io.github.matteobertozzi.rednaco.time.TimeRange;
import io.github.matteobertozzi.rednaco.time.TimeRange.ResetTimeRangeSlots;
import io.github.matteobertozzi.rednaco.time.TimeRange.UpdateTimeRangeSlot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

class MaxAvgTimeRangeGaugeImplSt implements MaxAvgTimeRangeGauge, ResetTimeRangeSlots, UpdateTimeRangeSlot {
  private final TimeRange timeRange;
  private final long[] ring;
  private long newValue;

  public MaxAvgTimeRangeGaugeImplSt(final long maxInterval, final long window, final TimeUnit unit) {
    this.timeRange = new TimeRange(Math.toIntExact(unit.toMillis(window)), 0);

    final int trcCount = (int) Math.ceil(unit.toMillis(maxInterval) / (float) timeRange.window());
    this.ring = new long[trcCount * 3]; // count|sum|max|...
  }

  @Override
  public void sample(final long timestamp, final long value) {
    newValue = value;
    timeRange.update(timestamp, ring.length / 3, this, this);
  }


  @Override
  public void updateTimeRangeSlot(final int slotIndex) {
    final int ringIndex = slotIndex * 3;
    ring[ringIndex]++;
    ring[ringIndex + 1] += newValue;
    ring[ringIndex + 2] = Math.max(ring[ringIndex + 2], newValue);
  }

  @Override
  public void resetTimeRangeSlots(final int fromIndex, final int toIndex) {
    Arrays.fill(ring, fromIndex * 3, toIndex * 3, 0);
  }

  @Override
  public MaxAvgTimeRangeGaugeSnapshot dataSnapshot() {
    final long now = TimeUtil.currentEpochMillis();
    timeRange.update(now, ring.length / 3, this);

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
