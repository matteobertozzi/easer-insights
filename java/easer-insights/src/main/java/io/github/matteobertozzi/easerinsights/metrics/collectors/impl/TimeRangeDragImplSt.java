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

import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter.TimeRangeCounterSnapshot;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeDrag;
import io.github.matteobertozzi.rednaco.collections.arrays.ArrayUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

class TimeRangeDragImplSt implements TimeRangeDrag {
  private final long[] counters;
  private final int window;
  private long lastInterval;
  private long next;

  public TimeRangeDragImplSt(final long maxInterval, final long window, final TimeUnit unit) {
    this.window = Math.toIntExact(unit.toMillis(window));
    this.lastInterval = 0;

    final int trcCount = (int) Math.ceil(unit.toMillis(maxInterval) / (float) this.window);
    this.counters = new long[trcCount];
  }

  @Override
  public void add(final long timestamp, final long delta) {
    update(timestamp, delta);
  }

  @Override
  public void set(final long timestamp, final long value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TimeRangeCounterSnapshot dataSnapshot() {
    update(TimeUtil.currentEpochMillis(), 0);

    final long[] data = new long[(int)Math.min(next + 1, counters.length)];
    final int eofIndex = 1 + (int) (next % counters.length);
    if (next >= data.length) {
      // 5, 6, 7, 8, 1, 2, 3, 4
      ArrayUtil.copy(data, 0, counters, eofIndex, counters.length);
      ArrayUtil.copy(data, counters.length - eofIndex, counters, 0, eofIndex);
    } else {
      //System.out.println("eofIndex: " + eofIndex + " -> next:" + next + "/" + length);
      ArrayUtil.copy(data, 0, counters, 0, eofIndex); // 1, 2, 3, 4
    }
    return new TimeRangeCounterSnapshot(lastInterval, window, data);
  }

  public void update(final long timestamp, final long delta) {
    final long alignedTs = TimeUtil.alignToWindow(timestamp, window);
    final long deltaTime = alignedTs - lastInterval;

    // update the current slot
    if (deltaTime == 0) {
      counters[(int)(next % counters.length)] += delta;
      return;
    }

    if (deltaTime == window) {
      lastInterval = alignedTs;
      final long currentValue = counters[(int)(next % counters.length)];
      counters[(int)(++next % counters.length)] = currentValue + delta;
      return;
    }

    // inject slots
    if (deltaTime > 0) {
      injectSlots(deltaTime);
      lastInterval = alignedTs;
      final long currentValue = next > 0 ? counters[(int)((next - 1) % counters.length)] : 0;
      counters[(int)(next % counters.length)] = currentValue + delta;
      return;
    }

    // update past slot
    final int availSlots = (int) Math.min(next + 1, counters.length);
    final int pastIndex = (int) (-deltaTime / window);
    if (pastIndex >= availSlots) {
      for (int i = 0; i < counters.length; ++i) {
        counters[i] += delta;
      }
    } else {
      long index = next - pastIndex;
      while (index <= next) {
        counters[(int)(index++ % counters.length)] += delta;
      }
    }
  }

  private void injectSlots(final long deltaTime) {
    final long currentValue = counters[(int)(next % counters.length)];
    final int slots = (int) (deltaTime / window);
    if (slots >= counters.length) {
      Arrays.fill(counters, currentValue);
      next += slots;
      return;
    }

    final int fromIndex = (int) ((next + 1) % counters.length);
    final int toIndex = (int) ((next + 1 + slots) % counters.length);
    if (fromIndex < toIndex) {
      Arrays.fill(counters, fromIndex, toIndex, currentValue);
    } else {
      Arrays.fill(counters, fromIndex, counters.length, currentValue);
      Arrays.fill(counters, 0, toIndex, currentValue);
    }
    next += slots;
  }
}
