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

package io.github.matteobertozzi.easerinsights.metrics;

import java.util.Map;
import java.util.function.BiConsumer;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector.MetricDataSnapshot;
import io.github.matteobertozzi.easerinsights.util.TimeUtil;

public interface MetricCollector extends MetricDefinition {
  int metricId();
  String type();

  boolean hasDimensions();
  String[] dimensionKeys();
  String[] dimensionValues();
  void forEachDimension(BiConsumer<String, String> consumer);

  MetricSnapshot snapshot();

  void update(long timestamp, long value);

  default void update(final long value) {
    update(TimeUtil.currentEpochMillis(), value);
  }

  final class Builder extends AbstractMetricBuilder<Builder> {
    public <T extends MetricDatumCollector> MetricCollector register(final T collector) {
      return MetricCollectorRegistry.INSTANCE.register(name(), unit(), label(), help(), collector);
    }
  }

  record MetricSnapshot (String name, String type, DatumUnit unit, String label, String help, Map<String, String> dimensions, MetricDataSnapshot data) {}
}
