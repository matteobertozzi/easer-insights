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
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.TimeRangeCounterCollector;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;

public interface TimeRangeCounter extends CollectorCounter, MetricDatumCollector {
  static TimeRangeCounter newSingleThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return TimeRangeCounterCollector.newSingleThreaded(maxInterval, window, unit);
  }

  static TimeRangeCounter newMultiThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return TimeRangeCounterCollector.newMultiThreaded(maxInterval, window, unit);
  }

  @Override
  default MetricCollector newCollector(final MetricDefinition definition, final int metricId) {
    return new TimeRangeCounterCollector(definition, this, metricId);
  }

  // ====================================================================================================
  //  Snapshot related
  // ====================================================================================================
  @Override TimeRangeCounterSnapshot dataSnapshot();

  record TimeRangeCounterSnapshot (long lastInterval, long window, long[] counters) implements MetricDataSnapshot {
    public static final TimeRangeCounterSnapshot EMPTY_SNAPSHOT = new TimeRangeCounterSnapshot(0, 0, new long[0]);

    public long getFirstInterval() {
      return lastInterval - (counters.length * window);
    }

    public long getLastInterval() {
      return lastInterval + window;
    }

    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      if (window == 0) return report.append("(no data)\n");

      final DatumUnitConverter unitConverter = metricDefinition.unit().humanConverter();

      report.append("window ").append(HumansUtil.humanTimeMillis(window));
      report.append(" - ").append(HumansUtil.humanDateFromEpochMillis(getFirstInterval()));
      report.append(" - [");
      for (int i = 0, n = counters.length; i < n; ++i) {
        if (i > 0) report.append(',');
        report.append(unitConverter.asHumanString(counters[i]));
      }
      report.append("] - ");
      report.append(HumansUtil.humanDateFromEpochMillis(getLastInterval()));
      report.append('\n');
      return report;
    }
  }
}
