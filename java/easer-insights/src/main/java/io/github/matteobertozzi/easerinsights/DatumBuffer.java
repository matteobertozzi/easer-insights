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

package io.github.matteobertozzi.easerinsights;

import java.time.Instant;
import java.util.Iterator;
import java.util.NoSuchElementException;

import io.github.matteobertozzi.easerinsights.util.TimeUtil;

public final class DatumBuffer {
  private static final int MAX_ENTRY_SIZE = 1 + 3 + 6 + 8;

  private DatumBuffer() {
    // no-op
  }

  @FunctionalInterface
  public interface DatumBufferFlusher {
    void flushAsync(byte[] page);
  }

  public static DatumBufferReader newReader() {
    return new DatumBufferReader();
  }

  public static DatumBufferWriter newWriter(final int alignWindowMs, final int pageSize, final DatumBufferFlusher flusher) {
    return new DatumBufferWriter(alignWindowMs, pageSize, flusher);
  }

  public static final class DatumBufferWriter {
    private final DatumBufferFlusher flusher;
    private final int alignWindowMs;
    private final int pageSize;

    private byte[] lastPage;
    private long lastTimestamp;
    private int lastOffset;

    private DatumBufferWriter(final int alignWindowMs, final int pageSize, final DatumBufferFlusher flusher) {
      this.alignWindowMs = alignWindowMs;
      this.flusher = flusher;
      this.pageSize = pageSize;
      this.lastOffset = pageSize;
    }

    public void flush() {
      if (lastPage == null) return;

      flusher.flushAsync(lastPage);
      lastPage = null;
      lastTimestamp = 0;
      lastOffset = pageSize;
    }

    public void add(final int metricId, final long timestamp, final long value) {
      if ((pageSize - lastOffset) < MAX_ENTRY_SIZE) {
        rollPage();
      }

      final int metricSize = ((32 - Integer.numberOfLeadingZeros(metricId)) + 7) >> 3;
      final int valueSize = (value != 0) ? ((64 - Long.numberOfLeadingZeros(value)) + 7) >> 3 : 1;

      // xx xxx xxx
      // |  |   |------ Timestamp Size (0: same as last, 1-6: delta, 7: full)
      // |  |---------- Value Size
      // |------------- Metric Id
      int offset = lastOffset + 1;
      lastPage[lastOffset] = (byte) (((metricSize - 1) << 6) | ((valueSize - 1) << 3));
      writeFixed(lastPage, offset, metricId, metricSize);
      offset += metricSize;
      writeFixed(lastPage, offset, value, valueSize);
      offset += valueSize;

      final long alignedTimestamp = TimeUtil.alignToWindow(timestamp, alignWindowMs);
      final long deltaTs = alignedTimestamp - lastTimestamp;
      if (deltaTs > 0) {
        final int timestampSize = ((64 - Long.numberOfLeadingZeros(deltaTs)) + 7) >> 3;
        lastPage[lastOffset] |= (byte) (timestampSize & 0x7);
        writeFixed(lastPage, offset, deltaTs, timestampSize);
        offset += timestampSize;
      } else if (deltaTs < 0) {
        lastPage[lastOffset] |= 7;
        writeFixed(lastPage, offset, alignedTimestamp, 6);
        offset += 6;
      }

      lastTimestamp = alignedTimestamp;
      lastOffset = offset;
    }

    private void rollPage() {
      if (lastPage != null) {
        flusher.flushAsync(lastPage);
      }

      lastPage = new byte[pageSize];
      lastTimestamp = 0;
      lastOffset = 0;
    }
  }

  public static final class DatumBufferEntry {
    private int metricId;
    private long timestamp;
    private long value;

    /** @return the metricsId */
    public int metricId() { return this.metricId; }

    /** @return the timestamp (UTC) */
    public long timestamp() { return this.timestamp; }

    /** @return the value of the current measurement */
    public long value() { return this.value; }

    /** @return the Instant of the timestamp (UTC) */
    public Instant instant() { return Instant.ofEpochMilli(timestamp); }

    @Override
    public String toString() {
      return "DatumBufferEntry [metricId=" + metricId + ", timestamp=" + timestamp + ", value=" + value + "]";
    }
  }

  public static final class DatumBufferReader implements Iterator<DatumBufferEntry> {
    private final DatumBufferEntry entry = new DatumBufferEntry();
    private byte[] page;
    private long lastTimestamp;
    private int offset;

    private DatumBufferReader() {
      resetPage(null);
    }

    public DatumBufferReader resetPage(final byte[] page) {
      this.page = page;
      this.lastTimestamp = 0;
      this.offset = 0;
      return this;
    }

    @Override
    public boolean hasNext() {
      return page != null && page[offset] != 0;
    }

    @Override
    public DatumBufferEntry next() {
      if (page == null || page[offset] == 0) {
        throw new NoSuchElementException();
      }

      // xx xxx xxx
      // |  |   |------ Timestamp Size (0: same as last, 1-6: delta, 7: full)
      // |  |---------- Value Size
      // |------------- Metric Id
      final int head = this.page[offset] & 0xff;
      final int metricSize = 1 + ((head >> 6) & 3);
      final int valueSize = 1 + ((head >> 3) & 7);
      final int timestampSize = (head & 7);

      offset++;
      entry.metricId = Math.toIntExact(readFixed(page, offset, metricSize));
      offset += metricSize;

      entry.value = readFixed(page, offset, valueSize);
      offset += valueSize;

      if (timestampSize == 7) {
        lastTimestamp = readFixed(page, offset, 6);
        offset += 6;
      } else if (timestampSize > 0) {
        lastTimestamp += readFixed(page, offset, timestampSize);
        offset += timestampSize;
      }
      entry.timestamp = lastTimestamp;

      return entry;
    }
  }

  // =========================================================================================================
  @SuppressWarnings("fallthrough")
  private static void writeFixed(final byte[] buf, final int off, final long v, final int width) {
    switch (width) {
      case 8: buf[off + 7] = ((byte)((v >>> 56) & 0xff));
      case 7: buf[off + 6] = ((byte)((v >>> 48) & 0xff));
      case 6: buf[off + 5] = ((byte)((v >>> 40) & 0xff));
      case 5: buf[off + 4] = ((byte)((v >>> 32) & 0xff));
      case 4: buf[off + 3] = ((byte)((v >>> 24) & 0xff));
      case 3: buf[off + 2] = ((byte)((v >>> 16) & 0xff));
      case 2: buf[off + 1] = ((byte)((v >>> 8) & 0xff));
      case 1: buf[off] = (byte)(v & 0xff);
    }
  }

  @SuppressWarnings("fallthrough")
  private static long readFixed(final byte[] buf, final int off, final int width) {
    long result = 0;
    switch (width) {
      case 8: result |= (buf[off + 7] & 0xFFL) << 56;
      case 7: result |= (buf[off + 6] & 0xFFL) << 48;
      case 6: result |= (buf[off + 5] & 0xFFL) << 40;
      case 5: result |= (buf[off + 4] & 0xFFL) << 32;
      case 4: result |= (buf[off + 3] & 0xFFL) << 24;
      case 3: result |= (buf[off + 2] & 0xFFL) << 16;
      case 2: result |= (buf[off + 1] & 0xFFL) << 8;
      case 1: result |= buf[off] & 0xFFL;
    }
    return result;
  }
}
