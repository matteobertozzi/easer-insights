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

package io.github.matteobertozzi.easerinsights.demo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumBuffer;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.EaserInsights;
import io.github.matteobertozzi.easerinsights.exporters.AbstractEaserInsightsDatumExporter;
import io.github.matteobertozzi.easerinsights.jvm.BuildInfo;
import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimension;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.easerinsights.metrics.collectors.CounterMap;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Heatmap;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK;
import io.github.matteobertozzi.easerinsights.util.TimeUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;

public final class DemoMain {
  private static final MetricDimension<Histogram> uriExecTime = Metrics.newCollectorWithDimensions()
    .dimensions("uri")
    .unit(DatumUnit.MILLISECONDS)
    .name("http_endpoint_exec_time")
    .label("HTTP Exec Time")
    .register(() -> Histogram.newSingleThreaded(new long[] { 5, 10, 25, 50, 75, 100, 250, 500, 1000 }));

  private static final TimeRangeCounter reqCount = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("http_req_count")
    .label("HTTP Request Count")
    .register(TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final CounterMap reqMap = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("http_req_map")
    .label("HTTP Request Map")
    .register(CounterMap.newMultiThreaded());

  private static final MaxAvgTimeRangeGauge execTime = Metrics.newCollector()
    .unit(DatumUnit.MILLISECONDS)
    .name("http_exec_time")
    .label("HTTP Exec Time")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private static final Heatmap execTimeHeatmap = Metrics.newCollector()
    .unit(DatumUnit.MILLISECONDS)
    .name("http_exec_time_heatmap")
    .label("HTTP Exec Time")
    .register(Heatmap.newMultiThreaded(60, 1, TimeUnit.MINUTES, Histogram.DEFAULT_DURATION_BOUNDS_MS));

  private static final TopK topExecTime = Metrics.newCollector()
    .unit(DatumUnit.MILLISECONDS)
    .name("http_top_exec_time")
    .label("HTTP Top Exec Time")
    .register(TopK.newMultiThreaded(10, 60, 10, TimeUnit.MINUTES));


  private static final class DummyExporter extends AbstractEaserInsightsDatumExporter {

    @Override
    public String name() {
      return "dummy";
    }

    @Override
    protected void datumBufferProcessor() {
      final int AWS_MAX_DATUM_BATCH_SIZE = 1000;
      final ArrayList<DatumBufferEntry> datumBatch = new ArrayList<>(AWS_MAX_DATUM_BATCH_SIZE);
      final DatumBufferReader datumBufferReader = DatumBuffer.newReader();
      while (isRunning()) {
        processDatumBufferAsBatch(datumBufferReader, datumBatch,
          entry -> entry, AWS_MAX_DATUM_BATCH_SIZE,
          entries -> System.out.println("process " + entries.size()));
      }
    }
  }

  public static void main(final String[] args) throws Exception {
    final BuildInfo buildInfo = BuildInfo.loadInfoFromManifest("example-http-service-insights");
    System.out.println(buildInfo);
    JvmMetrics.INSTANCE.setBuildInfo(buildInfo);

    EaserInsights.INSTANCE.open();
    EaserInsights.INSTANCE.addExporter(new DummyExporter());

    HttpServer.runServer(57025, Map.of(
      "/metrics", (ctx, req, query) -> {
        final String report = MetricsRegistry.INSTANCE.humanReport();
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.OK, HttpHeaderValues.TEXT_PLAIN, report.getBytes(StandardCharsets.UTF_8));
      },
      "/metrics/json", (ctx, req, query) -> {
        HttpServer.sendResponse(ctx, req, query, MetricsRegistry.INSTANCE.snapshot());
      },
      "*", (ctx, req, query) -> {
        final StringBuilder report = new StringBuilder();
        report.append("path: ").append(query.path()).append("\n");
        report.append("params: ").append(query.parameters()).append("\n");
        Thread.sleep(Math.round(Math.random() * 250));
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.NOT_FOUND, HttpHeaderValues.TEXT_PLAIN, report.toString().getBytes(StandardCharsets.UTF_8));
      }
    ), (req, query, resp, elapsedMs) -> {
      final long now = TimeUtil.currentEpochMillis();
      reqCount.inc(now);
      reqMap.inc(query.path(), now);
      execTime.sample(now, elapsedMs);
      execTimeHeatmap.sample(now, elapsedMs);
      topExecTime.sample(query.path(), now, elapsedMs);
      uriExecTime.get(query.path()).sample(now, elapsedMs);
    });
  }
}
