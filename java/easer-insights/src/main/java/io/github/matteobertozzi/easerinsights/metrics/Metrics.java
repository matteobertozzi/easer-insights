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

import java.util.function.Supplier;

import io.github.matteobertozzi.easerinsights.DatumUnit;

public final class Metrics {
  private Metrics() {
    // no-op
  }

  public static MetricCollectorBuilder newCollector() {
    return new MetricCollectorBuilder();
  }

  public static MetricDimensionsBuilder newCollectorWithDimensions() {
    return new MetricDimensionsBuilder();
  }

  public static final class MetricCollectorBuilder extends AbstractMetricBuilder<MetricCollectorBuilder> {
    public <T extends MetricDatumCollector> T register(final T collector) {
      validate();
      return MetricsRegistry.INSTANCE.register(name(), unit(), label(), help(), collector);
    }
  }

  public static final class MetricDimensionsBuilder extends AbstractMetricBuilder<MetricDimensionsBuilder> {
    private String[] dimensions;

    public MetricDimensionsBuilder dimensions(final String... dimensions) {
      this.dimensions = dimensions;
      return this;
    }

    public <T extends MetricDatumCollector> MetricDimension<T> register(final Supplier<T> collectorSupplier) {
      validate();
      if (dimensions == null || dimensions.length == 0) {
        throw new IllegalArgumentException("dimensions must be specified for metric collector: " + name());
      }
      return new MetricDimension<>(name(), dimensions, unit(), label(), help(), collectorSupplier);
    }
  }

  private static abstract class AbstractMetricBuilder<TBuild extends AbstractMetricBuilder<TBuild>> {
    private DatumUnit unit;
    private String name;
    private String label;
    private String help;

    @SuppressWarnings("unchecked")
    public TBuild name(final String name) {
      this.name = name;
      return (TBuild) this;
    }

    @SuppressWarnings("unchecked")
    public TBuild label(final String label) {
      this.label = label;
      return (TBuild) this;
    }

    @SuppressWarnings("unchecked")
    public TBuild help(final String help) {
      this.help = help;
      return (TBuild) this;
    }

    @SuppressWarnings("unchecked")
    public TBuild unit(final DatumUnit unit) {
      this.unit = unit;
      return (TBuild) this;
    }

    protected String name() { return name; }
    protected String label() { return label; }
    protected String help() { return help; }
    protected DatumUnit unit() { return unit; }

    protected void validate() {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("name not specified for metric collector");
      }
      if (unit == null) {
        throw new IllegalArgumentException("unit not specified for metric collector: " + name);
      }
    }
  }
}
