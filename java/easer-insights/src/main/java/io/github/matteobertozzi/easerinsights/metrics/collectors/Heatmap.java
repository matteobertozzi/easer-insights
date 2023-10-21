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

import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.HeatmapCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.HeatmapImplMt;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.HeatmapImplSt;
import io.github.matteobertozzi.easerinsights.util.DatumUnitConverter;
import io.github.matteobertozzi.easerinsights.util.StatisticsUtil;

public interface Heatmap extends CollectorGauge, MetricDatumCollector {
  static Heatmap newSingleThreaded(final long maxInterval, final long window, final TimeUnit unit, final long[] bounds) {
    return new HeatmapImplSt(maxInterval, window, unit, bounds);
  }

  static Heatmap newMultiThreaded(final long maxInterval, final long window, final TimeUnit unit, final long[] bounds) {
    return new HeatmapImplMt(maxInterval, window, unit, bounds);
  }

  static Heatmap newCollector(final MetricDefinition definition, final Heatmap heatmap, final int metricId) {
    return new HeatmapCollector(definition, heatmap, metricId);
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
      final DatumUnitConverter unitConverter = DatumUnitConverter.humanConverter(metricDefinition.unit());

      final long lastBound = bounds[bounds.length - 1];
      final double mult = 10.0 / lastBound;
      final int numIntervals = events.length / bounds.length;
      final long firstIntervalTs = lastInterval - (numIntervals * window);
      for (int i = 0; i < numIntervals; ++i) {
        final int offset = i * bounds.length;
        final long intervalEvents = StatisticsUtil.sum(events, offset, bounds.length);
        if (intervalEvents == 0) continue;

        final double stdDev = StatisticsUtil.standardDeviation(intervalEvents, sum[i], sumSquares[i]);
        final double p50 = StatisticsUtil.percentile(50, bounds, minValue[i], maxValue[i], intervalEvents, events, offset);
        final double p75 = StatisticsUtil.percentile(75, bounds, minValue[i], maxValue[i], intervalEvents, events, offset);
        final double p99 = StatisticsUtil.percentile(99, bounds, minValue[i], maxValue[i], intervalEvents, events, offset);

        report.append(Instant.ofEpochMilli(firstIntervalTs + (i * window)).atZone(ZoneId.systemDefault()));
        report.append(" - p50:").append(String.format("%-5s", unitConverter.asHumanString(Math.round(p50))));
        report.append(" p75:").append(String.format("%-5s", unitConverter.asHumanString(Math.round(p75))));
        report.append(" p99:").append(String.format("%-5s", unitConverter.asHumanString(Math.round(p99))));
        report.append(" stdDev:").append(String.format("%-5s", unitConverter.asHumanString(Math.round(stdDev))));
        report.append(" |");

        // Add hash marks based on percentage
        final int marks = (int) Math.round(mult * p99);
        for (int m = 0; m < marks; ++m) report.append('#');
        for (int m = marks; m < 11; ++m) report.append(' ');

        drawBounds(report, unitConverter, i, offset);
        report.append('\n');
      }
      return report;
    }

    private void drawBounds(final StringBuilder report, final DatumUnitConverter unitConverter, final int intervalIndex, int eventsOffset) {
      report.append(unitConverter.asHumanString(minValue[intervalIndex])).append(":").append(DatumUnitConverter.humanCount(events[eventsOffset++]));
      final long lastBound = maxValue[intervalIndex];
      for (int b = 1; b < bounds.length && bounds[b] < lastBound; ++b) {
        report.append(", ");
        report.append(unitConverter.asHumanString(bounds[b])).append(":").append(DatumUnitConverter.humanCount(events[eventsOffset++]));
      }
      if (lastBound > minValue[intervalIndex]) {
        report.append(", ");
        report.append(unitConverter.asHumanString(lastBound)).append(":").append(DatumUnitConverter.humanCount(events[eventsOffset++]));
      }
    }
  }
}
