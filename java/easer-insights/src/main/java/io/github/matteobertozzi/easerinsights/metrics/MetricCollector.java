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

import java.util.Map;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector.MetricDataSnapshot;

public interface MetricCollector {
  int metricId();
  String type();

  MetricDefinition definition();

  MetricSnapshot snapshot();

  record MetricSnapshot (String name, Map<String, String> dimensions, String type, DatumUnit unit, String label, String help, MetricDataSnapshot data) {
    public MetricSnapshot(final MetricDefinition definition, final String type, final MetricDataSnapshot snapshot) {
      this(definition.name(), definition.hasDimensions() ? definition.dimensions() : null, type, definition.unit(), definition.label(), definition.help(), snapshot);
    }
  }
}
