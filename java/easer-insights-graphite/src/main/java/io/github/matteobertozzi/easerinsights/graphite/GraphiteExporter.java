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

package io.github.matteobertozzi.easerinsights.graphite;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumBuffer;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.exporters.AbstractEaserInsightsDatumExporter;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.rednaco.data.JsonFormat;

public class GraphiteExporter extends AbstractEaserInsightsDatumExporter {
  private static final MaxAvgTimeRangeGauge graphiteWriteTime = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("graphite.exporter.write.time")
    .label("graphite Line Exporter Write Time")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final TimeRangeCounter graphiteWriteCount = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("graphite.exporter.write.succeded")
    .label("graphite Line Exporter Write Succeded")
    .register(TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final TimeRangeCounter graphiteWriteFailed = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("graphite.exporter.write.failed")
    .label("graphite Line Export Write Failed")
    .register(TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private final ArrayList<String> dimensions = new ArrayList<>();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  private final String url;
  private final String authToken;

  private GraphiteExporter(final String url, final String userId, final String token) {
    this.url = url;
    this.authToken = "Bearer " + userId + ':' + token;
  }

  public static GraphiteExporter newGraphiteExporter(final String url, final String userId, final String token) {
    return new GraphiteExporter(url, userId, token);
  }

  public GraphiteExporter addDefaultDimension(final String name, final String value) {
    dimensions.add(name + '=' + value);
    return this;
  }

  @Override
  public String name() {
    return "Graphite-Exporter";
  }

  @Override
  public void close() throws IOException {
    super.close();
    Logger.ignoreException("graphite", "closing", httpClient::close);
  }

  @Override
  protected void datumBufferProcessor() {
    final int MAX_DATUM_BATCH_SIZE = 1000;
    final ArrayList<GraphiteMetric> datumBatch = new ArrayList<>(MAX_DATUM_BATCH_SIZE);
    final DatumBufferReader datumBufferReader = DatumBuffer.newReader();
    while (isRunning()) {
      processDatumBufferAsBatch(datumBufferReader, datumBatch,
        this::graphiteFromEntry, MAX_DATUM_BATCH_SIZE,
        this::graphiteWriteData);
    }
  }

  private void graphiteWriteData(final Collection<GraphiteMetric> datumBatch) {
    final long startTime = System.nanoTime();
    try {
      final HttpResponse<String> resp = httpClient.send(HttpRequest.newBuilder()
        .uri(new URI(url))
        .header("Content-Type", "application/json")
        .header("Authorization", authToken)
        .POST(BodyPublishers.ofByteArray(JsonFormat.INSTANCE.asBytes(datumBatch)))
        .build(), BodyHandlers.ofString());
      if (resp.statusCode() == 204 || resp.statusCode() == 200) {
        Logger.info("Graphite exporter: {} {}", resp.statusCode(), resp.body());
        graphiteWriteCount.inc();
      } else {
        Logger.error("Graphite Exporter failed to write graphite data {}, discarding metric data: {statusCode} {}", url, resp.statusCode(), resp.body());
        graphiteWriteFailed.inc();
      }
    } catch (final Throwable e) {
      Logger.error(e, "Graphite Exporter failure, discarding metric data");
      graphiteWriteFailed.inc();
    } finally {
      final long elapsedNs = System.nanoTime() - startTime;
      graphiteWriteTime.sample(elapsedNs);
    }
  }

  // Graphite Data Format
  // name     - Graphite style name (required)
  // interval - the resolution of the metric in seconds (required)
  // value    - float64 value (required)
  // time     - unix timestamp in seconds (required)
  // tags     - list of key=value pairs of tags (optional)
  record GraphiteMetric(String name, int interval, long value, long time, List<String> tags) {}

  private GraphiteMetric graphiteFromEntry(final DatumBufferEntry entry) {
    final MetricCollector collector = MetricsRegistry.INSTANCE.get(entry.metricId());
    final MetricDefinition definition = collector.definition();
    return new GraphiteMetric(definition.name(), 1, entry.value(), entry.timestamp() / 1000, buildTags(definition));
  }

  private List<String> buildTags(final MetricDefinition definition) {
    if (dimensions.isEmpty() && !definition.hasDimensions()) {
      return null;
    }

    final String[] dimKeys = definition.dimensionKeys();
    final String[] dimVals = definition.dimensionValues();
    final int defDimensions = dimKeys != null ? dimKeys.length : 0;
    final ArrayList<String> tags = new ArrayList<>(dimensions.size() + defDimensions);
    tags.addAll(dimensions);
    for (int i = 0; i < defDimensions; ++i) {
      tags.add(dimKeys[i] + '=' + dimVals[i]);
    }
    return tags;
  }
}
