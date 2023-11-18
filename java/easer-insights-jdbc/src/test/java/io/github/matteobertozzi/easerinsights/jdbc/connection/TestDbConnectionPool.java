package io.github.matteobertozzi.easerinsights.jdbc.connection;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.DbType;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.easerinsights.logging.providers.TextLogProvider;

public class TestDbConnectionPool {
  static {
    Logger.setLogProvider(new TextLogProvider());
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

    try (final DbConnectionPool pool = new DbConnectionPool("TestPool")) {
      pool.start();

      for (int i = 0; i < 2; ++i) {
        for (final DbInfo dbInfo: dbInfos) {
          try (DbConnection con = pool.getConnection(dbInfo)) {
            System.out.println(con);
          }
        }
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
