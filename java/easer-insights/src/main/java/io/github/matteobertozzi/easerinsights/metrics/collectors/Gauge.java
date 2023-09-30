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

public abstract class Gauge implements MetricDatumCollector {
  protected Gauge() {
    // no-op
  }

  @Override
  public String type() {
    return "GAUGE";
  }

  // ==========================================================================================
  public static Gauge newSingleThreaded() {
    return new GaugeImplSt();
  }

  public static Gauge newMultiThreaded() {
    return new GaugeImplMt();
  }

  // ==========================================================================================
  protected abstract void set(long timestamp, long value);

  @Override
  public void update(final long timestamp, final long value) {
    set(timestamp, value);
  }

  // ==========================================================================================
  protected static final GaugeSnapshot EMPTY_SNAPSHOT = new GaugeSnapshot(0, 0);
  public record GaugeSnapshot (long timestamp, long value) implements MetricDataSnapshot {
    @Override
    public StringBuilder addToHumanReport(final MetricDefinition metricDefinition, final StringBuilder report) {
      if (timestamp == 0) return report.append("(no data)\n");

      final DatumUnitConverter unitConverter = DatumUnitConverter.humanConverter(metricDefinition.unit());

      report.append(DatumUnitConverter.humanDateFromEpochMillis(timestamp));
      report.append(": ");
      report.append(unitConverter.asHumanString(value));
      return report;
    }
  }
}
