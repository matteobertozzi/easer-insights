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

import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.util.DatumUnitConverter;

public abstract class Histogram implements MetricDatumCollector {
  private final long[] bounds;

  protected Histogram(final long[] bounds) {
    this.bounds = bounds;
  }

  @Override
  public String type() {
    return "HISTOGRAM";
  }

  // ==========================================================================================
  public static Histogram newSingleThreaded(final long[] bounds) {
    return new HistogramImplSt(bounds);
  }

  public static Histogram newMultiThreaded(final long[] bounds) {
    return new HistogramImplMt(bounds);
  }

  // ==========================================================================================
  @Override
  public void update(final long timestamp, final long value) {
    final int boundIndex = findBoundIndex(bounds, value);
    add(boundIndex, value);
  }

  protected abstract void add(int boundIndex, long value);

  private static int findBoundIndex(final long[] bounds, final long value) {
    for (int i = 0; i < bounds.length; ++i) {
      if (value < bounds[i]) {
        return i;
      }
    }
    return bounds.length;
  }

  protected long bound(final int index) {
    return bounds[index];
  }

  // ==========================================================================================
  protected static final HistogramSnapshot EMPTY_SNAPSHOT = new HistogramSnapshot(new long[0], new long[0], 0, 0, 0, 0);
  public record HistogramSnapshot(long[] bounds, long[] events, long numEvents, long minValue, long sum, long sumSquares) implements MetricDataSnapshot {
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
      if (numEvents() == 0) return 0;
      return (double) sum() / numEvents();
    }

    public double standardDeviation() {
      if (numEvents() == 0) return 0;

      final double dNumEvents = numEvents();
      final double dSum = sum();
      final double dSumSquares = sumSquares();
      final double variance = (dSumSquares * dNumEvents - dSum * dSum) / (dNumEvents * dNumEvents);
      return Math.sqrt(Math.max(variance, 0.0));
    }

    public double median() { return percentile(50.0); }

    public double percentile(final double p) {
      if (numEvents == 0) return 0;

      final long maxValue = bounds[bounds.length - 1];

      final double threshold = numEvents() * (p / 100.0);
      long cumulativeSum = 0;
      for (int b = 0; b < bounds.length; b++) {
        final long bucketValue = events[b];
        cumulativeSum += bucketValue;
        if (cumulativeSum >= threshold) {
          // Scale linearly within this bucket
          final long leftPoint = (b == 0) ? minValue : bounds[b - 1];
          final long rightPoint = bounds[b];
          final long leftSum = cumulativeSum - bucketValue;
          double pos = 0;
          final long rightLeftDiff = cumulativeSum - leftSum;
          if (rightLeftDiff != 0) {
            pos = (threshold - leftSum) / rightLeftDiff;
          }
          double r = leftPoint + (rightPoint - leftPoint) * pos;
          if (r < minValue) r = minValue;
          if (r > maxValue) r = maxValue;
          return r;
        }
      }
      return maxValue;
    }
  }
}
