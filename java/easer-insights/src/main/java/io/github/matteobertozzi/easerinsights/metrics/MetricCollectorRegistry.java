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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jvm.JvmMetrics;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector.MetricSnapshot;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition.AbstractMetric;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition.MetricKey;
import io.github.matteobertozzi.easerinsights.util.ImmutableCollections;

public final class MetricCollectorRegistry {
  public static final MetricCollectorRegistry INSTANCE = new MetricCollectorRegistry();

  private static final int COLLECTORS_PAGE_SIZE = 512;

  @FunctionalInterface
  public interface MetricDatumUpdateNotifier {
    void notifyDatumUpdate(MetricCollector collector, long timestamp, long value);
  }

  private final ConcurrentHashMap<MetricKey, MetricCollector> collectorsNames = new ConcurrentHashMap<>();
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
  private MetricCollector[][] collectorsPages;
  private MetricCollector[] lastPage;
  private int lastPageOffset;

  private MetricCollectorRegistry() {
    this.collectorsPages = new MetricCollector[0][];
    this.lastPage = null;
    this.lastPageOffset = COLLECTORS_PAGE_SIZE;
  }

  MetricCollector get(final MetricKey key) {
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
  private final CopyOnWriteArrayList<MetricDatumUpdateNotifier> datumUpdateNotifiers = new CopyOnWriteArrayList<>();
  public void registerMetricDatumUpdateNotifier(final MetricDatumUpdateNotifier notifier) {
    datumUpdateNotifiers.add(notifier);
  }

  void datumUpdateNotifiers(final SimpleMetricCollector collector, final long timestamp, final long value) {
    if (datumUpdateNotifiers.isEmpty()) return;

    for (final MetricDatumUpdateNotifier notifier: datumUpdateNotifiers) {
      notifier.notifyDatumUpdate(collector, timestamp, value);
    }
  }

  // ==============================================================================================================
  //  Metric Collector Registration related
  // ==============================================================================================================
  MetricCollector register(final String name, final DatumUnit unit,
                           final String label, final String help, final MetricDatumCollector collector) {
    final WriteLock wlock = rwLock.writeLock();
    wlock.lock();
    try {
      final int metricId = ensureMetricIdSlotAvailable();
      final MetricCollector metricDef = new SimpleMetricCollector(metricId, name, unit, label, help, collector);
      lastPage[lastPageOffset++] = metricDef;
      collectorsNames.put(new MetricKey(name), metricDef);
      return metricDef;
    } finally {
      wlock.unlock();
    }
  }

  MetricCollector register(final MetricKey key, final DatumUnit unit,
                           final String label, final String help, final Supplier<MetricDatumCollector> collectorSupplier) {
    final WriteLock wlock = rwLock.writeLock();
    wlock.lock();
    try {
      MetricCollector collector = collectorsNames.get(key);
      if (collector != null) return collector;

      final String[] dimensionKeys = key.dimensionsKeys();
      final String[] dimensionVals = key.dimensionsValues();
      final StringBuilder fullLabel = new StringBuilder(label);
      fullLabel.append(" - [");
      for (int i = 0; i < dimensionKeys.length; ++i) {
        if (i > 0) fullLabel.append(", ");
        fullLabel.append(dimensionKeys[i]).append(":").append(dimensionVals[i]);
      }
      fullLabel.append("]");

      final int metricId = ensureMetricIdSlotAvailable();
      collector = new MetricCollectorWithDimensions(metricId, key.metricName(),
          key.dimensionsKeys(), key.dimensionsValues(), unit, fullLabel.toString(), help,
          collectorSupplier.get());
      lastPage[lastPageOffset++] = collector;
      collectorsNames.put(key, collector);
      return collector;
    } finally {
      wlock.unlock();
    }
  }

  private int ensureMetricIdSlotAvailable() {
    if (lastPageOffset == COLLECTORS_PAGE_SIZE) {
      collectorsPages = Arrays.copyOf(collectorsPages, collectorsPages.length + 1);
      lastPage = new MetricCollector[COLLECTORS_PAGE_SIZE];
      lastPageOffset = 0;
      collectorsPages[collectorsPages.length - 1] = lastPage;
    }
    return 1 + (((collectorsPages.length - 1) * COLLECTORS_PAGE_SIZE) + lastPageOffset);
  }

  public String humanReport() {
    final StringBuilder sb = new StringBuilder(1 << 20);

    JvmMetrics.INSTANCE.addToHumanReport(sb);

    for (final MetricCollector[] page: collectorsPages) {
      for (final MetricCollector metricCollector: page) {
        if (metricCollector == null) break;

        final MetricSnapshot snapshot = metricCollector.snapshot();
        sb.append("\n--- ").append(snapshot.label()).append(" ---\n");
        snapshot.data().addToHumanReport(metricCollector, sb);
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

  private static class SimpleMetricCollector extends AbstractMetric implements MetricCollector {
    private final MetricDatumCollector collector;
    private final int metricId;

    protected SimpleMetricCollector(final int metricId, final String name, final DatumUnit unit,
        final String label, final String help, final MetricDatumCollector collector) {
      super(name, unit, label, help);
      this.collector = collector;
      this.metricId = metricId;
    }

    public int metricId() {
      return metricId;
    }

    @Override
    public String type() {
      return collector.type();
    }

    @Override
    public void update(final long timestamp, final long value) {
      collector.update(timestamp, value);
      MetricCollectorRegistry.INSTANCE.datumUpdateNotifiers(this, timestamp, value);
    }

    @Override
    public MetricSnapshot snapshot() {
      final Map<String, String> dimensions = hasDimensions() ? ImmutableCollections.mapOf(dimensionKeys(), dimensionValues()) : null;
      return new MetricSnapshot(name(), type(), unit(), label(), help(), dimensions, collector.snapshot());
    }

    @Override
    public String toString() {
      return "MetricCollector [metricId:" + metricId()
        + ", name:" + name()
        + (hasDimensions() ? "dimensions: " + Arrays.toString(dimensionKeys()) + ", " + Arrays.toString(dimensionValues()) : "")
        + "]";
    }

    @Override public boolean hasDimensions() { return false; }
    @Override public String[] dimensionKeys() { return null; }
    @Override public String[] dimensionValues() { return null; }
    @Override public void forEachDimension(final BiConsumer<String, String> consumer) { /* no-op */ }
  }

  private static final class MetricCollectorWithDimensions extends SimpleMetricCollector {
    private final String[] dimensionKeys;
    private final String[] dimensionVals;

    private MetricCollectorWithDimensions(final int metricId, final String name,
        final String[] dimensionKeys, final String[] dimensionVals, final DatumUnit unit,
        final String label, final String help, final MetricDatumCollector collector) {
      super(metricId, name, unit, label, help, collector);
      this.dimensionKeys = dimensionKeys;
      this.dimensionVals = dimensionVals;
    }

    @Override public boolean hasDimensions() { return true; }
    @Override public String[] dimensionKeys() { return dimensionKeys; }
    @Override public String[] dimensionValues() { return dimensionVals; }

    @Override
    public void forEachDimension(final BiConsumer<String, String> consumer) {
      for (int i = 0; i < dimensionKeys.length; ++i) {
        consumer.accept(dimensionKeys[i], dimensionVals[i]);
      }
    }
  }
}
