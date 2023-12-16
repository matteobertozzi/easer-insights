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

import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK;

public class TopKCollector implements MetricCollector, TopK {
  private final MetricDefinition definition;
  private final TopK collector;
  private final int metricId;

  public TopKCollector(final MetricDefinition definition, final TopK collector, final int metricId) {
    this.definition = definition;
    this.collector = collector;
    this.metricId = metricId;
  }

  public static TopK newSingleThreaded(final int k, final long maxInterval, final long window, final TimeUnit unit) {
    return new TopKImplSt(k, maxInterval, window, unit);
  }

  public static TopK newMultiThreaded(final int k, final long maxInterval, final long window, final TimeUnit unit) {
    return new TopKImplMt(k, maxInterval, window, unit);
  }

  @Override
  public void sample(final String key, final long timestamp, final long value) {
    collector.sample(key, timestamp, value);

    MetricsRegistry.INSTANCE.notifyDatumUpdate(this, key, MetricDatumUpdateType.SAMPLE, timestamp, value);
  }

  @Override
  public TopKSnapshot dataSnapshot() {
    return collector.dataSnapshot();
  }

  @Override
  public int metricId() {
    return metricId;
  }

  @Override
  public String type() {
    return "TOPK";
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
