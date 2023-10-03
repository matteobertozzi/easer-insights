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

public class MetricDimension<T extends MetricDatumCollector> {
  private final Supplier<T> collectorSupplier;
  private final String[] dimensionKeys;
  private final DatumUnit unit;
  private final String name;
  private final String label;
  private final String help;

  public MetricDimension(final String name, final String[] dimensionKeys, final DatumUnit unit,
      final String label, final String help, final Supplier<T> collectorSupplier) {
    this.collectorSupplier = collectorSupplier;
    this.dimensionKeys = dimensionKeys;
    this.unit = unit;
    this.name = name;
    this.label = label;
    this.help = help;
  }

  public T get(final String... dimensions) {
    if (dimensions == null || dimensions.length != dimensionKeys.length) {
      throw new IllegalArgumentException("expected " + dimensionKeys.length + " dimensions: " + Arrays.toString(dimensionKeys));
    }

    // most of the times we should already have the collector...
    final T collector = (T) MetricsRegistry.INSTANCE.get(name, dimensionKeys, dimensions);
    if (collector != null) return collector;

    // register a new collector for the new dimensions...
    return MetricsRegistry.INSTANCE.register(name, dimensionKeys, dimensions, unit, label, help, collectorSupplier);
  }
}
