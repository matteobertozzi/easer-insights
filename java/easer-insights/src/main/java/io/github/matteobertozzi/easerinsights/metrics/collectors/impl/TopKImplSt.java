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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK;
import io.github.matteobertozzi.easerinsights.util.TimeRange;
import io.github.matteobertozzi.easerinsights.util.TimeUtil;

public class TopKImplSt implements TopK {
  private final TopList[] slots;
  private final TimeRange timeRange;
  private final int k;

  public TopKImplSt(final int k, final long maxInterval, final long window, final TimeUnit unit) {
    this.timeRange = new TimeRange(Math.toIntExact(unit.toMillis(window)), 0);
    this.k = k;

    final int count = (int) Math.ceil(unit.toMillis(maxInterval) / (float) timeRange.window());
    this.slots = new TopList[count];
    for (int i = 0; i < count; ++i) {
      this.slots[i] = new TopList(k);
    }
  }

  @Override
  public void sample(final String key, final long timestamp, final long value) {
    timeRange.update(timestamp, slots.length, this::resetSlots, slotIndex -> slots[slotIndex].add(key, timestamp, value));
  }

  private void resetSlots(final int fromIndex, final int toIndex) {
    for (int i = fromIndex; i < toIndex; ++i) {
      slots[i].clear();
    }
  }

  @Override
  public TopKSnapshot dataSnapshot() {
    final long now = TimeUtil.currentEpochMillis();
    timeRange.update(now, slots.length, this::resetSlots);

    final TopList topEntries = new TopList(k);
    timeRange.iterReverse(slots.length, index -> topEntries.merge(slots[index]));

    final TopEntrySnapshot[] entries = new TopEntrySnapshot[topEntries.size()];
    for (int i = 0; i < entries.length; ++i) {
      final TopEntry entry = topEntries.entries[i];
      entries[i] = new TopEntrySnapshot(entry.key, entry.maxTimestamp, entry.maxValue, entry.minValue, entry.sum, entry.sumSquares, entry.count);
    }
    return new TopKSnapshot(entries);
  }

  public static final class TopEntry implements Comparable<TopEntry> {
    private String key;
    private long maxTimestamp;
    private long maxValue;
    private long minValue;
    private long sum;
    private long sumSquares;
    private long count;

    private TopEntry() {
      reset(null);
    }

    private boolean isEmpty() {
      return key == null;
    }

    private TopEntry reset(final String key) {
      this.key = key;
      this.maxTimestamp = 0;
      this.maxValue = Long.MIN_VALUE;
      this.minValue = Long.MAX_VALUE;
      this.sumSquares = 0;
      this.sum = 0;
      this.count = 0;
      return this;
    }

    private void update(final long timestamp, final long value) {
      if (value >= this.maxValue) {
        this.maxTimestamp = timestamp;
        this.maxValue = value;
      }
      this.minValue = Math.min(minValue, value);
      this.sumSquares += value * value;
      this.sum += value;
      this.count++;
    }

    private void merge(final TopEntry other) {
      if (other.maxValue > this.maxValue) {
        this.maxValue = other.maxValue;
        this.maxTimestamp = other.maxTimestamp;
      }
      this.minValue = Math.min(minValue, other.minValue);
      this.sumSquares += other.sumSquares;
      this.sum += other.sum;
      this.count += other.count;
    }

    @Override
    public int compareTo(final TopEntry o) {
      if (this == o) return 0;

      if (key == null) return 1;
      if (o.key == null) return -1;

      int cmp = Long.compare(o.maxValue, maxValue);
      if (cmp != 0) return cmp;

      cmp = Long.compare(o.maxTimestamp, maxTimestamp);
      if (cmp != 0) return cmp;

      cmp = Long.compare(sum, o.sum);
      if (cmp != 0) return cmp;

      return key.compareTo(o.key);
    }

    @Override
    public String toString() {
      return "TopEntry [key=" + key + ", maxTimestamp=" + maxTimestamp
          + ", maxValue=" + maxValue + ", minValue="
          + minValue + ", sumValues=" + sum + ", count=" + count + "]";
    }
  }

  private static class TopList {
    private final TopEntry[] entries;

    private TopList(final int k) {
      entries = new TopEntry[k];
      for (int i = 0; i < entries.length; ++i) {
        entries[i] = new TopEntry();
      }
    }

    private int size() {
      for (int i = 0; i < entries.length; ++i) {
        if (entries[i].isEmpty()) {
          return i;
        }
      }
      return entries.length;
    }

    private void clear() {
      for (int i = 0; i < entries.length; ++i) {
        entries[i].reset(null);
      }
    }

    private void merge(final TopList other) {
      for (int i = 0; i < other.entries.length; ++i) {
        merge(other.entries[i]);
      }
    }

    private void merge(final TopEntry entry) {
      boolean isGtThanCurrent = false;
      int emptySlot = -1;
      for (int i = 0; i < entries.length; i++) {
        if (Objects.equals(entries[i].key, entry.key)) {
          entries[i].merge(entry);
          return;
        }

        isGtThanCurrent |= (entry.maxValue >= entries[i].maxValue);
        if (entries[i].isEmpty()) {
          emptySlot = i;
          break;
        }
      }

      if (emptySlot >= 0) {
        entries[emptySlot].reset(entry.key).merge(entry);
        Arrays.sort(entries);
        return;
      }

      if (isGtThanCurrent) {
        entries[entries.length - 1].reset(entry.key).merge(entry);
        Arrays.sort(entries);
      }
    }

    private void add(final String key, final long timestamp, final long value) {
      boolean isGtThanCurrent = false;
      int emptySlot = -1;
      for (int i = 0; i < entries.length; i++) {
        if (Objects.equals(entries[i].key, key)) {
          entries[i].update(timestamp, value);
          return;
        }

        isGtThanCurrent |= (value >= entries[i].maxValue);
        if (entries[i].isEmpty()) {
          emptySlot = i;
          break;
        }
      }

      if (emptySlot >= 0) {
        entries[emptySlot].reset(key).update(timestamp, value);
        Arrays.sort(entries);
        return;
      }

      if (isGtThanCurrent) {
        entries[entries.length - 1].reset(key).update(timestamp, value);
        Arrays.sort(entries);
      }
    }
  }
}