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

import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.util.TimeUtil;

public class TimeRangeGaugeImplSt extends TimeRangeGauge {
  private final long[] counters;
  private final int window;

  private long lastInterval = Long.MAX_VALUE;
  private long next;

  protected TimeRangeGaugeImplSt(final long maxInterval, final long window, final TimeUnit unit) {
    this.window = Math.toIntExact(unit.toMillis(window));

    final int trcCount = (int) Math.ceil(unit.toMillis(maxInterval) / (float) this.window);
    this.counters = new long[trcCount];
    this.clear(TimeUtil.currentEpochMillis());
  }

  public void clear(final long now) {
    for (int i = 0; i < counters.length; ++i) {
      this.counters[i] = 0;
    }
    lastInterval = TimeUtil.alignToWindow(now, window);
    this.next = 0;
  }

  @Override
  protected long add(final long now, final long delta) {
    if ((now - lastInterval) < window) {
      final int index = Math.toIntExact(next % counters.length);
      counters[index] += delta;
      return counters[index];
    }

    final int index;
    if ((now - lastInterval) >= window) {
      injectZeros(now);
      index = Math.toIntExact(++next % counters.length);
      lastInterval = TimeUtil.alignToWindow(now, window);
      counters[index] = getPrevValue() + delta;
    } else {
      index = Math.toIntExact(next % counters.length);
      counters[index] += delta;
    }
    return counters[index];
  }

  @Override
  public TimeRangeGaugeSnapshot snapshot() {
    final long now = TimeUtil.currentEpochMillis();
    if ((now - lastInterval) >= window) injectZeros(now);

    final long[] data = new long[(int) Math.min(next + 1, counters.length)];
    for (int i = 0, n = data.length; i < n; ++i) {
      data[data.length - (i + 1)] = counters[Math.toIntExact((next - i) % counters.length)];
    }
    return new TimeRangeGaugeSnapshot(lastInterval, window, data);
  }

  private long getPrevValue() {
    return counters[Math.toIntExact((next - 1) % counters.length)];
  }

  private void injectZeros(final long now) {
    if ((now - lastInterval) < window) return;

    final long slots = Math.round((now - lastInterval) / (float) window) - 1;
    if (slots > 0) {
      final long value = counters[(int) (next % counters.length)];
      for (long i = 0; i < slots; ++i) {
        final long index = ++this.next;
        counters[(int) (index % counters.length)] = value;
      }
      lastInterval = TimeUtil.alignToWindow(now, window);
    }
  }
}
