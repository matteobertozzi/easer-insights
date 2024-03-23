/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.matteobertozzi.easerinsights.exporters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.EaserInsightsExporter;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.collections.queues.QueueUtil;

public abstract class AbstractEaserInsightsDatumExporter implements EaserInsightsExporter, EaserInsightsExporter.DatumBufferFlusher {
  private final BatchDatumExporter batchDatumExporter = new BatchDatumExporter();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread datumProcessorThread;

  @Override
  public void close() throws IOException {
    stop();
  }

  @Override
  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    datumProcessorThread = Thread.ofVirtual().name(name() + "DatumProcessor").start(this::datumBufferProcessor);
    Logger.debug("starting {} on virtual thread {}", this, datumProcessorThread);
  }

  @Override
  public void stop() {
    if (!running.compareAndSet(true, false)) {
      return;
    }

    Logger.ignoreException("datumProcessor", "joining", datumProcessorThread::join);
  }

  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void datumBufferFlushAsync(final byte[] page) {
    batchDatumExporter.datumBufferFlushAsync(page);
  }

  protected abstract void datumBufferProcessor();

  protected <T> void processDatumBufferAsBatch(final DatumBufferReader datumBufferReader,
      final ArrayList<T> datumBatch, final Function<DatumBufferEntry, T> transformer, final int maxBatchSize,
      final Consumer<Collection<T>> flusher) {
    batchDatumExporter.processDatumBufferAsBatch(datumBufferReader, datumBatch, transformer, maxBatchSize, flusher);
  }

  private static final class BatchDatumExporter implements EaserInsightsExporter.DatumBufferFlusher {
    private final LinkedTransferQueue<byte[]> datumBuffers = new LinkedTransferQueue<>();

    @Override
    public void datumBufferFlushAsync(final byte[] page) {
      this.datumBuffers.add(page);
    }

    public <T> void processDatumBufferAsBatch(final DatumBufferReader datumBufferReader,
        final ArrayList<T> datumBatch, final Function<DatumBufferEntry, T> transformer, final int maxBatchSize,
        final Consumer<Collection<T>> flusher) {
      byte[] datumBuffer = QueueUtil.pollWithoutInterrupt(datumBuffers, 1, TimeUnit.SECONDS);
      while (datumBuffer != null) {
        datumBufferReader.resetPage(datumBuffer);
        while (datumBufferReader.hasNext()) {
          final DatumBufferEntry entry = datumBufferReader.next();
          datumBatch.add(transformer.apply(entry));

          // flush if we reached the maximum batch size
          if (datumBatch.size() == maxBatchSize) {
            flusher.accept(datumBatch);
            datumBatch.clear();
          }
        }

        // fetch another buffer
        datumBuffer = datumBuffers.isEmpty() ? null : datumBuffers.poll();
      }
    }
  }
}
