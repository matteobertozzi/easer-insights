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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.logging.LogUtil;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;
import io.github.matteobertozzi.rednaco.strings.StringConverter;
import io.github.matteobertozzi.rednaco.threading.ShutdownUtil.StopSignal;
import io.github.matteobertozzi.rednaco.threading.SpinningThread;
import io.github.matteobertozzi.rednaco.threading.ThreadUtil;

public class DbConnectionPool implements StopSignal, Closeable {
  private static final boolean TRACE_CONNECTION_POOL = StringConverter.toBoolean(System.getProperty("easer.insights.jdbc.pool.trace.connections"), false);
  private static final long MAX_CONNECTION_OPEN_MS = StringConverter.toLong(System.getProperty("easer.insights.jdbc.pool.max.connection.open.ms"), TimeUnit.MINUTES.toMillis(10));
  private static final long MAX_CONNECTION_IDLE_MS = StringConverter.toLong(System.getProperty("easer.insights.jdbc.pool.max.connection.idle.ms"), TimeUnit.SECONDS.toMillis(10));
  private static final int MAX_CONNECTIONS_PER_THREAD = StringConverter.toInt(System.getProperty("easer.insights.jdbc.pool.max.connections.per.thread"), 6);

  private final ConcurrentHashMap<Thread, ConnectionPool> pool = new ConcurrentHashMap<>();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final String name;

  private final long maxConnectionOpenNs;
  private final long maxConnectionIdleNs;
  private final int maxConnectionsPerThread;

  private CleanerThread cleanerThread;

  public DbConnectionPool(final String name) {
    this(name, MAX_CONNECTIONS_PER_THREAD);
  }

  public DbConnectionPool(final String name, final int maxConnectionsPerThread) {
    this(name, maxConnectionsPerThread, Duration.ofMillis(MAX_CONNECTION_OPEN_MS), Duration.ofMillis(MAX_CONNECTION_IDLE_MS));
  }

  public DbConnectionPool(final String name, final int maxConnectionsPerThread, final Duration maxConnectionOpen, final Duration maxConnectionIdle) {
    this.name = name;
    this.maxConnectionOpenNs = maxConnectionOpen.toNanos();
    this.maxConnectionIdleNs = maxConnectionIdle.toNanos();
    this.maxConnectionsPerThread = maxConnectionsPerThread;
  }

  // ====================================================================================================
  //  PUBLIC get connection method
  // ====================================================================================================
  public void start() {
    if (!running.compareAndSet(false, true)) {
      Logger.warn("jdbc connection pool {} already started", name);
      return;
    }

    Logger.info("starting jdbc connection pool {}", name);
    cleanerThread = new CleanerThread(name + "-cleaner");
    cleanerThread.setDaemon(true);
    cleanerThread.start();
  }

  public void stop() {
    if (!running.compareAndSet(true, false)) {
      Logger.warn("jdbc connection pool {} already stopped", name);
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
  //  PUBLIC get connection method
  // ====================================================================================================
  public DbConnection getConnection(final DbInfo dbInfo) throws DbConnectionException {
    final String requestedBy = LogUtil.lookupLineClassAndMethod(2);
    return getConnection(dbInfo, requestedBy);
  }

  public DbConnection getConnection(final DbInfo dbInfo, final String requestedBy) throws DbConnectionException {
    return DbConnectionProvider.INSTANCE.getConnection(this, dbInfo, requestedBy);
  }

  protected DbConnection getRawConnection(final DbInfo dbInfo) {
    return getFromPool(Thread.currentThread(), dbInfo);
  }

  // ====================================================================================================
  // PRIVATE Helpers
  // ====================================================================================================
  private DbConnection getFromPool(final Thread thread, final DbInfo dbInfo) {
    final ConnectionPool localPool = pool.get(thread);
    return (localPool != null) ? localPool.get(this, dbInfo) : null;
  }

  protected void addToPool(final Thread thread, final DbConnection connection) {
    addToPool(thread, connection, false);
  }

  private void addToPool(final Thread thread, final DbConnection connection, final boolean register) {
    if (connection.isClosed() || !connection.isValid()) {
      Logger.debug("trying to add a closed connection to the pool: {}", connection);
      connection.setPool(null);
      connection.close();
      return;
    }

    ConnectionPool localPool = pool.get(thread);
    if (localPool == null) {
      localPool = pool.computeIfAbsent(thread, k -> new ConnectionPool());
      Logger.trace("adding a new thread to the connection pool: {}", thread);
    }

    connection.setPool(this);
    if (!register) {
      // in-use connections are not in the pool.
      localPool.add(this, connection);
    }
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
    private final HashMap<DbInfo, DbConnection> connections = new HashMap<>();
    private boolean isAlive = true;

    public void add(final DbConnectionPool pool, final DbConnection connection) {
      lock.lock();
      try {
        if (isAlive) {
          if (connections.size() > pool.maxConnectionsPerThread) {
            closeConnection(pool, connection, "the thread has already too many connections", false);
          } else if ((System.nanoTime() - connection.getOpenNs()) > pool.maxConnectionOpenNs) {
            closeConnection(pool, connection, "the connection was open for too long: " + HumansUtil.humanTimeSince(connection.getOpenNs()), false);
          } else {
            final DbConnection oldConnection = this.connections.put(connection.getDbInfo(), connection);
            closeConnection(pool, oldConnection, "a newer connection was added", false);
          }
        } else {
          closeConnection(pool, connection, "the thread is market as closed", false);
        }
      } finally {
        lock.unlock();
      }
    }

    public DbConnection get(final DbConnectionPool pool, final DbInfo dbInfo) {
      lock.lock();
      try {
        final DbConnection conn = connections.remove(dbInfo);
        if (conn == null) return null;

        final boolean isClosed = conn.isClosed();
        final boolean isValid = conn.isValid();
        if (isClosed || !isValid) {
          closeConnection(pool, conn, "the connection is closed or no longer valid closed=" + isClosed + " valid=" + isValid, false);
          return null;
        }

        if (DbConnectionPool.TRACE_CONNECTION_POOL) {
          Logger.trace("Returning {connectionId} connection {isClosed} {isValid} open since {}", conn.getId(), isClosed, isValid, HumansUtil.humanTimeSince(conn.getOpenNs()));
        }
        return conn;
      } finally {
        lock.unlock();
      }
    }

    public boolean cleanup(final DbConnectionPool pool, final boolean forceAll) {
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
            } else if (isExpired(pool, entry.getValue(), now)) {
              Logger.warn("connection is expired but it's still busy");
            }
            continue;
          }

          if (!forceAll && !isExpired(pool, entry.getValue(), now)) {
            continue;
          }

          closeConnection(pool, entry.getValue(), "cleaning up", forceAll);
          it.remove();
        }
        return connections.isEmpty();
      } finally {
        lock.unlock();
      }
    }

    private void closeConnection(final DbConnectionPool pool, final DbConnection connection, final String reason, final boolean forceAll) {
      if (connection == null) return;

      if (DbConnectionPool.TRACE_CONNECTION_POOL) {
        Logger.trace("removing connection {} from pool, {}{}", connection, reason, forceAll ? " (FORCE ALL)" : "");
      }
      connection.setPool(null);
      connection.close();
    }

    private static boolean isExpired(final DbConnectionPool pool, final DbConnection conn, final long now) {
      return conn.isClosed() || ((now - conn.getLastUpdateNs()) >= pool.maxConnectionIdleNs) || ((now - conn.getOpenNs()) >= pool.maxConnectionOpenNs);
    }
  }

  private final class CleanerThread extends SpinningThread {
    public CleanerThread(final String name) {
      super(name);
    }

    @Override
    protected void runLoop() {
      while (isRunning()) {
        if (!waitFor(Math.max(maxConnectionIdleNs, maxConnectionOpenNs), TimeUnit.NANOSECONDS)) {
          continue;
        }
        process();
      }
      process();
    }

    @Override
    protected void process() {
      final Iterator<Entry<Thread, ConnectionPool>> it = pool.entrySet().iterator();
      while (it.hasNext()) {
        final Entry<Thread, ConnectionPool> entry = it.next();
        final boolean isAlive = entry.getKey().isAlive();
        final boolean isEmpty = entry.getValue().cleanup(DbConnectionPool.this, !isAlive);
        if (!isAlive && isEmpty) {
          it.remove();
        }
      }
    }
  }
}
