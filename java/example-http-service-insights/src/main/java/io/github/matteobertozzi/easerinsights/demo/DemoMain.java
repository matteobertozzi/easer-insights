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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jvm.BuildInfo;
import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollectorRegistry;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimensions;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;

public final class DemoMain {
  private static final MetricDimensions execTime = new MetricDimensions.Builder()
      .dimensions("uri")
      .unit(DatumUnit.MILLISECONDS)
      .name("http_exec_time")
      .label("HTTP Exec Time")
      .register(() -> Histogram.newSingleThreaded(new long[] { 5, 10, 25, 50, 75, 100, 250, 500, 1000 }));

  private static final MetricCollector reqCount = new MetricCollector.Builder()
      .unit(DatumUnit.COUNT)
      .name("http_req_count")
      .label("HTTP Request Count")
      .register(TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  public static void main(final String[] args) throws Exception {
    final BuildInfo buildInfo = BuildInfo.loadInfoFromManifest("example-http-service-insights");
    System.out.println(buildInfo);
    JvmMetrics.INSTANCE.setBuildInfo(buildInfo);

    HttpServer.runServer(57025, Map.of(
      "/metrics", (ctx, req, query) -> {
        final String report = MetricCollectorRegistry.INSTANCE.humanReport();
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.OK, HttpHeaderValues.TEXT_PLAIN, report.getBytes(StandardCharsets.UTF_8));
      },
      "/metrics/json", (ctx, req, query) -> {
        HttpServer.sendResponse(ctx, req, query, MetricCollectorRegistry.INSTANCE.snapshot());
      },
      "*", (ctx, req, query) -> {
        final StringBuilder report = new StringBuilder();
        report.append("path: ").append(query.path()).append("\n");
        report.append("params: ").append(query.parameters()).append("\n");
        HttpServer.sendResponse(ctx, req, query, HttpResponseStatus.NOT_FOUND, HttpHeaderValues.TEXT_PLAIN, report.toString().getBytes(StandardCharsets.UTF_8));
      }
    ), (req, query, resp, elapsedMs) -> {
      reqCount.update(1);
      execTime.get(query.path()).update(elapsedMs);
    });
  }
}
