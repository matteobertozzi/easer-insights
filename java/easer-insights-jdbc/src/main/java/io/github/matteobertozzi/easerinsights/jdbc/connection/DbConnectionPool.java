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

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.logging.LogUtil;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimension;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.tracing.Span;
import io.github.matteobertozzi.easerinsights.tracing.TraceAttributes;
import io.github.matteobertozzi.easerinsights.tracing.Tracer;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;
import io.github.matteobertozzi.rednaco.strings.StringConverter;
import io.github.matteobertozzi.rednaco.threading.ShutdownUtil.StopSignal;
import io.github.matteobertozzi.rednaco.threading.SpinningThread;
import io.github.matteobertozzi.rednaco.threading.ThreadUtil;

public abstract class DbConnectionPool implements StopSignal, Closeable {
  protected static final boolean TRACE_CONNECTION_POOL = StringConverter.toBoolean(System.getProperty("easer.insights.jdbc.pool.trace.connections"), false);
  protected static final long MAX_CONNECTION_OPEN_MS = StringConverter.toLong(System.getProperty("easer.insights.jdbc.pool.max.connection.open.ms"), TimeUnit.MINUTES.toMillis(5));
  protected static final long MAX_CONNECTION_IDLE_MS = StringConverter.toLong(System.getProperty("easer.insights.jdbc.pool.max.connection.idle.ms"), TimeUnit.SECONDS.toMillis(10));

  private final String name;

  protected DbConnectionPool(final String name) {
    this.name = name;
  }

  public abstract void start();
  public abstract void stop();

  public String name() {
    return name;
  }

  // ====================================================================================================
  //  Factory Methods
  // ====================================================================================================
  public static DbConnectionPool newPerThreadPool(final String name) {
    return new DbPerThreadConnectionPool(name);
  }

  public static DbConnectionPool newPool(final String name) {
    return new DbGlobalConnectionPool(name);
  }

  public static DbConnectionPool newPool(final String name, final int maxPooledConnections) {
    return new DbGlobalConnectionPool(name, maxPooledConnections);
  }

  // ====================================================================================================
  //  PUBLIC get connection method
  // ====================================================================================================
  public DbConnection getConnection(final DbInfo dbInfo) throws DbConnectionException {
    final String requestedBy = LogUtil.lookupLineClassAndMethod(2);
    return getConnection(dbInfo, requestedBy);
  }

  public DbConnection getConnection(final DbInfo dbInfo, final String requestedBy) throws DbConnectionException {
    return DbConnectionProvider.INSTANCE.getConnection(this, dbInfo, requestedBy);
  }

  // ====================================================================================================
  // Internal Access Helpers
  // ====================================================================================================
  protected abstract DbConnection getRawConnection(DbInfo dbInfo);
  protected abstract boolean addToPool(Thread thread, DbConnection connection);

  // ====================================================================================================
  //  Internal Close helpers
  // ====================================================================================================
  protected void closePooledConnection(final DbConnection connection, final String reason, final boolean forceAll) {
    if (connection == null) return;

    if (TRACE_CONNECTION_POOL) {
      Logger.trace("removing connection {} from {pool}, {}{}", connection, this, reason, forceAll ? " (FORCE ALL)" : "");
    }
    connection.setPool(null);
    connection.close();
  }

  protected DbConnection verifyPooledConnection(final DbConnection conn) {
    if (conn == null) return null;

    if (conn.isClosed()) {
      closePooledConnection(conn, "the connection is closed", false);
      return null;
    }
    if (!conn.isValid()) {
      closePooledConnection(conn, "the connection is no longer valid", false);
      return null;
    }

    if (DbPerThreadConnectionPool.TRACE_CONNECTION_POOL) {
      Logger.trace("Returning connection {connectionId}, open since {}", conn.getId(), HumansUtil.humanTimeSince(conn.getOpenNs()));
    }
    return conn;
  }

  protected static abstract class AbstractDbConnectionPoolWithCleaner extends DbConnectionPool {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final long maxConnectionOpenNs;
    private final long maxConnectionIdleNs;

    private CleanerThread cleanerThread;

    protected AbstractDbConnectionPoolWithCleaner(final String name, final Duration maxConnectionOpen, final Duration maxConnectionIdle) {
      super(name);
      this.maxConnectionOpenNs = maxConnectionOpen.toNanos();
      this.maxConnectionIdleNs = maxConnectionIdle.toNanos();
    }

    // ====================================================================================================
    //  PUBLIC start/stop methods
    // ====================================================================================================
    public void start() {
      if (!running.compareAndSet(false, true)) {
        Logger.warn("jdbc connection pool {} already started", name());
        return;
      }

      Logger.info("starting jdbc connection pool {}", name());
      cleanerThread = new CleanerThread(name() + "-cleaner");
      cleanerThread.setDaemon(true);
      cleanerThread.start();
    }

    public void stop() {
      if (!running.compareAndSet(true, false)) {
        Logger.warn("jdbc connection pool {} already stopped", name());
        return;
      }

      if (cleanerThread != null) {
        cleanerThread.sendStopSignal();
        ThreadUtil.shutdown(cleanerThread, 30, TimeUnit.SECONDS);
        cleanerThread = null;
      }
    }

    @Override
    public boolean sendStopSignal() {
      if (cleanerThread != null) {
        cleanerThread.sendStopSignal();
      }
      return true;
    }

    @Override
    public void close() {
      stop();
    }

    // ====================================================================================================
    //  PRIVATE connection pool cleaner thread
    // ====================================================================================================
    protected abstract void cleanExpired();

    protected final boolean isExpired(final DbConnection conn, final long now) {
      return conn.isClosed() || isBeenIdleTooLong(conn, now) || isBeenOpenTooLong(conn, now);
    }

    protected final boolean isBeenIdleTooLong(final DbConnection conn, final long now) {
      return (now - conn.getLastUpdateNs()) >= maxConnectionIdleNs;
    }

    protected final boolean isBeenOpenTooLong(final DbConnection conn, final long now) {
      return (now - conn.getOpenNs()) >= maxConnectionOpenNs;
    }

    protected final void wakeCleaner() {
      cleanerThread.wake();
    }

    private final class CleanerThread extends SpinningThread {
      private static final MetricDimension<MaxAvgTimeRangeGauge> cleanTime = Metrics.newCollectorWithDimensions()
        .dimensions("name")
        .unit(DatumUnit.NANOSECONDS)
        .name("jdbc.pool.clean.time")
        .label("JDBC Pool Clean Time")
        .register(() -> MaxAvgTimeRangeGauge.newMultiThreaded(60, 1, TimeUnit.MINUTES));

      public CleanerThread(final String name) {
        super(name);
      }

      @Override
      protected void runLoop() {
        while (isRunning()) {
          waitFor(Math.max(maxConnectionIdleNs, maxConnectionOpenNs), TimeUnit.NANOSECONDS);
          process();
        }
        process();
      }

      @Override
      protected void process() {
        try (Span span = Tracer.newRootSpan()) {
          TraceAttributes.MODULE_NAME.set(span, "jdbc.pool");
          span.setName("JDBC " + name() + " Cleaner");

          final long startTime = System.nanoTime();
          cleanExpired();
          cleanTime.get(name()).sample(System.nanoTime() - startTime);
        }
      }
    }
  }
}
