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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.connection.DbConnectionPool.AbstractDbConnectionPoolWithCleaner;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimension;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeDrag;
import io.github.matteobertozzi.rednaco.collections.pool.MultiObjectPool;
import io.github.matteobertozzi.rednaco.strings.StringConverter;

public class DbGlobalConnectionPool extends AbstractDbConnectionPoolWithCleaner {
  private static final int MAX_POOLED_CONNECTIONS = StringConverter.toInt(System.getProperty("easer.insights.jdbc.pool.max.pooled.connections"), 16);

  private static final MetricDimension<TimeRangeDrag> poolSize = Metrics.newCollectorWithDimensions()
    .dimensions("name")
    .unit(DatumUnit.COUNT)
    .name("jdbc.pool.size.global")
    .label("JDBC Global Pool Size")
    .register(() -> TimeRangeDrag.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  private final GlobalPool pool;

  public DbGlobalConnectionPool(final String name) {
    this(name, MAX_POOLED_CONNECTIONS);
  }

  public DbGlobalConnectionPool(final String name, final int maxPooledConnections) {
    this(name, maxPooledConnections, Duration.ofMillis(MAX_CONNECTION_OPEN_MS), Duration.ofMillis(MAX_CONNECTION_IDLE_MS));
  }

  public DbGlobalConnectionPool(final String name, final int maxPooledConnections, final Duration maxConnectionOpen, final Duration maxConnectionIdle) {
    super(name, maxConnectionOpen, maxConnectionIdle);
    this.pool = new GlobalPool(poolSize.get(name), maxPooledConnections);
  }

  // ====================================================================================================
  // Internal Access Helpers
  // ====================================================================================================
  @Override
  protected DbConnection getRawConnection(final DbInfo dbInfo) {
    final DbConnection conn = pool.poll(dbInfo);
    return verifyPooledConnection(conn);
  }

  @Override
  protected boolean addToPool(final Thread thread, final DbConnection connection) {
    Logger.debug("add to pool: {} {}", connection.isClosed(), connection);
    if (connection.isClosed() || !connection.isValid()) {
      Logger.debug("trying to add a closed connection to the pool: {}", connection);
      connection.setPool(null);
      return false;
    }

    if (!pool.add(connection)) {
      Logger.debug("the pool is full, closing the connection: {} {}", thread, connection);
      return false;
    }
    return true;
  }

  @Override
  protected void cleanExpired() {
    final int active = pool.cleanExpired(this);
    Logger.debug("the pool was cleaned. {} active connections.", active);
  }

  // ====================================================================================================
  // PRIVATE Helpers
  // ====================================================================================================
  private static final class GlobalPool extends MultiObjectPool<DbInfo, DbConnection> {
    private GlobalPool(final TimeRangeDrag poolSizeTrc, final int size) {
      super(size, poolSizeTrc::sample);
    }

    private boolean add(final DbConnection con) {
      return super.add(con.getDbInfo(), con);
    }

    private int cleanExpired(final DbGlobalConnectionPool pool) {
      final long now = System.nanoTime();
      return clean(con -> pool.isExpired(con, now), con -> pool.closePooledConnection(con, "cleaning up", false));
    }
  }
}
