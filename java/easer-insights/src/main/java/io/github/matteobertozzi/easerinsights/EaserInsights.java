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
import java.util.logging.Logger;

import io.github.matteobertozzi.easerinsights.util.ThreadUtil;

public final class EaserInsights implements AutoCloseable {
  public static final EaserInsights INSTANCE = new EaserInsights();

  private static final Logger LOGGER = Logger.getLogger("EaserInsights");

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
      final EaserInsightsExporter exporter = exporters.remove(exporters.size() - 1);
      ThreadUtil.ignoreException(exporter.name(), "stopping", exporter::stop);
      ThreadUtil.ignoreException(exporter.name(), "closing", exporter::close);
    }
  }

  public void addExporter(final EaserInsightsExporter exporter) throws IOException {
    exporter.start();
    exporters.add(exporter);
    if (exporter instanceof final EaserInsightsExporter.DatumBufferFlusher flusher) {
      exporterQueue.subscribeToDatumBuffer(flusher);
    }
  }
}
