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
