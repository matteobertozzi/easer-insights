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
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.HistogramCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.HistogramImplMt;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.HistogramImplSt;
import io.github.matteobertozzi.easerinsights.util.DatumUnitConverter;
import io.github.matteobertozzi.easerinsights.util.StatisticsUtil;

public interface Histogram extends CollectorGauge, MetricDatumCollector {
  static Histogram newSingleThreaded(final long[] bounds) {
    return new HistogramImplSt(bounds);
  }

  static Histogram newMultiThreaded(final long[] bounds) {
    return new HistogramImplMt(bounds);
  }

  static Histogram newCollector(final MetricDefinition definition, final Histogram histogram, final int metricId) {
    return new HistogramCollector(definition, histogram, metricId);
  }

  @Override
  default MetricCollector newCollector(final MetricDefinition definition, final int metricId) {
    return new HistogramCollector(definition, this, metricId);
  }

  // ====================================================================================================
  //  Snapshot related
  // ====================================================================================================
  @Override HistogramSnapshot dataSnapshot();

  record HistogramSnapshot(long[] bounds, long[] events, long numEvents, long minValue, long sum, long sumSquares) implements MetricDataSnapshot {
    public static final HistogramSnapshot EMPTY_SNAPSHOT = new HistogramSnapshot(new long[0], new long[0], 0, 0, 0, 0);

    public static HistogramSnapshot of(final long[] bounds, final long[] events,
        final long minValue, final long maxValue, final long sum, final long sumSquares) {
      long numEvents = 0;
      for (int i = 0; i < events.length; i++) {
        numEvents += events[i];
      }

      if (numEvents == 0) return HistogramSnapshot.EMPTY_SNAPSHOT;

      int firstBound = 0;
      int lastBound = events.length - 1;

      while (events[firstBound] == 0) firstBound++;
      while (events[lastBound] == 0) lastBound--;

      final int numBounds = 1 + (lastBound - firstBound);
      final long[] snapshotBounds = new long[numBounds];
      final long[] snapshotEvents = new long[numBounds];
      for (int i = firstBound; i < lastBound; ++i) {
        snapshotBounds[i - firstBound] = bounds[i];
        snapshotEvents[i - firstBound] = events[i];
      }
      snapshotBounds[numBounds - 1] = maxValue;
      snapshotEvents[numBounds - 1] = events[lastBound];

      return new HistogramSnapshot(snapshotBounds, snapshotEvents, numEvents, minValue, sum, sumSquares);
    }

    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      final DatumUnitConverter unitConverter = DatumUnitConverter.humanConverter(metricDefinition.unit());

      if (numEvents == 0) return report.append("(no data)\n");

      report.append("Count: ").append(DatumUnitConverter.humanCount(numEvents));
      report.append(" Average: ").append(unitConverter.asHumanString(Math.round(average())));
      report.append(" StdDev: ").append(unitConverter.asHumanString(Math.round(standardDeviation())));
      report.append('\n');
      report.append("Min: ").append(unitConverter.asHumanString(minValue()));
      report.append(" Median: ").append(unitConverter.asHumanString(Math.round(median())));
      report.append(" Max: ").append(unitConverter.asHumanString(maxValue()));
      report.append('\n');
      report.append("Percentiles: P50: ").append(unitConverter.asHumanString(Math.round(percentile(50))));
      report.append(" P75: ").append(unitConverter.asHumanString(Math.round(percentile(75))));
      report.append(" P99: ").append(unitConverter.asHumanString(Math.round(percentile(99))));
      report.append(" P99.9: ").append(unitConverter.asHumanString(Math.round(percentile(99.9f))));
      report.append(" P99.99: ").append(unitConverter.asHumanString(Math.round(percentile(99.99f))));
      report.append("\n--------------------------------------------------------------------------------\n");

      final double mult = 100.0 / numEvents();
      long cumulativeSum = 0;
      for (int b = 0; b < bounds.length; ++b) {
        final long bucketValue = events[b];
        if (bucketValue == 0) continue;

        cumulativeSum += bucketValue;

        final String lineFormat = (b < bounds.length - 1) ? "[%15s, %15s) %7s %7.3f%% %7.3f%% " : "[%15s, %15s] %7s %7.3f%% %7.3f%% ";
        report.append(String.format(lineFormat,
            unitConverter.asHumanString((b == 0) ? minValue : bounds[b - 1]),
            unitConverter.asHumanString(bounds[b]),
            DatumUnitConverter.humanCount(bucketValue),
            (mult * bucketValue),
            (mult * cumulativeSum)));

        // Add hash marks based on percentage
        final long marks = Math.round(mult * bucketValue / 5 + 0.5);
        for (int i = 0; i < marks; ++i) report.append('#');
        report.append('\n');
      }
      return report;
    }

    public long maxValue() { return bounds.length != 0 ? bounds[bounds.length - 1] : 0; }

    public double average() {
      return StatisticsUtil.average(numEvents(), sum());
    }

    public double standardDeviation() {
      return StatisticsUtil.standardDeviation(numEvents(), sum(), sumSquares());
    }

    public double median() { return percentile(50.0); }

    public double percentile(final double p) {
      return StatisticsUtil.percentile(p, bounds(), minValue(), maxValue(), numEvents(), events, 0);
    }
  }

  // ====================================================================================================
  //  Default Bounds
  // ====================================================================================================
  long[] DEFAULT_DURATION_BOUNDS_MS = new long[] {
    5, 10, 25, 50, 75, 100, 150, 250, 350, 500, 750,  // msec
    1000, 2500, 5000, 10000, 25000, 50000, 60000,     // sec
    75000, 120000,                                    // min
  };

  long[] DEFAULT_SMALL_DURATION_BOUNDS_NS = new long[] {
    25, 50, 75, 100, 500, 1_000,                                      // nsec
    10_000, 25_000, 50_000, 75_000, 100_000,                          // usec
    250_000, 500_000, 750_000,
    1_000_000, 5000000L, 10000000L, 25000000L, 50000000L, 75000000L,  // msec
  };

  long[] DEFAULT_DURATION_BOUNDS_NS = new long[] {
    25, 50, 100,                                                              // nsec
    1_000, 10_000, 50_000, 100_000, 250_000, 500_000, 750_000,                // usec
    1_000_000, 5000000L, 10000000L, 25000000L, 50000000L, 75000000L,          // msec
    100000000L, 150000000L, 250000000L, 350000000L, 500000000L, 750000000L,   // msec
    1000000000L, 2500000000L, 5000000000L, 10000000000L, 25000000000L,        // sec
    60000000000L, 120000000000L, 300000000000L, 600000000000L, 900000000000L  // min
  };

  long[] DEFAULT_SIZE_BOUNDS = new long[] {
    0, 128, 256, 512,
    1 << 10, 2 << 10, 4 << 10, 8 << 10, 16 << 10, 32 << 10, 64 << 10, 128 << 10, 256 << 10, 512 << 10, // kb
    1 << 20, 2 << 20, 4 << 20, 8 << 20, 16 << 20, 32 << 20, 64 << 20, 128 << 20, 256 << 20, 512 << 20, // mb
  };

  long[] DEFAULT_SMALL_SIZE_BOUNDS = new long[] {
    0, 32, 64, 128, 256, 512,
    1 << 10, 2 << 10, 4 << 10, 8 << 10, 16 << 10, 32 << 10,
    64 << 10, 128 << 10, 256 << 10, 512 << 10, // kb
    1 << 20
  };

  long[] DEFAULT_COUNT_BOUNDS = new long[] {
    0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1000,
    2000, 2500, 5000, 10_000, 15_000, 20_000, 25_000,
    50_000, 75_000, 100_000, 250_000, 500_000, 1_000_000
  };
}
