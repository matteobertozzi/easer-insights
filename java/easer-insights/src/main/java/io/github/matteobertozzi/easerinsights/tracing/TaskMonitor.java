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

package io.github.matteobertozzi.easerinsights.tracing;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinitionUtil;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK;
import io.github.matteobertozzi.easerinsights.tracing.Span.SpanState;
import io.github.matteobertozzi.rednaco.strings.HumansTableView;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

public final class TaskMonitor {
  public static final TaskMonitor INSTANCE = new TaskMonitor();

  private final TopK topSlowTasks = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("task.monitor.slow.tasks")
    .label("Slow Tasks")
    .register(TopK.newMultiThreaded(10, 60, 5, TimeUnit.MINUTES));

  private TaskMonitor() {
    Tracer.subscribeToRootSpanEvents(this::onRootSpanStateChange);
  }

  private final ConcurrentLinkedDeque<RootSpan> recentlyCompleted = new ConcurrentLinkedDeque<>();
  private final Set<RootSpan> activeTasks = ConcurrentHashMap.newKeySet();

  private void onRootSpanStateChange(final RootSpan span) {
    Logger.debug("ROOT SPAN EVENT: {}", span);
    if (span.state() == SpanState.IN_PROGRESS) {
      activeTasks.add(span);
    } else {
      activeTasks.remove(span);
      if (recentlyCompleted.size() > 20) {
        recentlyCompleted.removeLast();
      }
      recentlyCompleted.addFirst(span);
    }
  }

  public StringBuilder addActiveTasksToHumanReport(final StringBuilder report) {
    final ArrayList<Span> activeTaskList = new ArrayList<>(activeTasks);
    activeTaskList.sort((a, b) -> Long.compare(b.startUnixNanos(), a.startUnixNanos()));

    final HumansTableView table = new HumansTableView();
    table.addColumns("Thread", "TenantId", "TraceId", "Queue Time", "Run Time", "Name");

    final long now = TimeUtil.currentEpochNanos();
    for (final Span task: activeTaskList) {
      final Map<String, Object> attrs = task.attributes();
      final long queueTimeNs = TraceAttributes.TASK_QUEUE_TIME_NS.get(attrs, -1);
      final long elapsed = now - task.startUnixNanos();
      table.addRow(
        TraceAttributes.THREAD_NAME.get(attrs, null),
        TraceAttributes.TENANT_NAME.get(attrs, null),
        task.traceId() + ":" + task.parentId() + ":" + task.spanId(),
        queueTimeNs >= 0 ? HumansUtil.humanTimeNanos(queueTimeNs) : "",
        HumansUtil.humanTimeNanos(elapsed),
        task.name()
      );
    }

    return table.addHumanView(report);
  }

  public StringBuilder addRecentlyCompletedTasksToHumanReport(final StringBuilder report) {
    final ArrayList<Span> completedTaskList = new ArrayList<>(recentlyCompleted);
    completedTaskList.sort((a, b) -> Long.compare(b.startUnixNanos(), a.startUnixNanos()));

    final HumansTableView table = new HumansTableView();
    table.addColumns("Thread", "TenantId", "TraceId", "Start Time", "Queue Time", "Execution Time", "Name", "Status");
    for (final Span task: completedTaskList) {
      final Map<String, Object> attrs = task.attributes();
      final long queueTimeNs = TraceAttributes.TASK_QUEUE_TIME_NS.get(attrs, -1);

      table.addRow(
        TraceAttributes.THREAD_NAME.get(attrs, null),
        TraceAttributes.TENANT_NAME.get(attrs, null),
        task.traceId() + ":" + task.parentId() + ":" + task.spanId(),
        dateFromNanos(task.startUnixNanos()),
        queueTimeNs >= 0 ? HumansUtil.humanTimeNanos(queueTimeNs) : "",
        HumansUtil.humanTimeNanos(task.elapsedNanos()),
        task.name(),
        task.state()
      );
    }

    return table.addHumanView(report);
  }

  private static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
  private static String dateFromNanos(final long nanos) {
    final Instant instant = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(nanos));
    return LOCAL_DATE_FORMAT.format(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
  }

  public StringBuilder addSlowTasksToHumanReport(final StringBuilder report) {
    final MetricDefinition def = MetricDefinitionUtil.newMetricDefinition("slow.task", DatumUnit.NANOSECONDS, null, null);
    return topSlowTasks.dataSnapshot().addToHumanReport(def, report);
  }
}
