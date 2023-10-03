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

package io.github.matteobertozzi.easerinsights.metrics;

public interface MetricDatumCollector {
  enum MetricDatumUpdateType { SAMPLE, DELTA }

  MetricCollector newCollector(MetricDefinition definition, int metricId);

  MetricDataSnapshot dataSnapshot();

  interface MetricDataSnapshot {
    StringBuilder addToHumanReport(MetricDefinition metricDefinition, StringBuilder report);
  }

  interface MetricDatumCollectorWithDimensions extends MetricDatumCollector {
    default String buildKey(final String[] dimensionKeys, final String[] dimensionValues) {
      final StringBuilder key = new StringBuilder(32);
      for (int i = 0; i < dimensionKeys.length; ++i) {
        if (i > 0) key.append(", ");
        key.append(dimensionKeys[i]).append(":").append(dimensionValues[i]);
      }
      return key.toString();
    }
  }
}