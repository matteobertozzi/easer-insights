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

package io.github.matteobertozzi.easerinsights.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram;

public class TestMetricsRegistry {
  @Test
  public void testMetricId() {
    final Histogram collector = Histogram.newSingleThreaded(new long[] { 1, 2 });
    final String key = "test.metrics.registry.test.metric.id";
    MetricsRegistry.INSTANCE.register(key, DatumUnit.COUNT, "test label", "test help", collector);

    final MetricCollector fullCollector = MetricsRegistry.INSTANCE.get(key);
    Assertions.assertNotNull(fullCollector);
    Assertions.assertTrue(fullCollector.metricId() > 0, "metricId: " + fullCollector.metricId());

    Assertions.assertNotEquals(0, MetricsRegistry.INSTANCE.size());
    MetricsRegistry.INSTANCE.forEach((arr, off, len) -> {
      for (int i = 0; i < len; ++i) {
        Assertions.assertTrue(arr[off + i].metricId() > 0, "metricId: " + arr[off + i].metricId());
      }
    });
  }
}
