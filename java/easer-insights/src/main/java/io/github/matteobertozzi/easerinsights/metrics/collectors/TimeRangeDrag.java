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

import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDatumCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter.TimeRangeCounterSnapshot;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.TimeRangeDragCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.TimeRangeDragImplMt;
import io.github.matteobertozzi.easerinsights.metrics.collectors.impl.TimeRangeDragImplSt;

public interface TimeRangeDrag extends CollectorCounter, MetricDatumCollector {
  static TimeRangeDrag newSingleThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return new TimeRangeDragImplSt(maxInterval, window, unit);
  }

  static TimeRangeDrag newMultiThreaded(final long maxInterval, final long window, final TimeUnit unit) {
    return new TimeRangeDragImplMt(maxInterval, window, unit);
  }

  @Override
  default MetricCollector newCollector(final MetricDefinition definition, final int metricId) {
    return new TimeRangeDragCollector(definition, this, metricId);
  }

  @Override TimeRangeCounterSnapshot dataSnapshot();
}
