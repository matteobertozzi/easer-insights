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

import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK;
import io.github.matteobertozzi.easerinsights.tracing.TraceId;
import io.github.matteobertozzi.easerinsights.tracing.Tracer;
import io.github.matteobertozzi.rednaco.time.TimeRange;
import io.github.matteobertozzi.rednaco.time.TimeRange.ResetTimeRangeSlots;
import io.github.matteobertozzi.rednaco.time.TimeRange.UpdateTimeRangeSlot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

public class TopKImplSt implements TopK, ResetTimeRangeSlots, UpdateTimeRangeSlot {
  private final TopList[] slots;
  private final TimeRange timeRange;
  private final int k;

  private String newKey;
  private long newTimestamp;
  private long newValue;

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
    newKey = key;
    newTimestamp = timestamp;
    newValue = value;
    timeRange.update(timestamp, slots.length, this, this);
    newKey = null;
  }


  @Override
  public void updateTimeRangeSlot(final int slotIndex) {
    slots[slotIndex].add(newKey, newTimestamp, newValue);
  }

  @Override
  public void resetTimeRangeSlots(final int fromIndex, final int toIndex) {
    for (int i = fromIndex; i < toIndex; ++i) {
      slots[i].clear();
    }
  }

  @Override
  public TopKSnapshot dataSnapshot() {
    return snapshotFrom(dataSnapshotTopList());
  }

  protected TopList dataSnapshotTopList() {
    final long now = TimeUtil.currentEpochMillis();
    timeRange.update(now, slots.length, this);

    final TopList topEntries = new TopList(k);
    timeRange.iterReverse(slots.length, index -> topEntries.merge(slots[index]));
    return topEntries;
  }

  protected static TopKSnapshot snapshotFrom(final TopList topEntries) {
    if (topEntries.isEmpty()) return TopKSnapshot.EMPTY_SNAPSHOT;

    topEntries.sort();
    final TopEntrySnapshot[] entries = new TopEntrySnapshot[topEntries.size()];
    for (int i = 0; i < entries.length; ++i) {
      final TopEntry entry = topEntries.entries[i];
      final String[] traceIds = entry.toTraceIdArray();
      entries[i] = new TopEntrySnapshot(entry.key, entry.maxTimestamp, entry.maxValue, entry.minValue, entry.sum, entry.sumSquares, entry.count, traceIds);
    }
    return new TopKSnapshot(entries);
  }

  private static final class TopEntry implements Comparable<TopEntry> {
    private final TraceId[] traceIds = new TraceId[8];
    private String key;
    private int keyHash;
    private long traceIdOffset;
    private long maxTimestamp;
    private long maxValue;
    private long minValue;
    private long sum;
    private long sumSquares;
    private long count;

    private TopEntry() {
      reset();
    }

    private boolean isEmpty() {
      return key == null;
    }

    private String[] toTraceIdArray() {
      final String[] result = new String[(int)Math.min(traceIdOffset, traceIds.length)];
      for (int i = 0; i < result.length; ++i) {
        result[i] = traceIds[i].toString();
      }
      return result;
    }

    private TopEntry reset() {
      Arrays.fill(traceIds, null);
      this.key = null;
      this.keyHash = 0;
      this.maxTimestamp = 0;
      this.maxValue = Long.MIN_VALUE;
      this.minValue = Long.MAX_VALUE;
      this.sumSquares = 0;
      this.sum = 0;
      this.count = 0;
      this.traceIdOffset = 0;
      return this;
    }

    private TopEntry reset(final String key, final long timestamp, final long value) {
      Arrays.fill(traceIds, null);
      this.traceIds[0] = Tracer.getThreadLocalSpan().traceId();
      this.traceIdOffset = 1;
      this.key = key;
      this.keyHash = key.hashCode();
      this.maxTimestamp = timestamp;
      this.maxValue = value;
      this.minValue = value;
      this.sumSquares = value * value;
      this.sum = value;
      this.count = 1;
      return this;
    }

    private void update(final long timestamp, final long value) {
      if (value >= this.maxValue) {
        this.traceIds[(int)(this.traceIdOffset++ & 7)] = Tracer.getThreadLocalSpan().traceId();
        this.maxTimestamp = timestamp;
        this.maxValue = value;
      }
      this.minValue = Math.min(minValue, value);
      this.sumSquares += value * value;
      this.sum += value;
      this.count++;
    }

    private void replace(final TopEntry other) {
      System.arraycopy(other.traceIds, 0, traceIds, 0, traceIds.length);
      this.traceIdOffset = other.traceIdOffset;
      this.key = other.key;
      this.keyHash = other.keyHash;
      this.maxTimestamp = other.maxTimestamp;
      this.maxValue = other.maxValue;
      this.minValue = other.minValue;
      this.sumSquares = other.sumSquares;
      this.sum = other.sum;
      this.count = other.count;
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

      if (traceIdOffset != traceIds.length) {
        mergeTraceIds(other);
      }
    }

    private void mergeTraceIds(final TopEntry other) {
      if (other.traceIdOffset > traceIds.length) {
        final int index = (int) (other.traceIdOffset - traceIds.length);
        for (int i = 0; i < index && traceIdOffset < traceIds.length; ++i) {
          this.traceIds[(int)traceIdOffset++] = other.traceIds[i];
        }
      } else {
        for (int i = (int)(other.traceIdOffset - 1); i >= 0 && traceIdOffset < traceIds.length; --i) {
          this.traceIds[(int)traceIdOffset++] = other.traceIds[i];
        }
      }
    }

    @Override
    public int compareTo(final TopEntry other) {
      int cmp = Long.compare(other.maxValue, maxValue);
      if (cmp != 0) return cmp;

      cmp = Long.compare(other.maxTimestamp, maxTimestamp);
      if (cmp != 0) return cmp;

      cmp = Long.compare(other.sum, sum);
      if (cmp != 0) return cmp;

      if (key == null) return 1;
      if (other.key == null) return -1;
      return key.compareTo(other.key);
    }

    @Override
    public String toString() {
      return "TopEntry [key=" + key + ", maxTimestamp=" + maxTimestamp
          + ", maxValue=" + maxValue + ", minValue="
          + minValue + ", sumValues=" + sum + ", count=" + count + "]";
    }
  }

  protected static final class TopList {
    private final TopEntry[] entries;

    private TopList(final int k) {
      entries = new TopEntry[k];
      for (int i = 0; i < entries.length; ++i) {
        entries[i] = new TopEntry();
      }
    }

    private boolean isEmpty() {
      for (int i = 0; i < entries.length; ++i) {
        if (!entries[i].isEmpty()) {
          return false;
        }
      }
      return true;
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
        entries[i].reset();
      }
    }

    private void sort() {
      Arrays.sort(entries);
    }

    private void merge(final TopList other) {
      for (int i = 0; i < other.entries.length; ++i) {
        merge(other.entries[i]);
      }
    }

    private void merge(final TopEntry mergeEntry) {
      final String key = mergeEntry.key;
      final int keyHash = mergeEntry.keyHash;
      boolean isGtThanCurrent = false;
      for (int i = 0; i < entries.length; i++) {
        final TopEntry entry = entries[i];
        final boolean hasKey = entry.key != null;
        if (hasKey) {
          if (keyHash == entry.keyHash && key.equals(entry.key)) {
            entry.merge(mergeEntry);
            return;
          }

          isGtThanCurrent |= (mergeEntry.maxValue > entry.maxValue);
        } else {
          // we have reached the end of populated entries
          // we are sure the key is not in the TopK list.
          entry.replace(mergeEntry);
          return;
        }
      }

      if (isGtThanCurrent) {
        // drop the last entry and replace it with the new key
        Arrays.sort(entries);
        entries[entries.length - 1].replace(mergeEntry);
      }
    }

    private void add(final String key, final long timestamp, final long value) {
      final int keyHash = key.hashCode();
      boolean isGtThanCurrent = false;
      for (int i = 0; i < entries.length; i++) {
        final TopEntry entry = entries[i];
        final boolean hasKey = entry.key != null;
        if (hasKey) {
          if (keyHash == entry.keyHash && key.equals(entry.key)) {
            entry.update(timestamp, value);
            return;
          }

          isGtThanCurrent |= (value >= entry.maxValue);
        } else {
          // we have reached the end of populated entries
          // we are sure the key is not in the TopK list.
          entry.reset(key, timestamp, value);
          return;
        }
      }

      if (isGtThanCurrent) {
        // drop the last entry and replace it with the new key
        Arrays.sort(entries);
        entries[entries.length - 1].reset(key, timestamp, value);
      }
    }
  }
}
