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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.EaserInsightsExporter;
import io.github.matteobertozzi.easerinsights.util.ThreadUtil;

public class BatchDatumExporter implements EaserInsightsExporter.DatumBufferFlusher {
  private final LinkedTransferQueue<byte[]> datumBuffers = new LinkedTransferQueue<>();

  @Override
  public void datumBufferFlushAsync(final byte[] page) {
    this.datumBuffers.add(page);
  }

  public <T> void processDatumBufferAsBatch(final DatumBufferReader datumBufferReader,
      final ArrayList<T> datumBatch, final Function<DatumBufferEntry, T> transformer, final int maxBatchSize,
      final Consumer<Collection<T>> flusher) {
    byte[] datumBuffer = ThreadUtil.poll(datumBuffers, 1, TimeUnit.SECONDS);
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
