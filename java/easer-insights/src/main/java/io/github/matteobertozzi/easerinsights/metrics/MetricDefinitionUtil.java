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
import java.util.Map;
import java.util.Objects;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.util.ImmutableCollections;

public final class MetricDefinitionUtil {
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private MetricDefinitionUtil() {
    // no-op
  }

  public static MetricDefinition newMetricDefinition(final String name, final DatumUnit unit, final String label, final String help) {
    return new SimpleMetricDefinition(name, unit, label, help);
  }

  public static MetricDefinition newMetricDefinition(final String name, final String[] dimensionKeys, final String[] dimensionVals,
      final DatumUnit unit, final String label, final String help) {
    return new DimensionsMetricDefinition(name,
      dimensionKeys, dimensionVals, ImmutableCollections.mapOf(dimensionKeys, dimensionVals),
      unit, newLabelWithDimensions(label, dimensionKeys, dimensionVals), help);
  }

  // ==============================================================================================================
  private static String newLabelWithDimensions(final String label, final String[] dimensionKeys, final String[] dimensionVals) {
    final StringBuilder fullLabel = new StringBuilder(label);
    fullLabel.append(" - [");
    for (int i = 0; i < dimensionKeys.length; ++i) {
      if (i > 0) fullLabel.append(", ");
      fullLabel.append(dimensionKeys[i]).append(":").append(dimensionVals[i]);
    }
    fullLabel.append("]");
    return fullLabel.toString();
  }

  // ==============================================================================================================
  private static class SimpleMetricDefinition implements MetricDefinition {
    private final DatumUnit unit;
    private final String name;
    private final String label;
    private final String help;

    private SimpleMetricDefinition(final String name, final DatumUnit unit, final String label, final String help) {
      this.unit = unit;
      this.name = name;
      this.label = label;
      this.help = help;
    }

    @Override public DatumUnit unit() { return unit; }
    @Override public String name() { return name; }
    @Override public String label() { return label;}
    @Override public String help() { return help;}

    @Override public boolean hasDimensions() { return false; }
    @Override public String[] dimensionKeys() { return EMPTY_STRING_ARRAY;}
    @Override public String[] dimensionValues() { return EMPTY_STRING_ARRAY; }
    @Override public Map<String, String> dimensions() { return Map.of(); }

    @Override
    public int hashCode() {
      return MetricKeyUtil.hashCode(name(), dimensionKeys(), dimensionValues());
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj instanceof final MetricDefinition otherDef) {
        return unit().equals(otherDef.unit())
          && name().equals(otherDef.name())
          && Arrays.equals(dimensionKeys(), otherDef.dimensionKeys())
          && Arrays.equals(dimensionValues(), otherDef.dimensionValues())
          && Objects.equals(label(), otherDef.label())
          && Objects.equals(help(), otherDef.help());
      } else if (obj instanceof final MetricKey otherKey) {
        return MetricKeyUtil.keyEquals(this, otherKey);
      }
      return false;
    }
  }

  private static class DimensionsMetricDefinition extends SimpleMetricDefinition {
    private final Map<String, String> dimensions;
    private final String[] dimensionKeys;
    private final String[] dimensionValues;

    private DimensionsMetricDefinition(final String name, final String[] dimensionKeys, final String[] dimensionValues, final Map<String, String> dimensions,
       final DatumUnit unit, final String label, final String help) {
      super(name, unit, label, help);
      this.dimensionKeys = dimensionKeys;
      this.dimensionValues = dimensionValues;
      this.dimensions = ImmutableCollections.mapOf(dimensionKeys, dimensionValues);
    }

    @Override public boolean hasDimensions() { return true; }
    @Override public String[] dimensionKeys() { return dimensionKeys;}
    @Override public String[] dimensionValues() { return dimensionValues; }
    @Override public Map<String, String> dimensions() { return dimensions; }
  }
}
