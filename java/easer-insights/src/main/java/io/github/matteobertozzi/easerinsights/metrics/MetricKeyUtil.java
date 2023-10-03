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

import io.github.matteobertozzi.easerinsights.util.ImmutableCollections;

public final class MetricKeyUtil {
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private MetricKeyUtil() {
    // no-op
  }

  public static MetricKey newMetricKey(final String name) {
    return new MetricKeyImpl(name, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY);
  }

  public static MetricKey newMetricKey(final String name, final String[] dimensionKeys, final String[] dimensionVals) {
    return new MetricKeyImpl(name, dimensionKeys, dimensionVals);
  }

  public static int hashCode(final String name, final String[] dimensionKeys, final String[] dimensionVals) {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(dimensionKeys);
    result = prime * result + Arrays.hashCode(dimensionVals);
    result = prime * result + Objects.hash(name);
    return result;
  }

  public static boolean keyEquals(final MetricKey a, final MetricKey b) {
    return a.name().equals(b.name())
        && Arrays.equals(a.dimensionKeys(), b.dimensionKeys())
        && Arrays.equals(a.dimensionValues(), b.dimensionValues());
  }

  // ==============================================================================================================
  private static class MetricKeyImpl implements MetricKey, Comparable<MetricKey> {
    private final String name;
    private final String[] dimensionKeys;
    private final String[] dimensionVals;
    private final int hashCode;

    private MetricKeyImpl(final String name, final String[] dimensionKeys, final String[] dimensionVals) {
      this.name = name;
      this.dimensionKeys = dimensionKeys;
      this.dimensionVals = dimensionVals;
      this.hashCode = MetricKeyUtil.hashCode(name, dimensionKeys, dimensionVals);
    }

    @Override public String name() { return name; }
    @Override public String[] dimensionKeys() { return dimensionKeys; }
    @Override public String[] dimensionValues() { return dimensionVals; }
    @Override public boolean hasDimensions() { return dimensionKeys != null && dimensionKeys.length > 0; }

    @Override
    public Map<String, String> dimensions() {
      return ImmutableCollections.mapOf(dimensionKeys(), dimensionValues());
    }

    @Override
    public int compareTo(final MetricKey other) {
      if (this == other) return 0;

      int cmp = name.compareTo(other.name());
      if (cmp != 0) return cmp;

      cmp = Arrays.compare(dimensionKeys, other.dimensionKeys());
      if (cmp != 0) return cmp;

      return Arrays.compare(dimensionVals, other.dimensionValues());
    }

    @Override
    public int hashCode() {
      return this.hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof final MetricKey other)) return false;

      return MetricKeyUtil.keyEquals(this, other);
    }
  }
}
