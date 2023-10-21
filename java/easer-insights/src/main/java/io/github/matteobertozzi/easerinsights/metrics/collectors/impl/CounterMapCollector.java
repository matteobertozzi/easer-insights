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

package io.github.matteobertozzi.easerinsights.metrics.collectors.impl;

import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.easerinsights.metrics.collectors.CounterMap;

public class CounterMapCollector implements MetricCollector, CounterMap {
  private final MetricDefinition definition;
  private final CounterMap collector;
  private final int metricId;

  public CounterMapCollector(final MetricDefinition definition, final CounterMap collector, final int metricId) {
    this.definition = definition;
    this.collector = collector;
    this.metricId = metricId;
  }

  @Override
  public void add(final String key, final long timestamp, final long delta) {
    collector.add(key, timestamp, delta);

    MetricsRegistry.INSTANCE.notifyDatumUpdate(this, key, MetricDatumUpdateType.DELTA, timestamp, delta);
  }

  @Override
  public void set(final String key, final long timestamp, final long value) {
    collector.set(key, timestamp, value);

    MetricsRegistry.INSTANCE.notifyDatumUpdate(this, key, MetricDatumUpdateType.SAMPLE, timestamp, value);
  }

  @Override
  public CounterMapSnapshot dataSnapshot() {
    return collector.dataSnapshot();
  }

  @Override
  public int metricId() {
    return metricId;
  }

  @Override
  public String type() {
    return "COUNTER_MAP";
  }

  @Override
  public MetricDefinition definition() {
    return definition;
  }

  @Override
  public MetricSnapshot snapshot() {
    return new MetricSnapshot(definition(), type(), dataSnapshot());
  }
}
