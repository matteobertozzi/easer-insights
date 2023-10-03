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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.CounterMap;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.util.TimeUtil;

public final class JvmGcMetrics {
  public static final JvmGcMetrics INSTANCE = new JvmGcMetrics();

  private final MaxAvgTimeRangeGauge gcCurrentPhaseDuration = Metrics.newCollector()
    .unit(DatumUnit.MILLISECONDS)
    .name("jvm.gc.current.phase.duration")
    .label("JVM GC Current Phase Duration")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final MaxAvgTimeRangeGauge gcPauseDuration = Metrics.newCollector()
    .unit(DatumUnit.MILLISECONDS)
    .name("jvm.gc.pause.duration")
    .label("JVM GC Pause Duration")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final CounterMap gcPhaseCause = Metrics.newCollector()
      .unit(DatumUnit.COUNT)
      .name("jvm.gc.phase.cause")
      .label("JVM GC Phase Cause")
      .register(CounterMap.newMultiThreaded());

  private JvmGcMetrics() {
    registerListeners();
  }

  public void collect(final long now) {
    // no-op
  }

  private static void onGcNotification(final Notification notification, final Object ref) {
    final GarbageCollectionNotificationInfo gcInfo = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
    ((JvmGcMetrics)ref).onGcNotification(gcInfo);
  }

  private void onGcNotification(final GarbageCollectionNotificationInfo notification) {
    final GcInfo gcInfo = notification.getGcInfo();

    final long now = TimeUtil.currentEpochMillis();
    gcPhaseCause.inc(notification.getGcName() + ": " + notification.getGcCause(), now);
    if (isConcurrentPhase(notification.getGcCause(), notification.getGcName())) {
      gcCurrentPhaseDuration.sample(now, gcInfo.getDuration());
    } else {
      gcPauseDuration.sample(now, gcInfo.getDuration());
    }
  }

  static boolean isConcurrentPhase(final String cause, final String name) {
    switch (name) {
      case "NO GC":
      case "ZGC Cycles":
      case "Shenandoah Cycles":
        return true;
      default:
        return name.startsWith("GPGC") && !name.endsWith("Pauses");
    }
  }

  private void registerListeners() {
    for (final GarbageCollectorMXBean mbean: ManagementFactory.getGarbageCollectorMXBeans()) {
      if (!(mbean instanceof final NotificationEmitter notificationEmitter)) {
        continue;
      }

      notificationEmitter.addNotificationListener(JvmGcMetrics::onGcNotification, JvmGcMetrics::isGarbageCollectionNotification, this);
    }
  }

  private static boolean isGarbageCollectionNotification(final Notification notification) {
    return notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION);
  }
}
