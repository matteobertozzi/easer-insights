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

import java.util.Formatter;

import io.github.matteobertozzi.easerinsights.DatumUnit.DatumUnitConverter;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.CounterMapCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.CounterMapImplMt;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.CounterMapImplSt;
import io.github.matteobertozzi.rednaco.collections.arrays.ArraySortUtil;
import io.github.matteobertozzi.rednaco.collections.arrays.ArrayUtil;

public interface CounterMap extends CollectorKeyCounter, MetricDatumCollector {
  static CounterMap newSingleThreaded() {
    return new CounterMapImplSt();
  }

  static CounterMap newMultiThreaded() {
    return new CounterMapImplMt();
  }

  @Override
  default MetricCollector newCollector(final MetricDefinition definition, final int metricId) {
    return new CounterMapCollector(definition, this, metricId);
  }

  // ====================================================================================================
  //  Snapshot related
  // ====================================================================================================
  @Override CounterMapSnapshot dataSnapshot();

  record CounterMapSnapshot (String[] keys, long[] values) implements MetricDataSnapshot {
    public static final CounterMapSnapshot EMPTY_SNAPSHOT = new CounterMapSnapshot(new String[0], new long[0]);

    public static CounterMapSnapshot ofUnsorted(final String[] keys, final long[] values) {
      ArraySortUtil.sort(0, keys.length, (aIndex, bIndex) -> Long.compare(values[bIndex], values[aIndex]), (aIndex, bIndex) -> {
        ArrayUtil.swap(keys, aIndex, bIndex);
        ArrayUtil.swap(values, aIndex, bIndex);
      });
      return new CounterMapSnapshot(keys, values);
    }

    public long totalValue() {
      long total = 0;
      for (int i = 0; i < values.length; ++i) {
        total += values[i];
      }
      return total;
    }

    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      final long total = totalValue();
      if (total <= 0) return report.append("(no data)\n");

      final DatumUnitConverter converter = metricDefinition.unit().humanConverter();
      try (Formatter formatter = new Formatter(report)) {
        for (int i = 0; i < keys.length; ++i) {
          final long value = values[i];
          report.append(" - ");
          formatter.format("%5.2f%%", 100.0 * (value / (double) total));
          report.append(" (");
          formatter.format("%7s", converter.asHumanString(value));
          report.append(") - ");
          report.append(keys[i]);
          report.append('\n');
        }
      }
      return report;
    }
  }
}
