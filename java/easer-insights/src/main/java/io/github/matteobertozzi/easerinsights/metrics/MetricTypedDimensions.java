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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition.AbstractMetric;

public final class MetricTypedDimensions<T extends Record> extends AbstractMetric {
  private final Supplier<MetricDatumCollector> collectorSupplier;
  private final RecordComponent[] components;
  private final String[] dimensionKeys;

  private MetricTypedDimensions(final String name, final Class<T> classOfDimensions, final DatumUnit unit,
      final String label, final String help, final Supplier<MetricDatumCollector> collectorSupplier) {
    super(name, unit, label, help);

    this.collectorSupplier = collectorSupplier;
    this.components = classOfDimensions.getRecordComponents();
    this.dimensionKeys = new String[components.length];
    for (int i = 0; i < components.length; ++i) {
      dimensionKeys[i] = components[i].getName();
    }
  }

  public MetricCollector get(final T dimensions) {
    final MetricKey key = new MetricKeyForTypedDimensions(name(), components, dimensionKeys, dimensions);

    final MetricCollector collector = MetricCollectorRegistry.INSTANCE.get(key);
    if (collector != null) return collector;

    return MetricCollectorRegistry.INSTANCE.register(key, unit(), label(), help(), collectorSupplier);
  }

  public static final class Builder<T extends Record> extends AbstractMetricBuilder<Builder<T>> {
    private Class<T> classOfDimensions;

    public Builder<T> dimensions(final Class<T> classOfDimensions) {
      this.classOfDimensions = classOfDimensions;
      return this;
    }

    public MetricTypedDimensions<T> register(final Supplier<MetricDatumCollector> collector) {
      if (classOfDimensions == null) {
        throw new IllegalArgumentException("expected dimensions, otherwise use: new MetricCollector.Builder()");
      }
      return new MetricTypedDimensions<>(name(), classOfDimensions, unit(), label(), help(), collector);
    }
  }

  private static final class MetricKeyForTypedDimensions extends MetricKey {
    private final RecordComponent[] components;
    private final String[] dimensionKeys;
    private final Record dimensions;

    public MetricKeyForTypedDimensions(final String metricName,
        final RecordComponent[] components, final String[] dimensionKeys,
        final Record dimensions) {
      super(metricName);
      this.dimensionKeys = dimensionKeys;
      this.components = components;
      this.dimensions = dimensions;
    }

    @Override
    public String[] dimensionsKeys() {
      return dimensionKeys;
    }

    @Override
    public String[] dimensionsValues() {
      try {
        final String[] values = new String[components.length];
        for (int i = 0; i < components.length; ++i) {
          values[i] = String.valueOf(components[i].getAccessor().invoke(dimensions));
        }
        return values;
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Objects.hash(dimensions);
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof final MetricKeyForTypedDimensions other)) return false;
      return super.equals(other)
          && Objects.equals(dimensions, other.dimensions);
    }

    @Override
    public String toString() {
      return "MetricRecordDimensions [metricName=" + metricName() + ", dimensions=" + dimensions + "]";
    }
  }
}
