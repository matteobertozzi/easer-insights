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

package io.github.matteobertozzi.easerinsights.jvm;

import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;

public final class JvmMemoryMetrics {
  public static final JvmMemoryMetrics INSTANCE = new JvmMemoryMetrics();

  private final MetricCollector allocatedMemory = new MetricCollector.Builder()
    .unit(DatumUnit.BYTES)
    .name("jvm.memory.allocated_memory")
    .label("JVM Allocated Memory")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final MetricCollector usedMemory = new MetricCollector.Builder()
    .unit(DatumUnit.BYTES)
    .name("jvm.memory.used_memory")
    .label("JVM Used Memory")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final MetricCollector usedMemoryHisto = new MetricCollector.Builder()
    .unit(DatumUnit.BYTES)
    .name("jvm.memory.allocated_memory_histo")
    .label("JVM Allocated Memory Histo")
    .register(Histogram.newMultiThreaded(new long[] {
      32 << 20, 64 << 20, 96 << 20, 128 << 20, 160 << 20, 224 << 20, 288 << 20,
      352 << 20, 416 << 20, 480 << 20, 544 << 20, 672 << 20, 800 << 20, 928 << 20,
      1056 << 20, 1312 << 20, 1568 << 20, 1824 << 20, 2080L << 20, 2336L << 20,
        2592L << 20, 2848L << 20, 3104L << 20, 3360L << 20, 3616L << 20, 3872L << 20,
        4128L << 20, 4384L << 20, 4640L << 20, 4896L << 20, 5152L << 20, 5408L << 20,
        5664L << 20, 5920L << 20, 6176L << 20, 6432L << 20, 6688L << 20, 6944L << 20,
        7200L << 20, 7456L << 20, 7712L << 20, 7968L << 20, 8192L << 20
    }));

  private JvmMemoryMetrics() {
    // no-op
  }

  public void collect(final long now) {
    final Runtime runtime = Runtime.getRuntime();
    final long memTotal = runtime.totalMemory();
    final long memUsed = memTotal - runtime.freeMemory();
    allocatedMemory.update(now, memTotal);
    usedMemory.update(now, memUsed);
    usedMemoryHisto.update(memUsed);
  }

  public long getUsedMemory() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  public long getAvailableMemory() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
  }

  public long getMaxMemory() {
    return Runtime.getRuntime().maxMemory();
  }

  public long getTotalMemory() {
    return Runtime.getRuntime().totalMemory();
  }

  public long getFreeMemory() {
    return Runtime.getRuntime().freeMemory();
  }
}
