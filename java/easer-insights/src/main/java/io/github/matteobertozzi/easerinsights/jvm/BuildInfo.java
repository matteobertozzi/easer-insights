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

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class BuildInfo {
  private static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final DateTimeFormatter MVN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
  private static final String UNKNOWN = "unknown";

  private String name;
  private String version = UNKNOWN;
  private String buildDate = UNKNOWN;
  private String createdBy = UNKNOWN;
  private String gitBranch = UNKNOWN;
  private String gitHash = UNKNOWN;

  public BuildInfo() {
    // no-op
  }

  public BuildInfo(final String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("expected name to be not empty");
    }
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getBuildDate() {
    return buildDate;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public String getGitBranch() {
    return gitBranch;
  }

  public String getGitHash() {
    return gitHash;
  }

  public static BuildInfo loadInfoFromManifest(final String name) throws IOException {
    final BuildInfo buildInfo = new BuildInfo(name);
    buildInfo.loadInfoFromManifest();
    return buildInfo;
  }

  public void loadInfoFromManifest() throws IOException {
    final Enumeration<URL> resources = BuildInfo.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
    while (resources.hasMoreElements()) {
      final Manifest manifest = new Manifest(resources.nextElement().openStream());
      final Attributes attributes = manifest.getMainAttributes();
      if (!Objects.equals(attributes.getValue("Implementation-Title"), name)) {
        continue;
      }

      // parse build timestamp
      final ZonedDateTime utcBuildDate = LocalDateTime.parse(attributes.getValue("buildTimestamp"), MVN_DATE_FORMAT).atZone(ZoneOffset.UTC);
      final LocalDateTime localBuildDate = utcBuildDate.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

      // add service info
      this.buildDate = localBuildDate.format(LOCAL_DATE_FORMAT);
      this.version = attributes.getValue("Implementation-Version");
      this.createdBy = attributes.getValue("Built-By");
      if (this.createdBy == null || createdBy.isEmpty()) {
        this.createdBy = attributes.getValue("builtBy");
      }
      this.gitBranch = attributes.getValue("gitBranch");
      this.gitHash = attributes.getValue("gitHash");
      break;
    }
  }

  public boolean isValid() {
    return !Objects.equals(buildDate, UNKNOWN) && !Objects.equals(version, UNKNOWN);
  }

  @Override
  public String toString() {
    return "BuildInfo [name=" + name + ", version=" + version +
        ", buildDate=" + buildDate + ", createdBy=" + createdBy +
        ", gitBranch=" + gitBranch + ", gitHash=" + gitHash + "]";
  }
}
