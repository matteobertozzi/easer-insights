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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Supplier;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector.MetricSnapshot;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector.MetricDatumUpdateType;

public final class MetricsRegistry {
  public static final MetricsRegistry INSTANCE = new MetricsRegistry();

  private static final int COLLECTORS_PAGE_SIZE = 512;

  private final ConcurrentHashMap<MetricKey, MetricCollector> collectorsNames = new ConcurrentHashMap<>(1024);
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
  private MetricCollector[][] collectorsPages;
  private MetricCollector[] lastPage;
  private int lastPageOffset;

  private MetricsRegistry() {
    this.collectorsPages = new MetricCollector[0][];
    this.lastPage = null;
    this.lastPageOffset = COLLECTORS_PAGE_SIZE;
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
    final int metricOffset = metricId - 1;
    final int pageId = metricOffset / COLLECTORS_PAGE_SIZE;
    final int pageOffset = metricOffset & (COLLECTORS_PAGE_SIZE - 1);

    final ReadLock lock = rwLock.readLock();
    lock.lock();
    try {
      return collectorsPages[pageId][pageOffset];
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
  @SuppressWarnings("unchecked")
  <T extends MetricDatumCollector> T register(final String name, final DatumUnit unit,
      final String label, final String help, final T datumCollector) {
    final MetricDefinition definition = MetricDefinitionUtil.newMetricDefinition(name, unit, label, help);
    final WriteLock wlock = rwLock.writeLock();
    wlock.lock();
    try {
      // check if we have already registered the key
      final MetricCollector counter = get(definition);
      if (counter != null) {
        throw new IllegalStateException("metric name already registered: " + counter.definition());
      }

      // generate a new collector
      return (T) register(definition, datumCollector);
    } finally {
      wlock.unlock();
    }
  }

  @SuppressWarnings("unchecked")
  <T extends MetricDatumCollector> T register(final String name, final String[] dimensionKeys, final String[] dimensionValues,
      final DatumUnit unit, final String label, final String help,
      final Supplier<T> collectorSupplier) {
    final MetricDefinition definition = MetricDefinitionUtil.newMetricDefinition(name, dimensionKeys, dimensionValues, unit, label, help);

    final WriteLock wlock = rwLock.writeLock();
    wlock.lock();
    try {
      // check if we have already registered the key
      final MetricCollector collector = get(definition);
      if (collector != null) return (T) collector;

      // generate a new collector
      return (T) register(definition, collectorSupplier.get());
    } finally {
      wlock.unlock();
    }
  }

  private MetricCollector register(final MetricDefinition definition, final MetricDatumCollector datumCollector) {
    final int metricId = ensureMetricIdSlotAvailable();
    final MetricCollector collector = datumCollector.newCollector(definition, metricId);
    lastPage[lastPageOffset++] = collector;
    collectorsNames.put(definition, collector);
    return collector;
  }

  // ================================================================================================
  private int ensureMetricIdSlotAvailable() {
    if (lastPageOffset == COLLECTORS_PAGE_SIZE) {
      collectorsPages = Arrays.copyOf(collectorsPages, collectorsPages.length + 1);
      lastPage = new MetricCollector[COLLECTORS_PAGE_SIZE];
      lastPageOffset = 0;
      collectorsPages[collectorsPages.length - 1] = lastPage;
    }
    return 1 + (((collectorsPages.length - 1) * COLLECTORS_PAGE_SIZE) + lastPageOffset);
  }

  // ================================================================================================
  //  HumanReport/Snapshot related
  // ================================================================================================
  public String humanReport() {
    final StringBuilder sb = new StringBuilder(1 << 20);

    JvmMetrics.INSTANCE.addToHumanReport(sb);

    for (final MetricCollector[] page: collectorsPages) {
      for (final MetricCollector metricCollector: page) {
        if (metricCollector == null) break;

        final MetricSnapshot snapshot = metricCollector.snapshot();
        sb.append("\n--- ").append(snapshot.label()).append(" (").append(snapshot.name()).append(") ---\n");
        snapshot.data().addToHumanReport(metricCollector.definition(), sb);
      }
    }
    return sb.toString();
  }

  public List<MetricSnapshot> snapshot() {
    final ArrayList<MetricSnapshot> snapshot = new ArrayList<>(collectorsPages.length * COLLECTORS_PAGE_SIZE);
    for (final MetricCollector[] page : collectorsPages) {
      for (final MetricCollector metricCollector : page) {
        if (metricCollector == null) break;
        snapshot.add(metricCollector.snapshot());
      }
    }
    return snapshot;
  }
}
