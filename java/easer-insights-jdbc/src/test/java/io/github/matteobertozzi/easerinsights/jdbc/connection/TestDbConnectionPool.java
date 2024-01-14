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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.DbType;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.logging.providers.TextLogProvider;
import io.github.matteobertozzi.easerinsights.metrics.MetricsRegistry;
import io.github.matteobertozzi.rednaco.threading.ThreadUtil;

public class TestDbConnectionPool {
  static {
    Logger.setLogProvider(TextLogProvider.newStreamProvider(System.out));
  }
  @Test
  public void testConnectionPool() throws Exception {
    final DbInfo[] dbInfos = new DbInfo[] {
      newDbInfo("host-1", "db-1a"),
      newDbInfo("host-1", "db-1b"),
      newDbInfo("host-2", "db-2a"),
      newDbInfo("host-2", "db-2b"),
      newDbInfo("host-2", "db-2c"),
      newDbInfo("host-3", "db-3a"),
      newDbInfo("host-3", "db-3b"),
      newDbInfo("host-3", "db-3c"),
    };

    //try (final DbConnectionPool pool = new DbGlobalConnectionPool("TestPool", 4, Duration.ofMillis(200), Duration.ofMillis(500))) {
    try (final DbConnectionPool pool = DbConnectionPool.newPerThreadPool("TestPool")) {
      pool.start();

      for (int i = 0; i < 5; ++i) {
        for (final DbInfo dbInfo: dbInfos) {
          try (DbConnection con = pool.getConnection(dbInfo)) {
            System.out.println("OPEN " + con);
            System.out.println(MetricsRegistry.INSTANCE.humanReport("jdbc.connection.active"));
            System.out.println("CLOSE " + con);
          }
        }
        ThreadUtil.sleep(250);
      }
    }
  }

  private static DbInfo newDbInfo(final String host, final String dbName) {
    return new DbInfo(DbType.UNKNOWN, host + "/" + dbName, host, dbName, TestDriver.class.getName(), new Properties());
  }

  public static class TestDriver implements Driver {
    private static final TestDriver INSTANCE = new TestDriver();
    private static final AtomicLong connectionIds = new AtomicLong();

    static {
      try {
        DriverManager.registerDriver(INSTANCE);
      } catch (final SQLException e) {
        System.err.println("unable to register test driver");
      }
    }

    public TestDriver() {
      // no-op
    }

    @Override
    public Connection connect(final String url, final Properties info) {
      return new NoOpConnection(connectionIds.incrementAndGet());
    }

    @Override public boolean acceptsURL(final String url) { return true; }
    @Override public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) { return null; }

    @Override public int getMajorVersion() { return 1; }
    @Override public int getMinorVersion() { return 0; }
    @Override public boolean jdbcCompliant() { return true; }
    @Override public java.util.logging.Logger getParentLogger() { return null; }
  }
}
