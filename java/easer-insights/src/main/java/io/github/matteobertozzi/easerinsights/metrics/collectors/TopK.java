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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.TopKCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.TopKImplMt;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.TopKImplSt;
import io.github.matteobertozzi.easerinsights.util.DatumUnitConverter;
import io.github.matteobertozzi.easerinsights.util.HumansTableView;

public interface TopK extends MetricDatumCollector, CollectorKeyGauge {
  static TopK newSingleThreaded(final int k, final long maxInterval, final long window, final TimeUnit unit) {
    return new TopKImplSt(k, maxInterval, window, unit);
  }

  static TopK newMultiThreaded(final int k, final long maxInterval, final long window, final TimeUnit unit) {
    return new TopKImplMt(k, maxInterval, window, unit);
  }

  default MetricCollector newCollector(final MetricDefinition definition, final int metricId) {
    return new TopKCollector(definition, this, metricId);
  }

  // ====================================================================================================
  //  Snapshot related
  // ====================================================================================================
  public record TopEntrySnapshot (String key, long maxTimestamp, long maxValue, long minValue, long sum, long sumSquares, long count) {
    public double average() {
      if (count == 0) return 0;
      return (double) sum / count;
    }

    public double variance() {
      if (count == 0) return 0;

      final double dNumEvents = count;
      final double dSum = sum;
      return ((double) sumSquares * dNumEvents - dSum * dSum) / (dNumEvents * dNumEvents);
    }

    public double standardDeviation() {
      return count != 0 ? Math.sqrt(Math.max(variance(), 0.0)) : 0;
    }
  }

  public record TopKSnapshot (TopEntrySnapshot[] entries) implements MetricDataSnapshot {
    public static final TopKSnapshot EMPTY_SNAPSHOT = new TopKSnapshot(new TopEntrySnapshot[0]);

    private static final List<String> HEADER = List.of("", "Max Timestamp", "Max", "Min", "Avg", "StdDev", "Freq");

    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      if (entries.length == 0) return report.append("(no data)\n");

      final DatumUnitConverter unitConverter = DatumUnitConverter.humanConverter(metricDefinition.unit());

      final HumansTableView table = new HumansTableView();
      table.addColumns(HEADER);
      for (int i = 0; i < entries.length; ++i) {
        final TopEntrySnapshot entry = entries[i];

        table.addRow(List.of(
          entry.key(), DatumUnitConverter.humanDateFromEpochMillis(entry.maxTimestamp()),
          unitConverter.asHumanString(entry.maxValue()), unitConverter.asHumanString(entry.minValue()),
          unitConverter.asHumanString(Math.round(entry.average())),
          unitConverter.asHumanString(Math.round(entry.standardDeviation())),
          DatumUnitConverter.humanCount(entry.count())
        ));
      }
      return table.addHumanView(report);
    }
  }
}
