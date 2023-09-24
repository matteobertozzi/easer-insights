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

import java.util.Arrays;
import java.util.function.Supplier;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition.AbstractMetric;

public final class MetricDimensions extends AbstractMetric {
  private final Supplier<MetricDatumCollector> collectorSupplier;
  private final String[] dimensionKeys;

  private MetricDimensions(final String name, final String[] dimensionKeys, final DatumUnit unit,
      final String label, final String help, final Supplier<MetricDatumCollector> collectorSupplier) {
    super(name, unit, label, help);

    this.collectorSupplier = collectorSupplier;
    this.dimensionKeys = dimensionKeys;
  }

  public MetricCollector get(final String... dimensions) {
    final MetricKey key = new MetricKeyForKvDimensions(name(), dimensionKeys, dimensions);

    final MetricCollector collector = MetricCollectorRegistry.INSTANCE.get(key);
    if (collector != null) return collector;

    return MetricCollectorRegistry.INSTANCE.register(key, unit(), label(), help(), collectorSupplier);
  }

  public static final class Builder extends AbstractMetricBuilder<Builder> {
    private String[] dimensions;

    public Builder dimensions(final String... dimensions) {
      this.dimensions = dimensions;
      return this;
    }

    public MetricDimensions register(final Supplier<MetricDatumCollector> collector) {
      if (dimensions == null || dimensions.length == 0) {
        throw new IllegalArgumentException("expected dimensions, otherwise use: new MetricCollector.Builder()");
      }
      return new MetricDimensions(name(), dimensions, unit(), label(), help(), collector);
    }
  }

  private static final class MetricKeyForKvDimensions extends MetricKey {
    private final String[] dimensionKeys;
    private final String[] dimensionValues;
    private final int hashCode;

    private MetricKeyForKvDimensions(final String metricName, final String[] dimensionKeys, final String[] dimensionValues) {
      super(metricName);
      this.dimensionKeys = dimensionKeys;
      this.dimensionValues = dimensionValues;
      this.hashCode = computeHashCode();
    }

    @Override
    public String[] dimensionsKeys() {
      return dimensionKeys;
    }

    @Override
    public String[] dimensionsValues() {
      return dimensionValues;
    }

    private int computeHashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Arrays.hashCode(dimensionKeys);
      result = prime * result + Arrays.hashCode(dimensionValues);
      return result;
    }

    @Override
    public int hashCode() {
      return this.hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof final MetricKeyForKvDimensions other)) return false;
      return super.equals(obj)
          && Arrays.equals(dimensionKeys, other.dimensionKeys)
          && Arrays.equals(dimensionValues, other.dimensionValues);
    }

    @Override
    public String toString() {
      return "MetricKvDimensions [metricName=" + metricName()
        + ", dimensionKeys=" + Arrays.toString(dimensionKeys)
        + ", dimensionValues=" + Arrays.toString(dimensionValues) + "]";
    }
  }
}
