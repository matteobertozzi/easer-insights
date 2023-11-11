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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector.MetricSnapshot;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector.MetricDatumUpdateType;
import io.github.matteobertozzi.rednaco.collections.arrays.paged.PagedArray;

public final class MetricsRegistry {
  public static final MetricsRegistry INSTANCE = new MetricsRegistry();

  private final ConcurrentHashMap<MetricKey, MetricCollector> collectorsNames = new ConcurrentHashMap<>(1024);
  private final PagedArray<MetricCollector> collectorsPages = new PagedArray<>(MetricCollector.class, 512);
  private final ReentrantLock lock = new ReentrantLock(true);

  private MetricsRegistry() {
    // no-op
  }

  public MetricCollector get(final String name) {
    return get(MetricKeyUtil.newMetricKey(name));
  }

  public MetricCollector get(final String name, final String[] dimensionKeys, final String[] dimensions) {
    return get(MetricKeyUtil.newMetricKey(name, dimensionKeys, dimensions));
  }

  public MetricCollector get(final MetricKey key) {
    return collectorsNames.get(key);
  }

  public MetricCollector get(final int metricId) {
    lock.lock();
    try {
      return collectorsPages.get(metricId - 1);
    } finally {
      lock.unlock();
    }
  }

  // ==============================================================================================================
  //  Metric Datum Update related
  // ==============================================================================================================
  @FunctionalInterface
  public interface MetricDatumUpdateNotifier {
    void notifyDatumUpdate(MetricCollector collector, long timestamp, long value);
  }

  private final CopyOnWriteArrayList<MetricDatumUpdateNotifier> datumUpdateNotifiers = new CopyOnWriteArrayList<>();
  public void registerMetricDatumUpdateNotifier(final MetricDatumUpdateNotifier notifier) {
    datumUpdateNotifiers.add(notifier);
  }

  public void notifyDatumUpdate(final MetricCollector collector, final MetricDatumUpdateType updateType, final long timestamp, final long value) {
    if (datumUpdateNotifiers.isEmpty()) return;

    for (final MetricDatumUpdateNotifier notifier: datumUpdateNotifiers) {
      notifier.notifyDatumUpdate(collector, timestamp, value);
    }
  }

  public void notifyDatumUpdate(final MetricCollector collector, final String key,
      final MetricDatumUpdateType updateType, final long timestamp, final long value) {
    // TODO
  }

  // ================================================================================================
  <T extends MetricDatumCollector> T register(final String name, final DatumUnit unit,
      final String label, final String help, final T datumCollector) {
    final MetricDefinition definition = MetricDefinitionUtil.newMetricDefinition(name, unit, label, help);
    return register(definition, datumCollector);
  }

  <T extends MetricDatumCollector> T register(final String name, final String[] dimensionKeys, final String[] dimensionValues,
      final DatumUnit unit, final String label, final String help,
      final T datumCollector) {
    final MetricDefinition definition = MetricDefinitionUtil.newMetricDefinition(name, dimensionKeys, dimensionValues, unit, label, help);
    return register(definition, datumCollector);
  }

  @SuppressWarnings("unchecked")
  <T extends MetricDatumCollector> T register(final String name, final String[] dimensionKeys, final String[] dimensionValues,
      final DatumUnit unit, final String label, final String help,
      final Supplier<T> collectorSupplier) {
    final MetricDefinition definition = MetricDefinitionUtil.newMetricDefinition(name, dimensionKeys, dimensionValues, unit, label, help);

    lock.lock();
    try {
      // check if we have already registered the key
      final MetricCollector collector = get(definition);
      if (collector != null) return (T) collector;

      // generate a new collector
      return (T) addCollector(definition, collectorSupplier.get());
    } finally {
      lock.unlock();
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends MetricDatumCollector> T register(final MetricDefinition definition, final T datumCollector) {
    lock.lock();
    try {
      // check if we have already registered the key
      final MetricCollector counter = get(definition);
      if (counter != null) {
        throw new IllegalStateException("metric name already registered: " + counter.definition());
      }

      // generate a new collector
      return (T) addCollector(definition, datumCollector);
    } finally {
      lock.unlock();
    }
  }

  private MetricCollector addCollector(final MetricDefinition definition, final MetricDatumCollector datumCollector) {
    final int metricId = collectorsPages.size();
    final MetricCollector collector = datumCollector.newCollector(definition, metricId);
    collectorsPages.add(collector);
    collectorsNames.put(definition, collector);
    return collector;
  }

  // ================================================================================================
  //  HumanReport/Snapshot related
  // ================================================================================================
  public String humanReport() {
    lock.lock();
    try {
      final StringBuilder sb = new StringBuilder(1 << 20);

      JvmMetrics.INSTANCE.addToHumanReport(sb);

      collectorsPages.forEach((page, pageOff, pageLen) -> {
        for (int i = 0; i < pageLen; ++i) {
          final MetricCollector metricCollector = page[pageOff + i];
          final MetricSnapshot snapshot = metricCollector.snapshot();
          sb.append("\n--- ").append(snapshot.label()).append(" (").append(snapshot.name()).append(") ---\n");
          snapshot.data().addToHumanReport(metricCollector.definition(), sb);
        }
      });
      return sb.toString();
    } finally {
      lock.unlock();
    }
  }

  public List<MetricSnapshot> snapshot() {
    lock.lock();
    try {
      final ArrayList<MetricSnapshot> snapshot = new ArrayList<>(collectorsPages.size());
      collectorsPages.forEach((page, pageOff, pageLen) -> {
        for (int i = 0; i < pageLen; ++i) {
          snapshot.add(page[pageOff + i].snapshot());
        }
      });
      return snapshot;
    } finally {
      lock.unlock();
    }
  }
}
