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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.connection.DbConnectionPool.AbstractDbConnectionPoolWithCleaner;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.metrics.MetricDimension;
import io.github.matteobertozzi.easerinsights.metrics.Metrics;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeDrag;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;
import io.github.matteobertozzi.rednaco.strings.StringConverter;

// NOTE: This is a per-thread connection pool
// use it when you have a controlled fixed number of threads,
// each thread will hold a connection per DbInfo.
public class DbPerThreadConnectionPool extends AbstractDbConnectionPoolWithCleaner {
  private static final int MAX_CONNECTIONS_PER_THREAD = StringConverter.toInt(System.getProperty("easer.insights.jdbc.pool.max.connections.per.thread"), 6);
  private static final int MAX_POOL_THREADS = StringConverter.toInt(System.getProperty("easer.insights.jdbc.pool.max.threads"), 32);

  private final ConcurrentHashMap<Thread, ConnectionPool> pool;
  private final int maxConnectionsPerThread;
  private final int maxPoolThreads;

  public DbPerThreadConnectionPool(final String name) {
    this(name, MAX_POOL_THREADS, MAX_CONNECTIONS_PER_THREAD);
  }

  public DbPerThreadConnectionPool(final String name, final int maxPoolThreads, final int maxConnectionsPerThread) {
    this(name, maxPoolThreads, maxConnectionsPerThread, Duration.ofMillis(MAX_CONNECTION_OPEN_MS), Duration.ofMillis(MAX_CONNECTION_IDLE_MS));
  }

  public DbPerThreadConnectionPool(final String name, final int maxPoolThreads, final int maxConnectionsPerThread, final Duration maxConnectionOpen, final Duration maxConnectionIdle) {
    super(name, maxConnectionOpen, maxConnectionIdle);
    this.pool = new ConcurrentHashMap<>(maxPoolThreads * 2);
    this.maxConnectionsPerThread = maxConnectionsPerThread;
    this.maxPoolThreads = maxPoolThreads;
  }

  // ====================================================================================================
  // Internal Access Helpers
  // ====================================================================================================
  protected DbConnection getRawConnection(final DbInfo dbInfo) {
    return getFromPool(Thread.currentThread(), dbInfo);
  }

  @Override
  protected boolean addToPool(final Thread thread, final DbConnection connection) {
    if (connection.isClosed() || !connection.isValid()) {
      Logger.debug("trying to add a closed connection to the pool: {}", connection);
      connection.setPool(null);
      return false;
    }

    ConnectionPool localPool = pool.get(thread);
    if (localPool == null) {
      if (pool.size() >= maxPoolThreads) {
        wakeCleaner();
        Logger.debug("too many threads in the pool, closing the connection: {} {}", thread, connection);
        connection.setPool(null);
        return false;
      }

      localPool = pool.computeIfAbsent(thread, this::newConnectionPool);
      Logger.trace("adding a new thread to the connection pool: {}", thread);
    }

    // in-use connections are not in the pool.
    return localPool.add(this, connection);
  }

  // ====================================================================================================
  // PRIVATE Helpers
  // ====================================================================================================
  private DbConnection getFromPool(final Thread thread, final DbInfo dbInfo) {
    final ConnectionPool localPool = pool.get(thread);
    return (localPool != null) ? localPool.get(this, dbInfo) : null;
  }

  private ConnectionPool newConnectionPool(final Thread thread) {
    return new ConnectionPool(this);
  }

  // ====================================================================================================
  //  Internal pool cleaner
  // ====================================================================================================
  private static final MetricDimension<TimeRangeDrag> activeThreads = Metrics.newCollectorWithDimensions()
    .dimensions("name")
    .unit(DatumUnit.COUNT)
    .name("jdbc.pool.active.threads")
    .label("JDBC Pool Active Threads")
    .register(() -> TimeRangeDrag.newMultiThreaded(60, 1, TimeUnit.MINUTES));

  @Override
  protected void cleanExpired() {
    final Iterator<Entry<Thread, ConnectionPool>> it = pool.entrySet().iterator();
    while (it.hasNext()) {
      final Entry<Thread, ConnectionPool> entry = it.next();
      final boolean isAlive = entry.getKey().isAlive();
      final boolean isEmpty = entry.getValue().cleanup(this, !isAlive);
      if (!isAlive && isEmpty) {
        it.remove();
      }
    }
    activeThreads.get(name()).set(pool.size());
  }

  // ====================================================================================================
  //  PRIVATE connection pool
  // ====================================================================================================
  private static final class ConnectionPool {
    // NOTE: We can probably use the ConcurrentHashMap without synchronization
    // but since this is a thread-local, and the monitor does not run that often
    // let's make everything synchronized for safety.
    // (The cleanup thread is the only concurrent/bad thing around here,
    // we can probably remove it with a per-thread cleaner on the event-loop)
    private final ReentrantLock lock = new ReentrantLock(true);
    private final HashMap<DbInfo, DbConnection> connections;
    private boolean isAlive = true;

    private ConnectionPool(final DbPerThreadConnectionPool pool) {
      this.connections = HashMap.newHashMap(pool.maxConnectionsPerThread);
    }

    public boolean add(final DbPerThreadConnectionPool pool, final DbConnection connection) {
      lock.lock();
      try {
        if (isAlive) {
          if (connections.size() > pool.maxConnectionsPerThread) {
            Logger.trace("the {pool} thread has already too many connections, closing: {}", pool, connection);
          } else if (pool.isBeenOpenTooLong(connection, System.nanoTime())) {
            Logger.trace("the connection was open for too long {}: {}", HumansUtil.humanTimeSince(connection.getOpenNs()), connection);
          } else {
            // add the connection to the pool
            connection.setPool(pool);
            final DbConnection oldConnection = this.connections.put(connection.getDbInfo(), connection);
            pool.closePooledConnection(oldConnection, "a newer connection was added", false);
            return true;
          }

          // close the connection
          connection.setPool(null);
          return false;
        } else {
          Logger.trace("the {pool} is market as closed, closing: {}", pool, connection);
          return false;
        }
      } finally {
        lock.unlock();
      }
    }

    public DbConnection get(final DbPerThreadConnectionPool pool, final DbInfo dbInfo) {
      lock.lock();
      try {
        final DbConnection conn = connections.remove(dbInfo);
        return pool.verifyPooledConnection(conn);
      } finally {
        lock.unlock();
      }
    }

    public boolean cleanup(final DbPerThreadConnectionPool pool, final boolean forceAll) {
      lock.lock();
      try {
        this.isAlive = !forceAll;

        // no-allocation path for empty pool
        if (connections.isEmpty()) return true;

        // verify expiration time for each connection
        final long now = System.nanoTime();
        final Iterator<Entry<DbInfo, DbConnection>> it = connections.entrySet().iterator();
        while (it.hasNext()) {
          final Entry<DbInfo, DbConnection> entry = it.next();
          if (entry.getValue().isBusy()) {
            if (forceAll) {
              Logger.alert("connection is busy, but should be force-closed: {}", entry.getValue());
            } else if (pool.isExpired(entry.getValue(), now)) {
              Logger.warn("connection is expired but it's still busy");
            }
            continue;
          }

          if (!forceAll && !pool.isExpired(entry.getValue(), now)) {
            continue;
          }

          pool.closePooledConnection(entry.getValue(), "cleaning up", forceAll);
          it.remove();
        }
        return connections.isEmpty();
      } finally {
        lock.unlock();
      }
    }
  }
}
