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

package io.github.matteobertozzi.easerinsights;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferWriter;
import io.github.matteobertozzi.easerinsights.logger.Logger;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.rednaco.threading.ThreadUtil;

final class EaserInsightsExporterQueue {
  private final LinkedTransferQueue<byte[]> datumBuffers = new LinkedTransferQueue<>();
  private final CopyOnWriteArrayList<EaserInsightsExporter.DatumBufferFlusher> datumBufferListeners = new CopyOnWriteArrayList<>();
  private final DatumBufferWriter bufferWriter = DatumBuffer.newWriter(1000, 8192, datumBuffers::add);

  private final AtomicBoolean running = new AtomicBoolean(false);
  private boolean isListeningOnMetrics = false;
  private Thread flusherThread;

  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    flusherThread = Thread.ofVirtual().name("DatumBufferFlusher").start(this::datumBufferFlusher);
  }

  public void stop() {
    if (!running.compareAndSet(true, false)) {
      return;
    }

    if (flusherThread != null) {
      Logger.ignoreException("flusherThread", "joining", flusherThread::join);
    }
  }

  // TODO: bring back the stride
  private final ReentrantLock lock = new ReentrantLock(true);
  private void newMetricDatumCollected(final MetricCollector collector, final long timestamp, final long value) {
    lock.lock();
    try {
      bufferWriter.add(collector.metricId(), timestamp, value);
    } finally {
      lock.unlock();
    }
  }

  private void forceDatumBufferFlush() {
    lock.lock();
    try {
      bufferWriter.flush();
    } finally {
      lock.unlock();
    }
  }

  public void subscribeToDatumBuffer(final EaserInsightsExporter.DatumBufferFlusher flusher) {
    if (!isListeningOnMetrics) {
      isListeningOnMetrics = true;
      MetricsRegistry.INSTANCE.registerMetricDatumUpdateNotifier(this::newMetricDatumCollected);
    }

    datumBufferListeners.add(flusher);
  }

  private void datumBufferFlusher() {
    long lastFlushNs = System.nanoTime();
    while (running.get()) {
      try {
        final byte[] page = ThreadUtil.poll(datumBuffers, 1, TimeUnit.SECONDS);
        if (page == null) {
          if ((System.nanoTime() - lastFlushNs) > TimeUnit.MINUTES.toNanos(1)) {
            lastFlushNs = System.nanoTime();
            forceDatumBufferFlush();
          }
          continue;
        }

        lastFlushNs = System.nanoTime();
        for (final EaserInsightsExporter.DatumBufferFlusher flusher: datumBufferListeners) {
          flusher.datumBufferFlushAsync(page);
        }
      } catch (final Throwable e) {
        Logger.error(e, "failure while flushing export buffer");
      }
    }
  }
}
