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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.rednaco.strings.HumansTableView;

public final class JvmCpuMetrics {
  public static final JvmCpuMetrics INSTANCE = new JvmCpuMetrics();

  private final MaxAvgTimeRangeGauge cpuUsage = Metrics.newCollector()
    .unit(DatumUnit.PERCENT)
    .name("jvm.cpu.cpu_usage")
    .label("JVM CPU Usage")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final TimeRangeCounter cpuTime = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jvm.cpu.cpu_time")
    .label("JVM CPU Time")
    .register(TimeRangeCounter.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final MaxAvgTimeRangeGauge threadCount = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("jvm.cpu.thread_count")
    .label("JVM Thread Count")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private JvmCpuMetrics() {
    // no-op
  }

  private long lastCpuTimeNs = 0;
  public void collect(final long now) {
    cpuUsage.sample(now, getCpuUsage());
    final long nowCpuTimeNs = getCpuTimeNs();
    cpuTime.add(now, nowCpuTimeNs - lastCpuTimeNs);
    lastCpuTimeNs = nowCpuTimeNs;

    threadCount.sample(now, getThreadCount());
  }

  // ================================================================================
  //  Threads Related
  // ================================================================================
  public int availableProcessors() {
    return Runtime.getRuntime().availableProcessors();
  }

  public int getThreadCount() {
    return Thread.activeCount();
  }

  // ================================================================================
  //  CPU Related
  // ================================================================================
  public long getCpuUsage() {
    final OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
    if (bean instanceof final com.sun.management.OperatingSystemMXBean osBean) {
      return Math.round(osBean.getProcessCpuLoad() * 100);
    }
    return -1;
  }

  public long getCpuTimeNs() {
    final OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
    if (bean instanceof final com.sun.management.OperatingSystemMXBean osBean) {
      return osBean.getProcessCpuTime();
    }
    return -1;
  }

  // ================================================================================
  //  Snapshot/Human Report Related
  // ================================================================================
  public JvmThreadInfo[] threadInfoSnapshot() {
    final Set<Thread> threads = Thread.getAllStackTraces().keySet();
    int index = 0;
    final JvmThreadInfo[] threadInfo = new JvmThreadInfo[threads.size()];
    for (final Thread thread: threads) {
      threadInfo[index++] = new JvmThreadInfo(thread);
    }
    Arrays.sort(threadInfo);
    return threadInfo;
  }

  private static final List<String> THREAD_INFO_HEADER = List.of("State", "Name", "Group", "Type", "Priority");
  public StringBuilder addToHumanReport(final StringBuilder report) {
    final HumansTableView table = new HumansTableView();
    table.addColumns(THREAD_INFO_HEADER);
    for (final JvmThreadInfo thread: threadInfoSnapshot()) {
      table.addRow(thread.state(), thread.name(), thread.group(),
        thread.isVirtual() ? "Virtual" : thread.isDaemon() ? "Daemon" : "Normal",
        thread.priority());
    }
    return table.addHumanView(report);
  }

  public record JvmThreadInfo (String name, String group, Thread.State state, boolean isDaemon, boolean isVirtual, int priority) implements Comparable<JvmThreadInfo> {
    public JvmThreadInfo(final Thread thread) {
      this(thread.getName(), thread.getThreadGroup().getName(), thread.getState(), thread.isDaemon(), thread.isVirtual(), thread.getPriority());
    }

    @Override
    public int compareTo(final JvmThreadInfo other) {
      final int cmp = state.compareTo(other.state);
      return (cmp != 0) ? cmp : name.compareTo(other.name);
    }
  }
}
