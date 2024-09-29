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

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;
import io.github.matteobertozzi.rednaco.strings.StringConverter;

public final class DbConnectionProvider {
  public static final DbConnectionProvider INSTANCE = new DbConnectionProvider();

  private final ReentrantLock jdbcGlobalJdbcInitLock = new ReentrantLock(true);
  private final Set<String> jdbcDriversLoaded = ConcurrentHashMap.newKeySet();

  private DbConnectionProvider() {
    // no-op
  }

  // ====================================================================================================
  // PUBLIC get connection method
  // ====================================================================================================
  public DbConnection getConnection(final DbInfo dbInfo, final String requestedBy) throws DbConnectionException {
    return getConnection(null, dbInfo, requestedBy);
  }

  public DbConnection getConnection(final DbConnectionPool pool, final DbInfo dbInfo, final String requestedBy) throws DbConnectionException {
    DbConnection connection = fetchConnectionFromPool(pool, dbInfo);
    if (connection == null) {
      Logger.debug("creating new connection for {thread} {}", Thread.currentThread().getName(), requestedBy);
      connection = createNewConnection(dbInfo);
      if (connection == null) return null;
      connection.setPool(pool);
    }

    // acquire the connection (set busy name = requester)
    connection.acquire(requestedBy);
    return connection;
  }

  private void loadJdbcDriver(final String driverName) throws DbConnectionException {
    // The driver is already loaded, nothing to do.
    if (driverName == null || jdbcDriversLoaded.contains(driverName)) return;

    // NOTE: concurrent connection must wait the jdbc driver to be loaded/initialized.
    // Since this should be rare, we take a global JDBC lock rather than a per-driver lock.
    try {
      final long startTime = System.nanoTime();
      jdbcGlobalJdbcInitLock.lock();
      try {
        if (!jdbcDriversLoaded.contains(driverName)) {
          final Constructor<?> ctor = Class.forName(driverName).getConstructor();
          jdbcDriversLoaded.add(driverName);
          Logger.debug("loaded {}: {}", driverName, ctor);
        }
      } finally {
        jdbcGlobalJdbcInitLock.unlock();
      }
      final long elapsed = System.nanoTime() - startTime;
      Logger.debug("jdbc driver {} loaded in {}", driverName, HumansUtil.humanTimeNanos(elapsed));
    } catch (final Throwable e) {
      throw new DbConnectionException(e, "Unable to initialize the JDBC driver " + driverName + ": " + e.getMessage());
    }
  }

  // ====================================================================================================
  // Connection helpers
  // ====================================================================================================
  public static final int DEFAULT_LOGIN_TIMEOUT_SEC = StringConverter.toInt(System.getProperty("easer.insights.jdbc.login.timeout.sec"), 5);
  public static final int MAX_CONNECTION_TIMEOUT_SEC = StringConverter.toInt(System.getProperty("easer.insights.jdbc.max.connections.timeout.sec"), 10);

  private static final ConcurrentHashMap<String, Semaphore> connectionLimiter = new ConcurrentHashMap<>();
  private static boolean tryAcquireConnectionPermission(final DbInfo dbInfo) {
    final int maxConnections = dbInfo.maxConnections();
    if (maxConnections <= 0) return true;

    final Semaphore semaphore = connectionLimiter.computeIfAbsent(dbInfo.internalGroupId(), k -> new Semaphore(maxConnections));
    try {
      return semaphore.tryAcquire(MAX_CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      Thread.interrupted();
      return false;
    }
  }

  private static void releaseConnectionPermission(final DbInfo dbInfo) {
    final int maxConnections = dbInfo.maxConnections();
    if (maxConnections <= 0) return;

    connectionLimiter.get(dbInfo.internalGroupId()).release();
  }

  private DbConnection createNewConnection(final DbInfo dbInfo) throws DbConnectionException {
    final long startTime = System.nanoTime();

    // Ensure that the driver is loaded
    loadJdbcDriver(dbInfo.driver());

    Connection connection = null;
    try {
      if (!tryAcquireConnectionPermission(dbInfo)) {
        throw new DbConnectionException("Unable to connect to server DB " + dbInfo.url() + ". too many connection open (service limit reached)");
      }

      DriverManager.setLoginTimeout(DEFAULT_LOGIN_TIMEOUT_SEC);
      connection = DriverManager.getConnection(dbInfo.url(), dbInfo.properties());
      if (connection == null) {
        releaseConnectionPermission(dbInfo);
        throw new DbConnectionException("unable to create db connection to " + dbInfo);
      }

      final long elapsedTime = System.nanoTime() - startTime;
      final DbStats dbStats = DbStats.get(dbInfo);
      dbStats.addConnectionTime(elapsedTime);
      dbStats.incOpenConnections();

      // Initialize the connection
      try {
        connection.setTransactionIsolation(dbInfo.getTransactionIsolation());
      } catch (final SQLFeatureNotSupportedException e) {
        Logger.warn("feature not supported for {} -> {}", dbInfo.type(), e.getMessage());
      }
      connection.setAutoCommit(true);
      return new DbConnection(dbInfo, dbStats, connection);
    } catch (final SQLException e) {
      final DbStats stats = DbStats.get(dbInfo);
      closeQuietly(dbInfo, stats, connection);
      stats.addConnectionFailure(System.nanoTime() - startTime);

      Logger.error("unable to create a new db-connection: {} {}", e.getErrorCode(), e.getMessage());
      throw new DbConnectionException(e, "Unable to connect to server DB : " + dbInfo.url());
    }
  }

  static void closeQuietly(final DbInfo dbInfo, final DbStats stats, final Connection connection) {
    if (connection == null) return;
    try {
      releaseConnectionPermission(dbInfo);
      stats.decOpenConnections();
      connection.close();
    } catch (final SQLException e) {
      Logger.error(e, "unable to close the Connection {}: {}", e.getSQLState(), connection);
    }
  }

  private DbConnection fetchConnectionFromPool(final DbConnectionPool pool, final DbInfo dbInfo) {
    if (pool == null) return null;

    final long startTime = System.nanoTime();
    final DbConnection connection = pool.getRawConnection(dbInfo);
    final long elapsedTime = System.nanoTime() - startTime;
    final DbStats stats = connection != null ? connection.stats() : DbStats.get(dbInfo);
    stats.addPoolConnectionTime(elapsedTime);

    return connection;
  }
}
