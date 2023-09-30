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
import java.util.Objects;

import io.github.matteobertozzi.easerinsights.util.DatumUnitConverter;

public final class JvmMetrics {
  public static final JvmMetrics INSTANCE = new JvmMetrics();

  private BuildInfo buildInfo = null;

  private JvmMetrics() {
    // no-op
  }

  public void collect(final long now) {
    JvmGcMetrics.INSTANCE.collect(now);
    JvmCpuMetrics.INSTANCE.collect(now);
    JvmMemoryMetrics.INSTANCE.collect(now);
    JvmDiskMetrics.INSTANCE.collect(now);
  }

  // ================================================================================
  //  Build Info
  // ================================================================================
  public BuildInfo buildInfo() {
    return buildInfo;
  }

  public void setBuildInfo(final BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
  }

  // ================================================================================
  //  Uptime Related
  // ================================================================================
  public long getPid() {
    return ProcessHandle.current().pid();
  }

  public long getStartTime() {
    return ManagementFactory.getRuntimeMXBean().getStartTime();
  }

  public long getUptime() {
    return ManagementFactory.getRuntimeMXBean().getUptime();
  }

  // ================================================================================
  //  System Related
  // ================================================================================
  public String getJavaVersion() {
    return System.getProperty("java.vm.name")
        + " " + getJavaVersionNumber()
        + " (" + getJavaVendor() + ")";
  }

  public String getJavaVersionNumber() {
    return System.getProperty("java.vm.version");
  }

  public String getJavaVendor() {
    final String vendor = System.getProperty("java.vendor");
    final String vendorVersion = System.getProperty("java.vendor.version");
    if (Objects.equals(vendor, vendorVersion)) return vendor;
    return vendor + " " + vendorVersion;
  }

  public String getOsName() {
    return System.getProperty("os.name");
  }

  public String getOsVersion() {
    return System.getProperty("os.version");
  }

  public String getOsArch() {
    return System.getProperty("os.arch");
  }

  public StringBuilder addToHumanReport(final StringBuilder report) {
    // Java Version
    report.append(JvmMetrics.INSTANCE.getJavaVersion()).append("\n");

    // Build Info
    if (buildInfo != null) {
      report.append(" - BuildInfo: ");
      report.append(buildInfo.getName()).append(" ").append(buildInfo.getVersion());
      report.append(" (").append(buildInfo.getBuildDate()).append(")\n");
      report.append(" - Built by ").append(buildInfo.getCreatedBy());
      report.append(" from ").append(buildInfo.getGitBranch());
      report.append(" ").append(buildInfo.getGitHash());
      report.append("\n");
    }

    // OS
    report.append(" - OS: ");
    report.append(JvmMetrics.INSTANCE.getOsName()).append(" ");
    report.append(JvmMetrics.INSTANCE.getOsVersion()).append(" (");
    report.append(JvmMetrics.INSTANCE.getOsArch()).append(")\n");

    // JVM Memory
    report.append(" - Memory:");
    report.append(" Max ").append(DatumUnitConverter.humanSize(JvmMemoryMetrics.INSTANCE.getMaxMemory()));
    report.append(" - Allocated ").append(DatumUnitConverter.humanSize(JvmMemoryMetrics.INSTANCE.getTotalMemory()));
    report.append(" - Used ").append(DatumUnitConverter.humanSize(JvmMemoryMetrics.INSTANCE.getUsedMemory()));
    report.append("\n");

    // JVM Uptime
    report.append(" - Uptime: ").append(DatumUnitConverter.humanTimeMillis(getUptime()));
    report.append("\n");

    // PID
    report.append(" - PID: ").append(getPid());
    report.append("\n");

    return report;
  }
}
