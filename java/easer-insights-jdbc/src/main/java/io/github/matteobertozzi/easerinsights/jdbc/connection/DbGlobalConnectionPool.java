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
import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.connection.DbConnectionPool.AbstractDbConnectionPoolWithCleaner;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.strings.StringConverter;

public class DbGlobalConnectionPool extends AbstractDbConnectionPoolWithCleaner {
  private static final int MAX_POOLED_CONNECTIONS = StringConverter.toInt(System.getProperty("easer.insights.jdbc.pool.max.pooled.connections"), 16);

  private final GloabalPool pool;

  public DbGlobalConnectionPool(final String name) {
    this(name, MAX_POOLED_CONNECTIONS);
  }

  public DbGlobalConnectionPool(final String name, final int maxPooledConnections) {
    this(name, maxPooledConnections, Duration.ofMillis(MAX_CONNECTION_OPEN_MS), Duration.ofMillis(MAX_CONNECTION_IDLE_MS));
  }

  public DbGlobalConnectionPool(final String name, final int maxPooledConnections, final Duration maxConnectionOpen, final Duration maxConnectionIdle) {
    super(name, maxConnectionOpen, maxConnectionIdle);
    this.pool = new GloabalPool(maxPooledConnections);
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
    pool.cleanExpired(this);
  }

  // ====================================================================================================
  // PRIVATE Helpers
  // ====================================================================================================
  private static final class GloabalPool {
    private final ReentrantLock lock = new ReentrantLock();

    private final int[] dbInfoHashes;
    private final Object[] items; // dbInfo|dbConnection|...
    private int freeSlotIndex;
    private int size;

    private GloabalPool(final int size) {
      this.dbInfoHashes = new int[size];
      this.items = new Object[size << 1];
      freeSlotIndex = 0;
      for (int i = 0, n = dbInfoHashes.length - 1; i < n; ++i) {
        dbInfoHashes[i] = -(i + 1);
      }
      dbInfoHashes[dbInfoHashes.length - 1] = 0;
    }

    private boolean add(final DbConnection con) {
      final DbInfo dbInfo = con.getDbInfo();
      final int dbInfoHash = dbInfo.hashCode() & 0x7fffffff;
      lock.lock();
      try {
        if (freeSlotIndex < 0) {
          return false;
        }

        final int index = freeSlotIndex;
        final int itemIndex = index << 1;
        freeSlotIndex = -dbInfoHashes[index];
        dbInfoHashes[index] = dbInfoHash;
        items[itemIndex] = dbInfo;
        items[itemIndex + 1] = con;
        size++;
        return true;
      } finally {
        lock.unlock();
      }
    }

    private DbConnection poll(final DbInfo dbInfo) {
      final int dbInfoHash = dbInfo.hashCode() & 0x7fffffff;

      lock.lock();
      try {
        if (size == 0) {
          return null;
        }

        for (int i = 0; i < dbInfoHashes.length; ++i) {
          if (dbInfoHashes[i] != dbInfoHash) continue;

          final int index = (i << 1);
          final DbInfo indexDbInfo = (DbInfo)items[index];
          if (!dbInfo.equals(indexDbInfo)) continue;

          final DbConnection con = (DbConnection)items[index + 1];
          items[index] = null;
          items[index + 1] = null;
          dbInfoHashes[i] = -freeSlotIndex;
          freeSlotIndex = i;
          size--;
          return con;
        }
        return null;
      } finally {
        lock.unlock();
      }
    }

    private void cleanExpired(final DbGlobalConnectionPool pool) {
      final long now = System.nanoTime();
      lock.lock();
      try {
        for (int i = 0; i < dbInfoHashes.length; ++i) {
          final int index = (i << 1);
          final DbConnection con = (DbConnection)items[index + 1];
          if (con == null) continue;

          if (pool.isExpired(con, now)) {
            items[index] = null;
            items[index + 1] = null;
            dbInfoHashes[i] = freeSlotIndex;
            freeSlotIndex = i;
            size--;
          }
        }
      } finally {
        lock.unlock();
      }
    }
  }
}
