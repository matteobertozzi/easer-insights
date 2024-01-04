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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumBuffer;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.EaserInsights;
import io.github.matteobertozzi.easerinsights.exporters.AbstractEaserInsightsDatumExporter;
import io.github.matteobertozzi.easerinsights.jvm.JvmMemoryMetrics;
import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.logging.providers.TextLogProvider;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimension;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.easerinsights.metrics.collectors.CounterMap;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Heatmap;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK;
import io.github.matteobertozzi.easerinsights.tracing.TaskMonitor;
import io.github.matteobertozzi.easerinsights.tracing.TraceRecorder;
import io.github.matteobertozzi.easerinsights.tracing.Tracer;
import io.github.matteobertozzi.easerinsights.tracing.providers.Base58RandSpanId;
import io.github.matteobertozzi.easerinsights.tracing.providers.Hex128RandTraceId;
import io.github.matteobertozzi.easerinsights.tracing.providers.basic.BasicTracer;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;
import io.github.matteobertozzi.rednaco.strings.TemplateUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil;
import io.github.matteobertozzi.rednaco.util.BuildInfo;
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
          entries -> System.out.println("exporter process " + entries.size()));
      }
    }
  }

  private static String loadResourceAsString(final String path) throws IOException {
    try (InputStream stream = DemoMain.class.getClassLoader().getResourceAsStream(path)) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String buildDashboardPage() throws IOException {
    return Files.readString(Path.of("../../dashboard-demo.html"));
  }

  private static String buildMonitorPage() throws IOException {
    final String template = loadResourceAsString("webapp/monitor.html");

    final StringBuilder builder = new StringBuilder(4096);
    final HashMap<String, String> templateVars = new HashMap<>(32);

    templateVars.put("now", ZonedDateTime.now().toString());

    templateVars.put("service.name", JvmMetrics.INSTANCE.buildInfo().name());
    templateVars.put("build.version", JvmMetrics.INSTANCE.buildInfo().version() + " (" + JvmMetrics.INSTANCE.buildInfo().buildDate() + ")");
    templateVars.put("service.uptime", HumansUtil.humanTimeMillis(JvmMetrics.INSTANCE.getUptime()));

    templateVars.put("memory.max", HumansUtil.humanBytes(JvmMemoryMetrics.INSTANCE.maxMemory()));
    templateVars.put("memory.allocated", HumansUtil.humanBytes(JvmMemoryMetrics.INSTANCE.totalMemory()));
    templateVars.put("memory.used", HumansUtil.humanBytes(JvmMemoryMetrics.INSTANCE.usedMemory()));

    builder.setLength(0);
    TaskMonitor.INSTANCE.addActiveTasksToHumanReport(builder);
    templateVars.put("active.tasks", builder.toString());

    builder.setLength(0);
    TaskMonitor.INSTANCE.addSlowTasksToHumanReport(builder);
    templateVars.put("slow.tasks", builder.toString());

    builder.setLength(0);
    TaskMonitor.INSTANCE.addRecentlyCompletedTasksToHumanReport(builder);
    templateVars.put("recently.completed.tasks", builder.toString());

    return TemplateUtil.processTemplate(template, templateVars);
  }

  public static void main(final String[] args) throws Exception {
    Logger.setLogProvider(new TextLogProvider());
    Tracer.setTraceProvider(BasicTracer.INSTANCE);
    Tracer.setIdProviders(Hex128RandTraceId.PROVIDER, Base58RandSpanId.PROVIDER);

    final BuildInfo buildInfo = BuildInfo.fromManifest("example-http-service-insights");
    JvmMetrics.INSTANCE.setBuildInfo(buildInfo);
    Logger.debug("starting {}", buildInfo);

    EaserInsights.INSTANCE.open();
    EaserInsights.INSTANCE.addExporter(new DummyExporter());

    HttpServer.runServer(57025, Map.of(
      "/metrics", (ctx, req, query) -> {
        final String report = MetricsRegistry.INSTANCE.humanReport();
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.OK, HttpHeaderValues.TEXT_PLAIN, report.getBytes(StandardCharsets.UTF_8));
      },
      "/metrics/data", (ctx, req, query) -> {
        HttpServer.sendResponse(ctx, req, query, MetricsRegistry.INSTANCE.snapshot());
      },
      "/metrics/dashboard", (ctx, req, query) -> {
        final String dashboard = buildDashboardPage();
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.OK, HttpHeaderValues.TEXT_HTML, dashboard.getBytes(StandardCharsets.UTF_8));
      },
      "/monitor", (ctx, req, query) -> {
        final String report = buildMonitorPage();
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.OK, HttpHeaderValues.TEXT_HTML, report.getBytes(StandardCharsets.UTF_8));
      },
      "/traces", (ctx, req, query) -> {
        HttpServer.sendResponse(ctx, req, query, TraceRecorder.INSTANCE.snapshot());
      },
      "/slow", (ctx, req, query) -> {
        final long minMillis = Long.parseLong(query.parameters().getOrDefault("minMillis", List.of("100")).get(0));
        final long maxMillis = Long.parseLong(query.parameters().getOrDefault("maxMillis", List.of("15000")).get(0));
        final StringBuilder report = new StringBuilder();
        report.append("path: ").append(query.path()).append("\n");
        report.append("params: ").append(query.parameters()).append("\n");
        Thread.sleep(minMillis + Math.round(Math.random() * maxMillis));
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.NOT_FOUND, HttpHeaderValues.TEXT_PLAIN, report.toString().getBytes(StandardCharsets.UTF_8));
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
