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

import java.util.Objects;

import io.github.matteobertozzi.easerinsights.DatumUnit;

public interface MetricDefinition {
  DatumUnit unit();

  String name();
  String label();
  String help();

  abstract class AbstractMetric implements MetricDefinition {
    private final DatumUnit unit;
    private final String name;
    private final String label;
    private final String help;

    protected AbstractMetric(final String name, final DatumUnit unit, final String label, final String help) {
      this.name = name;
      this.unit = unit;
      this.label = label;
      this.help = help;
    }

    @Override public final DatumUnit unit() { return unit; }

    @Override public final String name() { return name; }

    @Override public final String label() { return label; }

    @Override public final String help() { return help; }
  }

  abstract class AbstractMetricBuilder<TBuild extends AbstractMetricBuilder<TBuild>> {
    private DatumUnit unit;
    private String name;
    private String label;
    private String help;

    @SuppressWarnings("unchecked")
    public TBuild unit(final DatumUnit unit) {
      this.unit = unit;
      return (TBuild) this;
    }

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

    protected DatumUnit unit() { return unit; }

    protected String name() { return name; }

    protected String label() { return label; }
    protected String help() { return help; }
  }

  class MetricKey {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private final String metricName;

    public MetricKey(final String metricName) {
      this.metricName = metricName;
    }

    public String metricName() {
      return metricName;
    }

    public String[] dimensionsKeys() {
      return EMPTY_STRING_ARRAY;
    }

    public String[] dimensionsValues() {
      return EMPTY_STRING_ARRAY;
    }

    @Override
    public int hashCode() {
      return metricName.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof final MetricKey other)) return false;
      return Objects.equals(metricName, other.metricName);
    }

    @Override
    public String toString() {
      return "MetricKey [metricName=" + metricName + "]";
    }
  }
}
