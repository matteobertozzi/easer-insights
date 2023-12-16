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

import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit.DatumUnitConverter;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.HeatmapCollector;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;

public interface Heatmap extends CollectorGauge, MetricDatumCollector {
  static Heatmap newSingleThreaded(final long maxInterval, final long window, final TimeUnit unit, final long[] bounds) {
    return HeatmapCollector.newSingleThreaded(maxInterval, window, unit, bounds);
  }

  static Heatmap newMultiThreaded(final long maxInterval, final long window, final TimeUnit unit, final long[] bounds) {
    return HeatmapCollector.newMultiThreaded(maxInterval, window, unit, bounds);
  }

  @Override
  default MetricCollector newCollector(final MetricDefinition definition, final int metricId) {
    return new HeatmapCollector(definition, this, metricId);
  }

  // ====================================================================================================
  //  Snapshot related
  // ====================================================================================================
  @Override HeatmapSnapshot dataSnapshot();

  record HeatmapSnapshot(long lastInterval, long window, long[] bounds, long[] events,
      long[] minValue, long[] maxValue, long[] sum, long[] sumSquares) implements MetricDataSnapshot {
    public static final HeatmapSnapshot EMPTY_SNAPSHOT = new HeatmapSnapshot(0, 0, new long[0], new long[0], null, null, null, null);

    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      final DatumUnitConverter unitConverter = metricDefinition.unit().humanConverter();

      long maxEvents = 0;
      for (int i = 0; i < events.length; ++i) {
        maxEvents = Math.max(maxEvents, events[i]);
      }

      final long e25 = Math.round(maxEvents * 0.25);
      final long e50 = Math.round(maxEvents * 0.50);
      final long e75 = Math.round(maxEvents * 0.75);
      final long e90 = Math.round(maxEvents * 0.90);
      final long e99 = Math.round(maxEvents * 0.99);

      final int numIntervals = events.length / bounds.length;
      for (int b = bounds.length - 1; b >= 0; --b) {
        report.append(String.format("%6s |", unitConverter.asHumanString(bounds[b])));
        for (int i = 0; i < numIntervals; ++i) {
          final long numEvents = events[(i * bounds.length) + b];
          if (numEvents > e25) {
            final int perc = (int) Math.round(100 * ((double)numEvents / maxEvents));
            report.append(" ").append(perc < 100 ? perc : "**").append(" ");
          } else {
            report.append(" -- ");
          }
        }
        report.append("\n");
      }
      report.append("       +");
      report.append('-' * numIntervals * 4);
      report.append("\n");
      report.append("        25%:").append(HumansUtil.humanCount(e25));
      report.append(", 50%:").append(HumansUtil.humanCount(e50));
      report.append(", 75%:").append(HumansUtil.humanCount(e75));
      report.append(", 90%:").append(HumansUtil.humanCount(e90));
      report.append(", 99%:").append(HumansUtil.humanCount(e99));
      report.append(", max:").append(HumansUtil.humanCount(maxEvents));
      report.append("\n");
      return report;
    }
  }
}
