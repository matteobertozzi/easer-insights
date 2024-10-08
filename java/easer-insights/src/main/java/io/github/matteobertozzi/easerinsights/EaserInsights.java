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

import java.io.IOException;
import java.util.ArrayList;

import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.bytes.encoding.IntEncoder;
import io.github.matteobertozzi.rednaco.strings.Base32;
import io.github.matteobertozzi.rednaco.util.RandData;

public final class EaserInsights implements AutoCloseable {
  public static final EaserInsights INSTANCE = new EaserInsights();
  public static final String INSTANCE_ID = timeBasedId128();

  private final ArrayList<EaserInsightsExporter> exporters = new ArrayList<>();
  private final EaserInsightsExporterQueue exporterQueue = new EaserInsightsExporterQueue();

  private EaserInsights() {
    // no-op
  }

  public EaserInsights open() {
    exporterQueue.start();
    return this;
  }

  @Override
  public void close() {
    exporterQueue.stop();

    while (!exporters.isEmpty()) {
      final EaserInsightsExporter exporter = exporters.removeLast();
      Logger.ignoreException(exporter.name(), "stopping", exporter::stop);
      Logger.ignoreException(exporter.name(), "closing", exporter::close);
    }
  }

  public void addExporter(final EaserInsightsExporter exporter) throws IOException {
    exporter.start();
    exporters.add(exporter);
    if (exporter instanceof final EaserInsightsExporter.DatumBufferFlusher flusher) {
      exporterQueue.subscribeToDatumBuffer(flusher);
    }
  }

  private static String timeBasedId128() {
    final long timestamp = System.currentTimeMillis();
    final long high = (timestamp << 16) | RandData.generateInt() & 0xffffL;
    final long low = RandData.generateLong();
    final byte[] buf = new byte[16];
    IntEncoder.BIG_ENDIAN.writeFixed64(buf, 0, high);
    IntEncoder.BIG_ENDIAN.writeFixed64(buf, 8, low);
    return Base32.base32hex().encode(buf);
  }
}
