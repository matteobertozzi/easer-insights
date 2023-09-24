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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimensions;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;

public final class JvmDiskMetrics {
  public static final JvmDiskMetrics INSTANCE = new JvmDiskMetrics();

  private final CopyOnWriteArraySet<File> dataDirs = new CopyOnWriteArraySet<>();

  private final MetricCollector fdsUsage = new MetricCollector.Builder()
    .unit(DatumUnit.COUNT)
    .name("jvm.fds.open.count")
    .label("JVM Open FDs")
    .register(MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final MetricDimensions diskUsed = new MetricDimensions.Builder()
    .dimensions("disk_path")
    .unit(DatumUnit.BYTES)
    .name("jvm.disk.used")
    .label("JVM Disk Used")
    .register(() -> MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private final MetricDimensions diskAvail = new MetricDimensions.Builder()
    .dimensions("disk_path")
    .unit(DatumUnit.BYTES)
    .name("jvm.disk.avail")
    .label("JVM Disk Avail")
    .register(() -> MaxAvgTimeRangeGauge.newMultiThreaded(60 * 24, 1, TimeUnit.MINUTES));

  private JvmDiskMetrics() {
    // no-op
  }

  public void collect(final long now) {
    fdsUsage.update(now, getOpenFileDescriptorCount());

    for (final File dir: dataDirs) {
      diskAvail.get(dir.getAbsolutePath()).update(now, dir.getFreeSpace());
      diskUsed.get(dir.getAbsolutePath()).update(now, dir.getUsableSpace());
    }
  }

  // ================================================================================
  //  Disk Related
  // ================================================================================
  public void registerDataDir(final File dataDir) {
    this.dataDirs.add(dataDir);
  }

  public void unregisterDataDir(final File dataDir) {
    this.dataDirs.remove(dataDir);
  }

  public boolean hasDataDirs() {
    return !this.dataDirs.isEmpty();
  }

  public Set<File> getDataDirs() {
    return this.dataDirs;
  }

  // ================================================================================
  //  FDs Related
  // ================================================================================
  public long getOpenFileDescriptorCount() {
    try {
      final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
      if (os instanceof final com.sun.management.UnixOperatingSystemMXBean unixBean) {
        return unixBean.getOpenFileDescriptorCount();
      }
    } catch (final Throwable e) {
      // no-op
    }
    return -1;
  }
}
