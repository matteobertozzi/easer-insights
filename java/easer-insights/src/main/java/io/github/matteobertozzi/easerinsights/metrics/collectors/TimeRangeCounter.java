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

public abstract class TimeRangeCounter implements MetricDatumCollector {
  protected TimeRangeCounter() {
    // no-op
  }

  @Override
  public String type() {
    return "TIME_RANGE_COUNTER";
  }

  // ==========================================================================================
  public static TimeRangeCounter newSingleThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return new TimeRangeCounterImplSt(maxInterval, window, unit);
  }

  public static TimeRangeCounter newMultiThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return new TimeRangeCounterImplMt(maxInterval, window, unit);
  }

  // ==========================================================================================
  protected abstract long add(long timestamp, long delta);

  @Override
  public void update(final long timestamp, final long value) {
    add(timestamp, value);
  }

  // ==========================================================================================
  protected static final TimeRangeCounterSnapshot EMPTY_SNAPSHOT = new TimeRangeCounterSnapshot(0, 0, new long[0]);
  public record TimeRangeCounterSnapshot (long lastInterval, long window, long[] counters) implements MetricDataSnapshot {
    public long getFirstInterval() {
      return lastInterval - (counters.length * window);
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
      for (int i = 0, n = counters.length; i < n; ++i) {
        if (i > 0) report.append(',');
        report.append(unitConverter.asHumanString(counters[i]));
      }
      report.append("] - ");
      report.append(DatumUnitConverter.humanDateFromEpochMillis(getLastInterval()));
      report.append('\n');
      return report;
    }
  }
}
