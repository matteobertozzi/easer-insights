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

import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.util.DatumUnitConverter;

public abstract class MaxAvgTimeRangeGauge implements MetricDatumCollector {
  protected MaxAvgTimeRangeGauge() {
    // no-op
  }

  @Override
  public String type() {
    return "MAX_AVG_TIME_RANGE_GAUGE";
  }

  // ==========================================================================================
  public static MaxAvgTimeRangeGauge newSingleThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return new MaxAvgTimeRangeGaugeImplSt(maxInterval, window, unit);
  }

  public static MaxAvgTimeRangeGauge newMultiThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return new MaxAvgTimeRangeGaugeImplMt(maxInterval, window, unit);
  }

  // ==========================================================================================
  protected abstract void set(long timestamp, long value);

  @Override
  public void update(final long timestamp, final long value) {
    set(timestamp, value);
  }

  // ==========================================================================================
  protected static final MaxAvgTimeRangeGaugeSnapshot EMPTY_SNAPSHOT = new MaxAvgTimeRangeGaugeSnapshot(0, 0, new long[0], new long[0]);
  public record MaxAvgTimeRangeGaugeSnapshot (long lastInterval, long window, long[] avg, long[] max) implements MetricDataSnapshot {
    public long getFirstInterval() {
      return lastInterval - (max.length * window);
    }

    public long getLastInterval() {
      return lastInterval + window;
    }

    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      if (window == 0) return report.append("(no data)\n");

      final DatumUnitConverter unitConverter = DatumUnitConverter.humanConverter(metricDefinition.unit());

      report.append("window ").append(DatumUnitConverter.humanTimeMillis(window));
      report.append(" - ").append(DatumUnitConverter.humanDateFromEpochMillis(getFirstInterval()));
      report.append(" - [");
      for (int i = 0; i < max.length; ++i) {
        if (i > 0) report.append(',');
        report.append(unitConverter.asHumanString(avg[i])).append('/').append(unitConverter.asHumanString(max[i]));
      }
      report.append("] - ");
      report.append(DatumUnitConverter.humanDateFromEpochMillis(getLastInterval()));
      report.append('\n');
      return report;
    }
  }
}
