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

package io.github.matteobertozzi.easerinsights.jdbc.connection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimensionGroup;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Heatmap;
import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeDrag;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

final class DbStats extends MetricDimensionGroup {
  private static final String[] DIMENSION_KEYS = new String[] { "db_host", "db_name" };

  private final String key;

  private DbStats(final DbInfo dbInfo, final String key) {
    super(DIMENSION_KEYS, dbInfo.host(), dbInfo.dbName());
    this.key = key;
  }

  private static final ConcurrentHashMap<String, DbStats> groups = new ConcurrentHashMap<>();
  static DbStats get(final DbInfo dbInfo) {
    final String key = dbInfo.host() + "/" + dbInfo.dbName();
    final DbStats stats = groups.get(key);
    if (stats != null) return stats;

    return groups.computeIfAbsent(key, k -> new DbStats(dbInfo, key));
  }

  // ====================================================================================================
  //  Global Db Metrics
  // ====================================================================================================
  private static final TopK connectionTopTimes = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.connection.top.times")
    .label("JDBC Connection 15 Top Times")
    .register(TopK.newMultiThreaded(15, 24, 1, TimeUnit.HOURS));

  // ====================================================================================================
  //  Connections Related
  // ====================================================================================================
  private final MaxAvgTimeRangeGauge connectionTime = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.connection.time")
    .label("JDBC Connection Time")
    .register(this, MaxAvgTimeRangeGauge.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private final MaxAvgTimeRangeGauge connectionFailures = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.connection.failure.time")
    .label("JDBC Connection Failure Time")
    .register(this, MaxAvgTimeRangeGauge.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private final MaxAvgTimeRangeGauge poolConnectionTime = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.pool.connection.time")
    .label("JDBC Connection Pool Time")
    .register(this, MaxAvgTimeRangeGauge.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private final TimeRangeDrag activeConnections = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("jdbc.connection.active")
    .label("JDBC Active Connections")
    .register(this, TimeRangeDrag.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  // ====================================================================================================
  //  Fetch Queries Related
  // ====================================================================================================
  private final TimeRangeCounter fetchQueryFailures = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("jdbc.fetch.query.failures")
    .label("JDBC Fetch Query Failures")
    .register(this, TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private final Heatmap fetchQueryTime  = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.fetch.query.time")
    .label("JDBC Fetch Query Time")
    .register(this, Heatmap.newMultiThreaded(60, 1, TimeUnit.MINUTES, Histogram.DEFAULT_DURATION_BOUNDS_NS));

  private final TopK fetchQueryTopTimes = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.fetch.query.top.times")
    .label("JDBC Fetch Query Top 15 Times")
    .register(this, TopK.newMultiThreaded(15, 24, 1, TimeUnit.HOURS));

  private final TopK fetchQueryTopRows = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("jdbc.fetch.query.top.rows")
    .label("JDBC Fetch Query Top 15 Rows")
    .register(this, TopK.newMultiThreaded(15, 24, 1, TimeUnit.HOURS));

  // ====================================================================================================
  //  Update Queries Related
  // ====================================================================================================
  private final TimeRangeCounter updateQueryFailures = Metrics.newCollector()
    .unit(DatumUnit.COUNT)
    .name("jdbc.update.query.failures")
    .label("JDBC Update Query Failures")
    .register(this, TimeRangeCounter.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private final Heatmap updateQueryTime  = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.update.query.time")
    .label("JDBC Update Query Time")
    .register(this, Heatmap.newMultiThreaded(60, 1, TimeUnit.MINUTES, Histogram.DEFAULT_DURATION_BOUNDS_NS));

  private final TopK updateQueryTopTimes = Metrics.newCollector()
    .unit(DatumUnit.NANOSECONDS)
    .name("jdbc.update.query.top.times")
    .label("JDBC Update Query Top 15 Times")
    .register(this, TopK.newMultiThreaded(15, 24, 1, TimeUnit.HOURS));

  // ====================================================================================================
  //  Connections Related
  // ====================================================================================================
  public void incOpenConnections() {
    activeConnections.inc();
  }

  public void decOpenConnections() {
    activeConnections.dec();
  }

  public void addConnectionTime(final long elapsedNs) {
    connectionTime.sample(elapsedNs);
    connectionTopTimes.sample(key, elapsedNs);
  }

  public void addConnectionFailure(final long elapsedNs) {
    connectionFailures.sample(elapsedNs);
    connectionTopTimes.sample(key, elapsedNs);
  }

  public void addPoolConnectionTime(final long elapsedNs) {
    poolConnectionTime.sample(elapsedNs);
  }

  // ====================================================================================================
  //  Fetch Queries Related
  // ====================================================================================================
  public void addExecuteQuery(final String sql, final long elapsedNs, final long rowCount) {
    final long now = TimeUtil.currentEpochMillis();
    fetchQueryTime.sample(now, elapsedNs);
    fetchQueryTopTimes.sample(sql, now, elapsedNs);
    fetchQueryTopRows.sample(sql, now, rowCount);
  }

  public void addExecuteQueryFailure(final String sql, final long elapsedNs) {
    final long now = TimeUtil.currentEpochMillis();
    fetchQueryFailures.inc(now);
    fetchQueryTime.sample(now, elapsedNs);
    fetchQueryTopTimes.sample(sql, now, elapsedNs);
  }

  // ====================================================================================================
  //  Update Queries Related
  // ====================================================================================================
  public void addExecuteUpdate(final String sql, final long elapsedNs) {
    final long now = TimeUtil.currentEpochMillis();
    updateQueryTime.sample(now, elapsedNs);
    updateQueryTopTimes.sample(sql, now, elapsedNs);
  }

  public void addExecuteUpdateFailure(final String sql, final long elapsedNs) {
    final long now = TimeUtil.currentEpochMillis();
    updateQueryFailures.inc(now);
    updateQueryTime.sample(now, elapsedNs);
    updateQueryTopTimes.sample(sql, now, elapsedNs);
  }
}
