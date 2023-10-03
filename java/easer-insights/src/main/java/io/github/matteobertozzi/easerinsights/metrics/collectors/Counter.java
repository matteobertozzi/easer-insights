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

package io.github.matteobertozzi.easerinsights.metrics.collectors;

import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.CounterCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.CounterImplMt;
import io.github.matteobertozzi.easerinsights.util.DatumUnitConverter;

public interface Counter extends CollectorCounter, MetricDatumCollector {
  public static Counter newSingleThreaded() {
    return new CounterImplMt();
  }

  public static Counter newMultiThreaded() {
    return new CounterImplMt();
  }

  default MetricCollector newCollector(final MetricDefinition definition, final int metricId) {
    return new CounterCollector(definition, this, metricId);
  }

  // ====================================================================================================
  //  Snapshot related
  // ====================================================================================================
  public record CounterSnapshot (long lastUpdate, long value) implements MetricDataSnapshot {
    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      final DatumUnitConverter converter = DatumUnitConverter.humanConverter(metricDefinition.unit());
      report.append(converter.asHumanString(value)).append(" lastUpdated: ").append(DatumUnitConverter.humanDateFromEpochMillis(lastUpdate));
      report.append("\n");
      return report;
    }
  }
}