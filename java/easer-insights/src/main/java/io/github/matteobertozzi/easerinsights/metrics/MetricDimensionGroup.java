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

import io.github.matteobertozzi.easerinsights.DatumUnit;

public class MetricDimensionGroup {
  private final String[] dimensionKeys;
  private final String[] dimensionVals;

  protected MetricDimensionGroup(final String[] dimensionKeys, final String... dimensionVals) {
    if (dimensionKeys == null || dimensionVals == null) {
      throw new IllegalArgumentException("expected dimension keys and values");
    }

    if (dimensionKeys.length != dimensionVals.length) {
      throw new IllegalArgumentException("mismatch between dimension keys and values - keys: " + Arrays.toString(dimensionKeys) + ", vals: " + Arrays.toString(dimensionVals));
    }

    this.dimensionKeys = dimensionKeys;
    this.dimensionVals = dimensionVals;
  }

  <T extends MetricDatumCollector> T register(final String name, final DatumUnit unit, final String label, final String help, final T collector) {
    return MetricsRegistry.INSTANCE.register(name, dimensionKeys, dimensionVals, unit, label, help, collector);
  }
}
