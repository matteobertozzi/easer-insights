package io.github.matteobertozzi.easerinsights.metrics.collectors.impl;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.metrics.collectors.Heatmap;
import io.github.matteobertozzi.rednaco.collections.arrays.ArrayUtil;
import io.github.matteobertozzi.rednaco.time.TimeRange;
import io.github.matteobertozzi.rednaco.time.TimeRange.ResetTimeRangeSlots;
import io.github.matteobertozzi.rednaco.time.TimeRange.UpdateTimeRangeSlot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

public class HeatmapImplSt implements Heatmap, ResetTimeRangeSlots, UpdateTimeRangeSlot {
  private final TimeRange timeRange;
  private final long[] bounds;
  private final long[] ring;
  private final int totalSlots;
  private long newValue;

  public HeatmapImplSt(final long maxInterval, final long window, final TimeUnit unit, final long[] bounds) {
    this.timeRange = new TimeRange(Math.toIntExact(unit.toMillis(window)), 0);
    this.bounds = bounds;

    final int stride = (bounds.length + 5);
    this.totalSlots = (int) Math.ceil(unit.toMillis(maxInterval) / (float) timeRange.window());
    this.ring = new long[totalSlots * stride]; // |min|max|sum|sumSquares|events...
    this.ring[0] = Long.MAX_VALUE;
  }

  @Override
  public void sample(final long timestamp, final long value) {
    newValue = value;
    timeRange.update(timestamp, totalSlots, this, this);
  }

  @Override
  public void updateTimeRangeSlot(final int slotIndex) {
    final int boundIndex = findBoundIndex(newValue);
    final int ringIndex = slotIndex * (bounds.length + 5);
    //System.out.println(" -> UPDATE slotIndex:" + slotIndex + " ringIndex:" + ringIndex + " boundIndex:" + boundIndex);
    ring[ringIndex] = Math.min(ring[ringIndex], newValue);
    ring[ringIndex + 1] = Math.max(ring[ringIndex + 1], newValue);
    ring[ringIndex + 2] += newValue;
    ring[ringIndex + 3] += newValue * newValue;
    ring[ringIndex + 4 + boundIndex]++;
  }

  @Override
  public void resetTimeRangeSlots(final int fromIndex, final int toIndex) {
    final int resetLength = toIndex - fromIndex;
    final int stride = bounds.length + 5;
    //System.out.println("resetSlots: " + resetLength + " from: " + fromIndex);
    Arrays.fill(ring, fromIndex * stride, toIndex * stride, 0);
    for (int i = 0; i < resetLength; ++i) {
      ring[(fromIndex + i) * stride] = Long.MAX_VALUE;
    }
  }

  private int findBoundIndex(final long value) {
    for (int i = 0; i < bounds.length; ++i) {
      if (value < bounds[i]) {
        return i;
      }
    }
    return bounds.length;
  }

  @Override
  public HeatmapSnapshot dataSnapshot() {
    final int stride = bounds.length + 5;
    final long now = TimeUtil.currentEpochMillis();
    timeRange.update(now, totalSlots, this);

    // compute bounds
    long lastBound = 0;
    for (int i = 0; i < ring.length; i += stride) {
      lastBound = Math.max(lastBound, ring[i + 1]);
    }
    final int lastBoundIndex = findBoundIndex(lastBound) + 1;
    final long[] snapshotBounds = new long[lastBoundIndex];
    for (int i = 0; i < lastBoundIndex && i < bounds.length; ++i) {
      snapshotBounds[i] = bounds[i];
    }
    snapshotBounds[snapshotBounds.length - 1] = lastBound;

    // build the snapshot
    final int length = timeRange.size(totalSlots);
    final long[] events = new long[length * lastBoundIndex];
    final long[] minValue = new long[length];
    final long[] maxValue = new long[length];
    final long[] sumSquares = new long[length];
    final long[] sum = new long[length];
    timeRange.copy(totalSlots, length, (dstIndex, srcFromIndex, srcToIndex) -> {
      final int copyLength = srcToIndex - srcFromIndex;
      for (int i = 0; i < copyLength; ++i) {
        final int baseDstIndex = dstIndex + i;
        final int ringIndex = (srcFromIndex + i) * stride;
        minValue[baseDstIndex] = ring[ringIndex] == Long.MAX_VALUE ? 0 : ring[ringIndex];
        maxValue[baseDstIndex] = ring[ringIndex + 1];
        sum[baseDstIndex] = ring[ringIndex + 2];
        sumSquares[baseDstIndex] = ring[ringIndex + 3];
        ArrayUtil.copy(events, baseDstIndex * lastBoundIndex, ring, ringIndex + 4, ringIndex + 4 + lastBoundIndex);
      }
    });

    return new HeatmapSnapshot(timeRange.lastInterval(), timeRange.window(), snapshotBounds, events, minValue, maxValue, sum, sumSquares);
  }
}
