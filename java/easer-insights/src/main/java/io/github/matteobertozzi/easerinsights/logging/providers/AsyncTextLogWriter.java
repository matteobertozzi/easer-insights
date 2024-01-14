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

package io.github.matteobertozzi.easerinsights.logging.providers;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.logging.LogBuffer;
import io.github.matteobertozzi.easerinsights.logging.providers.LogBufferUtil.AbstractLogBuffer;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.rednaco.bytes.ByteArrayAppender;
import io.github.matteobertozzi.rednaco.strings.StringFormat;
import io.github.matteobertozzi.rednaco.threading.BenchUtil;
import io.github.matteobertozzi.rednaco.threading.StripedLock;
import io.github.matteobertozzi.rednaco.threading.StripedLock.Cell;
import io.github.matteobertozzi.rednaco.threading.ThreadUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil;
import io.github.matteobertozzi.rednaco.util.BitUtil;

public class AsyncTextLogWriter implements AutoCloseable {
  private final AtomicBoolean running = new AtomicBoolean(true);

  private final ArrayBlockingQueue<RecyclableBlock> poolQueue;
  private final PublishQueue publishQueue;

  private final StripedLock<AsyncTextLogBuffer> locks;
  private final PrintStream stream;
  private final Thread writerThread;

  public AsyncTextLogWriter(final PrintStream stream, final int blockSize) {
    final int stripes = BitUtil.nextPow2(Runtime.getRuntime().availableProcessors());

    publishQueue = new PublishQueue(stripes * 2);
    poolQueue = new ArrayBlockingQueue<>(stripes * 2);
    for (int i = 0; i < stripes; ++i) {
      poolQueue.add(new RecyclableBlock(blockSize, true));
    }

    this.locks = new StripedLock<>(stripes, () -> new AsyncTextLogBuffer(blockSize, poolQueue, publishQueue));
    this.stream = stream;
    this.writerThread = new Thread(this::writeLoop, "AsyncTextLogWriter");
    this.writerThread.start();
  }

  @Override
  public void close() {
    forceFlush();
    running.set(false);
    ThreadUtil.shutdown(writerThread);
  }

  public Cell<AsyncTextLogBuffer> get() {
    return locks.get();
  }

  public void forceFlush() {
    for (int i = 0, n = locks.stripes(); i < n; ++i) {
      final Cell<AsyncTextLogBuffer> cell = locks.get(i);
      cell.lock();
      try {
        cell.data().forceFlush();
      } finally {
        cell.unlock();
      }
    }
  }


  private final class Flusher {
    private final MaxAvgTimeRangeGauge flushSize = Metrics.newCollector()
      .unit(DatumUnit.BYTES)
      .name("easer.insights.logger.flush.size")
      .label("Async Logger Flush Size")
      .register(MaxAvgTimeRangeGauge.newSingleThreaded(60, 1, TimeUnit.MINUTES));

    private final MaxAvgTimeRangeGauge flushTime = Metrics.newCollector()
      .unit(DatumUnit.NANOSECONDS)
      .name("easer.insights.logger.flush.time")
      .label("Async Logger Flush Time")
      .register(MaxAvgTimeRangeGauge.newSingleThreaded(60, 1, TimeUnit.MINUTES));

    private final TimeRangeCounter unpooledBlocks = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("easer.insights.logger.unpooled.blocks")
    .label("Async Logger Unpooled Blocks")
    .register(TimeRangeCounter.newSingleThreaded(60, 1, TimeUnit.MINUTES));

    private final ArrayList<RecyclableBlock> items;

    private Flusher() {
      this.items = new ArrayList<>(locks.stripes());
    }

    public boolean poll() {
      if (!publishQueue.poll(items, 250, TimeUnit.MILLISECONDS)) {
        return false;
      }

      for (final RecyclableBlock block: items) {
        final long startTime = System.nanoTime();
        final int flushBlkSize = block.transferTo(stream);
        final long now = TimeUtil.currentEpochMillis();
        flushTime.sample(now, System.nanoTime() - startTime);
        flushSize.sample(now, flushBlkSize);
        if (block.pooled()) {
          block.reset();
          poolQueue.add(block);
        } else {
          unpooledBlocks.inc(now);
        }
      }
      items.clear();
      return true;
    }
  }

  private void writeLoop() {
    final long flushInterval = TimeUnit.SECONDS.toNanos(2);
    long lastForceFlushNs = System.nanoTime();
    final Flusher flusher = new Flusher();

    while (running.get()) {
      flusher.poll();

      final long now = System.nanoTime();
      if ((now - lastForceFlushNs) > 0) {
        timedForceFlush(flushInterval, flusher::poll);
        lastForceFlushNs = now;
      }
    }

    flusher.poll();
  }

  private void timedForceFlush(final long forceFlushInterval, final Runnable flush) {
    final long now = System.nanoTime();
    for (int i = 0, n = locks.stripes(); i < n; ++i) {
      final Cell<AsyncTextLogBuffer> cell = locks.get(i);
      final AsyncTextLogBuffer logBuf = cell.data();
      if ((now - logBuf.lastFlushNs()) > forceFlushInterval) {
        boolean flushed;
        cell.lock();
        try {
          flushed = logBuf.forceFlush();
        } finally {
          cell.unlock();
        }

        if (flushed) {
          // Force the flush for the above
          flush.run();
        }
      }
    }
  }

  private static final class RecyclableBlock {
    private final byte[] block;
    private final boolean pooled;
    private int offset;

    private RecyclableBlock(final int blockSize) {
      this(blockSize, false);
    }

    private RecyclableBlock(final int blockSize, final boolean pooled) {
      this.block = new byte[blockSize];
      this.pooled = pooled;
    }

    public boolean pooled() { return pooled; }
    public boolean isEmpty() { return offset == 0; }
    public boolean isNotEmpty() { return offset != 0; }
    public boolean isFull() { return offset == block.length; }

    public void reset() {
      this.offset = 0;
    }

    public int available() {
      return block.length - offset;
    }

    public int add(final int value) {
      block[offset++] = (byte) (value & 0xff);
      return offset;
    }

    public int add(final byte[] buf, final int off, final int len) {
      System.arraycopy(buf, off, block, offset, len);
      offset += len;
      return offset;
    }

    public void add(final CharSequence csq, final int off, final int len) {
      for (int i = 0; i < len; ++i) {
        block[offset++] = (byte) csq.charAt(off + i);
      }
    }

    public void split(final RecyclableBlock newBlock, final int splitOffset) {
      newBlock.add(block, splitOffset, offset - splitOffset);
      offset = splitOffset;
    }

    public int transferTo(final PrintStream stream) {
      stream.write(block, 0, offset);
      return offset;
    }
  }

  public final class AsyncTextLogBuffer extends AbstractLogBuffer implements ByteArrayAppender {
    private final ArrayList<RecyclableBlock> localBlocks = new ArrayList<>();
    private final AtomicLong lastFlushNs = new AtomicLong(System.nanoTime());
    private final ArrayBlockingQueue<RecyclableBlock> poolQueue;
    private final PublishQueue publishQueue;
    private final int blockSize;

    private RecyclableBlock currentBlock;
    private int currentEntryLen = 0;
    private int lastEntryBlkOff;

    private int entryAvgLen = 128;
    private long entrySumLen;
    private long entryCount;

    private AsyncTextLogBuffer(final int blockSize, final ArrayBlockingQueue<RecyclableBlock> poolQueue, final PublishQueue publishQueue) {
      this.blockSize = blockSize;
      this.poolQueue = poolQueue;
      this.publishQueue = publishQueue;
      this.currentBlock = new RecyclableBlock(blockSize, true);
    }

    private long lastFlushNs() {
      return lastFlushNs.get();
    }

    public void commitEntry() {
      // add new line
      if (currentBlock.isFull()) flushBlock();
      lastEntryBlkOff = currentBlock.add('\n');

      // if there is less space than avg, push to the writer
      if (currentBlock.available() < entryAvgLen) {
        forceFlush();
      }

      // stats
      entrySumLen += currentEntryLen;
      entryCount++;
      entryAvgLen = (int) (entrySumLen / entryCount);
      currentEntryLen = 0;
    }

    private boolean forceFlush() {
      if (currentBlock.isEmpty() && localBlocks.isEmpty()) {
        return false;
      }

      localBlocks.add(currentBlock);
      pushBlocksToWriter();
      this.currentBlock = fetchBlock();
      this.lastEntryBlkOff = 0;
      return true;
    }

    private void flushBlock() {
      if (currentEntryLen < blockSize && localBlocks.size() > 3) {
        // split the block to limit memory usage
        final RecyclableBlock newBlock = new RecyclableBlock(blockSize);
        currentBlock.split(newBlock, lastEntryBlkOff);
        localBlocks.add(currentBlock);
        this.currentBlock = newBlock;
        this.lastEntryBlkOff = 0;
        pushBlocksToWriter();
      } else {
        // add the block to the local queue
        localBlocks.add(currentBlock);
        // fetch a new block
        this.currentBlock = fetchBlock();
        this.lastEntryBlkOff = 0;
      }
    }

    private RecyclableBlock fetchBlock() {
      RecyclableBlock newBlock = poolQueue.poll();
      if (newBlock != null) return newBlock;

      newBlock = new RecyclableBlock(blockSize);
      return newBlock;
    }

    private void pushBlocksToWriter() {
      publishQueue.publish(localBlocks);
      localBlocks.clear();
      entrySumLen = entryAvgLen;
      entryCount = 1;
    }

    @Override
    public void add(final int value) {
      if (currentBlock.isFull()) flushBlock();
      currentBlock.add(value);
      currentEntryLen++;
    }

    @Override
    public void add(final byte[] buf) {
      add(buf, 0, buf.length);
    }

    @Override
    public void add(final byte[] buf, int off, int len) {
      int bufAvail = currentBlock.available();
      while (currentBlock.isNotEmpty() && len >= bufAvail) {
        currentEntryLen += bufAvail;
        currentBlock.add(buf, off, bufAvail);
        flushBlock();
        off += bufAvail;
        len -= bufAvail;
        bufAvail = currentBlock.available();
      }
      while (len >= blockSize) {
        currentEntryLen += blockSize;
        currentBlock.add(buf, off, blockSize);
        flushBlock();
        off += blockSize;
        len -= blockSize;
      }
      if (len > 0) {
        currentEntryLen += len;
        currentBlock.add(buf, off, len);
      }
    }

    @Override
    public LogBuffer append(final char ch) {
      if (currentBlock.available() < 3) flushBlock();

      final RecyclableBlock block = this.currentBlock;
      if (ch < 0x80) {
        block.add(ch);
      } else if (ch < 0x800) { // 11-bit character
        block.add(0xc0 | (ch >>> 6) & 0xff);
        block.add(0x80 | (ch & 0x3f));
      } else if (ch < 0xd800 || ch > 0xdfff) { // 16-bit character
        block.add(0xe0 | (ch >>> 12) & 0xff);
        block.add(0x80 | ((ch >>> 6) & 0x3f));
        block.add(0x80 | (ch & 0x3f));
      } else {
        throw new UnsupportedOperationException();
      }
      return this;
    }

    @Override
    public LogBuffer append(final CharSequence csq, final int fromIndex, final int toIndex) {
      for (int i = fromIndex; i < toIndex; i++) {
        if (currentBlock.available() < 4) flushBlock();

        final RecyclableBlock block = this.currentBlock;
        char ch = csq.charAt(i);
        if (ch < 0x80) { // 7-bit ASCII character
          block.add(ch);
          // This could be an ASCII run, or possibly entirely ASCII
          for (int j = 1, n = Math.min(toIndex - i, block.available()); j < n; ++j) {
            ch = csq.charAt(i + 1);
            if (ch >= 0x80) break;
            i++;
            block.add(ch); // another 7-bit ASCII character
          }
        } else if (ch < 0x800) { // 11-bit character
          block.add(0xc0 | (ch >>> 6) & 0xff);
          block.add(0x80 | (ch & 0x3f));
        } else if (ch < 0xd800 || ch > 0xdfff) { // 16-bit character
          block.add(0xe0 | (ch >>> 12) & 0xff);
          block.add(0x80 | ((ch >>> 6) & 0x3f));
          block.add(0x80 | (ch & 0x3f));
        } else { // Possibly a 21-bit character
          if (!Character.isHighSurrogate(ch)) { // Malformed or not UTF-8
            block.add('?');
            continue;
          }
          if (i == toIndex - 1) { // Truncated or not UTF-8
            block.add('?');
            break;
          }
          final char low = csq.charAt(++i);
          if (!Character.isLowSurrogate(low)) { // Malformed or not UTF-8
            block.add('?');
            block.add(Character.isHighSurrogate(low) ? '?' : low);
            continue;
          }
          // Write the 21-bit character using 4 bytes
          // See http://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G2630
          final int codePoint = Character.toCodePoint(ch, low);
          block.add(0xf0 | (codePoint >>> 18) & 0xff);
          block.add(0x80 | ((codePoint >>> 12) & 0x3f));
          block.add(0x80 | ((codePoint >>> 6) & 0x3f));
          block.add(0x80 | (codePoint & 0x3f));
        }
      }
      return this;
    }

    @Override
    public LogBuffer appendAscii(final char ch) {
      if (currentBlock.isFull()) flushBlock();
      currentBlock.add(ch);
      currentEntryLen++;
      return this;
    }

    @Override
    public LogBuffer appendAscii(final CharSequence csq, int fromIndex, final int toIndex) {
      int len = toIndex - fromIndex;
      int bufAvail = currentBlock.available();
      while (currentBlock.isNotEmpty() && len >= bufAvail) {
        currentEntryLen += bufAvail;
        currentBlock.add(csq, fromIndex, bufAvail);
        flushBlock();
        fromIndex += bufAvail;
        len -= bufAvail;
        bufAvail = currentBlock.available();
      }
      while (len >= blockSize) {
        currentEntryLen += blockSize;
        currentBlock.add(csq, fromIndex, blockSize);
        flushBlock();
        fromIndex += blockSize;
        len -= blockSize;
      }
      if (len > 0) {
        currentEntryLen += len;
        currentBlock.add(csq, fromIndex, len);
      }
      return this;
    }

    @Override
    public LogBuffer appendNamedFormat(final String format, final Object[] args) {
      if (args.length == 0) return append(format);
      // TODO: avoid extra allocations...
      return append(StringFormat.namedFormat(format, args));
    }
  }

  private static final class PublishQueue {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final ArrayList<RecyclableBlock> items;

    public PublishQueue(final int capacity) {
      this.items = new ArrayList<>(capacity);
    }

    public void publish(final List<RecyclableBlock> blocks) {
      lock.lock();
      try {
        //while (items.size() > 10) {
        //  notFull.awaitUninterruptibly();
        //}
        items.addAll(blocks);
        notEmpty.signal();
      } finally {
        lock.unlock();
      }
    }

    public boolean poll(final List<RecyclableBlock> target, final long timeout, final TimeUnit unit) {
      long nanos = unit.toNanos(timeout);
      lock.lock();
      try {
        while (items.isEmpty()) {
          if (nanos <= 0) {
            return false;
          }
          nanos = notEmpty.awaitNanos(nanos);
        }
        target.addAll(items);
        items.clear();
        notFull.signal();
        return true;
      } catch (final InterruptedException e) {
        return false;
      } finally {
        lock.unlock();
      }
    }
  }

  public static void main(final String[] args) throws Throwable {
    try (AsyncTextLogWriter asyncWriter = new AsyncTextLogWriter(new PrintStream(OutputStream.nullOutputStream()), 64)) {
      BenchUtil.runInThreads("demo", 4, 2_000_000, () -> {
        final Cell<AsyncTextLogBuffer> cell = asyncWriter.get();
        cell.lock();
        try {
          final AsyncTextLogBuffer buffer = cell.data();
          buffer.appendNamedFormat("hello woop {}", new Object[] { System.nanoTime() });
          buffer.commitEntry();
        } finally {
          cell.unlock();
        }
      });
      asyncWriter.forceFlush();
    }
  }
}
