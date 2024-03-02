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

package io.github.matteobertozzi.easerinsights.influx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import io.github.matteobertozzi.easerinsights.DatumBuffer;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.exporters.AbstractEaserInsightsDatumExporter;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.rednaco.threading.ThreadUtil;

public class InfluxLineExporter extends AbstractEaserInsightsDatumExporter {
  private static final MaxAvgTimeRangeGauge influxWriteTime = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("influx.line.exporter.write.time")
    .label("Influx Line Exporter Write Time")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final TimeRangeCounter influxWriteCount = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("influx.line.exporter.write.succeded")
    .label("Influx Line Exporter Write Succeded")
    .register(TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final TimeRangeCounter influxWriteFailed = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("influx.line.exporter.write.failed")
    .label("Influx Line Export Write Failed")
    .register(TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final TimeRangeCounter influxWriteSize = Metrics.newCollector()
    .unit(DatumUnit.BYTES)
    .name("influx.line.exporter.write.size")
    .label("Influx Line Export Write Size")
    .register(TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final int MAX_DATUM_BATCH_SIZE = 10_000;
  private final ArrayList<String> dimensions = new ArrayList<>();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  private final String url;
  private final String authToken;
  private int maxBatchSize;

  private InfluxLineExporter(final String url, final String authToken) {
    this.url = url;
    this.authToken = authToken;
    this.maxBatchSize = MAX_DATUM_BATCH_SIZE;
  }

  public static InfluxLineExporter newInfluxExporter(final String url, final String userId, final String token) {
    Logger.debug("new influx exporter for user {}: {}", userId, url);
    return new InfluxLineExporter(url, "Bearer " + userId + ':' + token);
  }

  public static InfluxLineExporter newInfluxExporter(final String url, final String token) {
    Logger.debug("new influx exporter: {}", url);
    return new InfluxLineExporter(url, "Token " + token);
  }

  public InfluxLineExporter addDefaultDimension(final String name, final String value) {
    dimensions.add(name);
    dimensions.add(value);
    return this;
  }

  public InfluxLineExporter addDefaultDimensions(final Map<String, String> defaultDimensions) {
    for (final Entry<String, String> entry: defaultDimensions.entrySet()) {
      addDefaultDimension(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public InfluxLineExporter setMaxBatchSize(final int maxBatchSize) {
    this.maxBatchSize = maxBatchSize;
    return this;
  }

  @Override
  public String name() {
    return "Influx-LineExporter";
  }

  @Override
  public void close() throws IOException {
    super.close();
    Logger.ignoreException("influx", "closing", httpClient::close);
  }

  @Override
  protected void datumBufferProcessor() {
    final DatumBufferReader datumBufferReader = DatumBuffer.newReader();
    final ArrayList<String> datumBatch = new ArrayList<>(MAX_DATUM_BATCH_SIZE);
    while (isRunning()) {
      try {
        processDatumBufferAsBatch(datumBufferReader, datumBatch,
          this::influxLineFromEntry, maxBatchSize,
          this::influxWriteData);
      } catch (final Throwable e) {
        Logger.error(e, "unable to process batch");
      }
    }
    Logger.debug("Influx Exporter terminated!");
  }

  private void influxWriteData(final Collection<String> datumBatch) {
    final long startTime = System.nanoTime();
    try {
      final byte[] gzipData = gzipInfluxLines(datumBatch);
      final HttpResponse<String> resp = httpClient.send(HttpRequest.newBuilder()
        .uri(new URI(url))
        .header("Content-Type", "text/plain")
        .header("Content-Encoding", "gzip")
        .header("Authorization", authToken)
        .POST(BodyPublishers.ofByteArray(gzipData))
        .build(), BodyHandlers.ofString());
      if (resp.statusCode() == 204 || resp.statusCode() == 200) {
        Logger.info("Influx exporter {} entries: {} {}", datumBatch.size(), resp.statusCode(), resp.body());
        influxWriteCount.inc();
      } else if (resp.statusCode() == 429) {
        // Too Many Requests... wait a bit
        ThreadUtil.sleep(2500);
      } else {
        Logger.error("Influx Exporter failed to write influx data {}, discarding metric data: {statusCode} {}", url, resp.statusCode(), resp.body());
        influxWriteFailed.inc();
      }
      influxWriteSize.add(gzipData.length);
    } catch (final Throwable e) {
      Logger.error(e, "Influx Exporter failure, discarding metric data");
      influxWriteFailed.inc();
    } finally {
      final long elapsedNs = System.nanoTime() - startTime;
      influxWriteTime.sample(elapsedNs);
    }
  }

  private String influxLineFromEntry(final DatumBufferEntry entry) {
    final MetricCollector collector = MetricsRegistry.INSTANCE.get(entry.metricId());
    final StringBuilder builder = new StringBuilder(80);
    builder.append(collector.definition().name());
    for (int i = 0; i < dimensions.size(); i += 2) {
      builder.append(',').append(dimensions.get(i)).append('=');
      escape(builder, dimensions.get(i + 1));
    }
    if (collector.definition().hasDimensions()) {
      final String[] dimKeys = collector.definition().dimensionKeys();
      final String[] dimVals = collector.definition().dimensionValues();
      for (int i = 0; i < dimKeys.length; ++i) {
        builder.append(',').append(dimKeys[i]).append('=');
        escape(builder, dimVals[i]);
      }
    }
    builder.append(" value=");
    builder.append(entry.value());
    builder.append("i ");
    builder.append(entry.timestamp() * 1_000_000L);
    return builder.toString();
  }

  private static byte[] gzipInfluxLines(final Collection<String> lines) throws IOException {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream(1 << 20)) {
      try (GZIPOutputStream gzip = new GZIPOutputStream(stream)) {
        for (final String line: lines) {
          gzip.write(line.getBytes(StandardCharsets.UTF_8));
          gzip.write('\n');
        }
      }
      return stream.toByteArray();
    }
  }

  private static void escape(final StringBuilder buf, final String text) {
    final int length = text.length();
    int index = 0;
    for (; index < length; ++index) {
      final char c = text.charAt(index);
      if (c == ' ' || c == '"') {
        break;
      }
    }

    if (index == length) {
      buf.append(text);
      return;
    }

    buf.append(text, 0, index);
    for (; index < length; ++index) {
      final char c = text.charAt(index);
      switch (c) {
        case ' ' -> buf.append("\\ ");
        case '"' -> buf.append("\\\"");
        default -> buf.append(c);
      }
    }
  }
}
