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

package io.github.matteobertozzi.easerinsights.aws.cloudwatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollectorRegistry;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

public final class AwsCloudWatchUtil {
  private AwsCloudWatchUtil() {
    // no-op
  }

  public static MetricDatum metricDatumFromEntry(final DatumBufferEntry entry, final Collection<Dimension> defaultDimensions) {
    final MetricCollector collector = MetricCollectorRegistry.INSTANCE.get(entry.metricId());
    final MetricDatum.Builder builder = MetricDatum.builder();
    builder.metricName(collector.name());
    builder.timestamp(entry.instant());
    setMetricDatumValue(builder, collector.unit(), entry.value());
    setMetricDimensions(builder, collector, defaultDimensions);
    return builder.build();
  }

  private static void setMetricDatumValue(final MetricDatum.Builder builder, final DatumUnit unit, final long value) {
    switch (unit) {
      case BITS -> setMetricDatumValue(builder, StandardUnit.BITS, value);
      case BYTES -> setMetricDatumValue(builder, StandardUnit.BYTES, value);
      case COUNT -> setMetricDatumValue(builder, StandardUnit.COUNT, value);
      case PERCENT -> setMetricDatumValue(builder, StandardUnit.PERCENT, value);
      case MICROSECONDS -> setMetricDatumValue(builder, StandardUnit.MICROSECONDS, value);
      case MILLISECONDS -> setMetricDatumValue(builder, StandardUnit.MILLISECONDS, value);
      case NANOSECONDS -> setMetricDatumValue(builder, StandardUnit.MICROSECONDS, TimeUnit.NANOSECONDS.toMicros(value));
      case SECONDS -> setMetricDatumValue(builder, StandardUnit.SECONDS, value);
    }
  }

  private static void setMetricDatumValue(final MetricDatum.Builder builder, final StandardUnit unit, final double value) {
    builder.unit(unit);
    builder.value(value);
  }

  private static void setMetricDimensions(final MetricDatum.Builder builder, final MetricCollector collector, final Collection<Dimension> defaultDimensions) {
    if (collector.hasDimensions()) {
      final String[] keys = collector.dimensionKeys();
      final String[] vals = collector.dimensionValues();
      final ArrayList<Dimension> dimensions = new ArrayList<>(keys.length + defaultDimensions.size());
      dimensions.addAll(defaultDimensions);
      for (int i = 0; i < keys.length; ++i) {
        dimensions.add(Dimension.builder().name(keys[i]).value(vals[i]).build());
      }
      builder.dimensions(dimensions);
    } else {
      builder.dimensions(defaultDimensions);
    }
  }
}
