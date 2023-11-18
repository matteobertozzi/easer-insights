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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.DbType;
import io.github.matteobertozzi.easerinsights.jdbc.connection.DbRefChecker.DbConnectionRefChecker;
import io.github.matteobertozzi.easerinsights.logging.Logger;

public final class DbConnection implements Closeable {
  private static final AtomicLong ID_GENERATOR = new AtomicLong();

  private final AtomicReference<DbConnectionPool> pool = new AtomicReference<>();
  private final AtomicReference<String> busyName = new AtomicReference<>();
  private final AtomicLong lastUpdateNs = new AtomicLong();
  private final long openNs;
  private final long id;

  private final DbConnectionRefChecker dbRefConChecker;
  private final DbRefChecker dbRefChecker;
  private final DbStats dbStats;

  private final Connection connection;
  private final DbInfo dbInfo;

  private long queryPerTxn;

  DbConnection(final DbInfo dbInfo, final DbStats stats, final Connection connection) {
    this.id = ID_GENERATOR.incrementAndGet();
    this.connection = connection;
    this.dbInfo = dbInfo;
    this.openNs = System.nanoTime();
    this.lastUpdateNs.set(this.openNs);

    this.dbStats = stats;
    this.dbRefChecker = DbRefChecker.get(dbInfo);
    this.dbRefConChecker = dbRefChecker.addConnection(this);
  }

  @Override
  public String toString() {
    return "DbConnection [id:" + id + ", name:" + busyName.get() + ", raw:" + connection + "]";
  }

  public DbInfo getDbInfo() {
    return dbInfo;
  }

  public DbType getDbType() {
    return dbInfo.type();
  }

  public Connection getRawConnection() {
    return connection;
  }

  public long getId() {
    return id;
  }

  @Override
  public void close() {
    final DbConnectionPool dbPool = this.pool.get();
    if (dbPool != null) {
      Logger.debug("close with pool: {}", this);
      // TODO: has uncommitted data? ROLLBACK
      try {
        if (!getAutoCommit()) {
          rollback();
        }
      } catch (final SQLException e) {
        rollback();
      }
      dbPool.addToPool(Thread.currentThread(), this);
    } else {
      Logger.debug("close direct no pool associated: {}", this);
      closeQuietly(connection);
    }

    resetQueryPerTransaction();
    busyName.set(null);
    dbRefChecker.closeConnection(this);
  }

  private void resetQueryPerTransaction() {
    this.queryPerTxn = 0;
  }

  // ====================================================================================================
  //  Connection state related
  // ====================================================================================================
  DbStats stats() {
    return dbStats;
  }

  public long getOpenNs() {
    return openNs;
  }

  public long getLastUpdateNs() {
    return lastUpdateNs.get();
  }

  public boolean isBusy() {
    return busyName.get() != null;
  }

  public void setPool(final DbConnectionPool newPool) {
    this.pool.set(newPool);
  }

  public void acquire(final String requestedBy) {
    this.lastUpdateNs.set(System.nanoTime());
    this.busyName.set(requestedBy);
  }

  public boolean isClosed() {
    try {
      return connection.isClosed();
    } catch (final SQLException e) {
      Logger.debug(e, "unable to verify the closed state of the Connection");

      // Don't reuse it for the pool
      this.pool.set(null);
      return true;
    }
  }

  public boolean isValid() {
    try {
      return connection.isValid(1);
    } catch (final SQLException e) {
      Logger.debug(e, "unable to verify the validity state of the connection");

      // Don't reuse it for the pool
      this.pool.set(null);
      return false;
    } catch (final Throwable e) {
      // probably method not supported
      Logger.debug(e, "unable to verify the validity state of the connection, use closed");
      return !isClosed();
    }
  }

  public DatabaseMetaData getMetaData() throws SQLException {
    try {
      return connection.getMetaData();
    } catch (final SQLException e) {
      // Don't reuse it for the pool
      this.pool.set(null);
      throw e;
    }
  }

  public String getCatalog() throws SQLException {
    try {
      return connection.getCatalog();
    } catch (final SQLException e) {
      // Don't reuse it for the pool
      this.pool.set(null);
      throw e;
    }
  }

  // ====================================================================================================
  // Metadata related
  // ====================================================================================================
  public void commit() throws SQLException {
    resetQueryPerTransaction();
    try {
      connection.commit();
    } catch (final SQLException e) {
      Logger.alert(busyName.get() + " unable to commit transaction");

      // Don't reuse it for the pool
      this.pool.set(null);

      throw e;
    }
  }

  public boolean rollback() {
    resetQueryPerTransaction();
    try {
      connection.rollback();
      return true;
    } catch (final SQLException e) {
      Logger.alert(busyName.get() + " unable to rollback transaction");

      // Don't reuse it for the pool
      this.pool.set(null);

      // TODO: Close connection
      return false;
    }
  }

  public boolean rollback(final Savepoint savepoint) {
    resetQueryPerTransaction();

    try {
      connection.rollback(savepoint);
      return true;
    } catch (final SQLException e) {
      Logger.alert("{} unable to rollback transaction, savepoint={}", busyName.get(), savepoint);

      // Don't reuse it for the pool
      this.pool.set(null);

      // TODO: Close connection
      return false;
    }
  }

  public boolean getAutoCommit() throws SQLException {
    return connection.getAutoCommit();
  }

  public boolean setAutoCommit(final boolean value) {
    try {
      connection.setAutoCommit(value);
      return true;
    } catch (final SQLException e) {
      Logger.alert(busyName.get() + " unable to set auto-commit to " + value);

      // Don't reuse it for the pool
      this.pool.set(null);
      return false;
    }
  }

  // ====================================================================================================
  // SavePoint helpers
  // ====================================================================================================
  public Savepoint setSavepoint() throws SQLException {
    return this.connection.setSavepoint();
  }

  public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
    try {
      this.connection.releaseSavepoint(savepoint);
    } catch (final SQLFeatureNotSupportedException e) {
      Logger.trace("release savepoint not supported for {}: {}", getDbType(), e.getMessage());
    } catch (final Exception e) {
      switch (getDbType()) {
        case SQLSERVER, ORACLE, SQLITE: {
          // no need for sql server to release the savepoint
          Logger.debug("unable to release savepoint for {}: {}", getDbType(), e.getMessage());
          break;
        } default: {
          Logger.error(e, "unable to release savepoint");
          throw e;
        }
      }
    }
  }

  // ====================================================================================================
  // New Statement related
  // ====================================================================================================
  public PreparedStatement prepareStatement(final String sql) throws SQLException {
    try {
      final PreparedStatement stmt = this.connection.prepareStatement(sql);
      queryPerTxn++;
      return new DbPreparedStatement(dbRefConChecker, dbStats, stmt, sql);
    } catch (final SQLException e) {
      // Don't reuse it for the pool
      this.pool.set(null);
      throw e;
    }
  }

  public PreparedStatement prepareStatementWithGeneratedKeys(final String sql) throws SQLException {
    try {
      final PreparedStatement stmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      queryPerTxn++;
      return new DbPreparedStatement(dbRefConChecker, dbStats, stmt, sql);
    } catch (final SQLException e) {
      // Don't reuse it for the pool
      this.pool.set(null);
      throw e;
    }
  }

  public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
    try {
      final PreparedStatement stmt = this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
      queryPerTxn++;
      return new DbPreparedStatement(dbRefConChecker, dbStats, stmt, sql);
    } catch (final SQLException e) {
      // Don't reuse it for the pool
      this.pool.set(null);
      throw e;
    }
  }

  public PreparedStatement prepareStreamingStatement(final String tableName, final String sql) throws SQLException {
    return prepareStreamingStatement(tableName, 1000, sql);
  }

  public PreparedStatement prepareStreamingStatement(final String tableName, final int fetchSize, final String sql) throws SQLException {
    final PreparedStatement stmt = prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    try {
      if (this.getDbType() == DbType.MYSQL) {
        // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-implementation-notes.html
        // The combination of a forward-only, read-only result set, with a fetch size of
        // Integer.MIN_VALUE
        // serves as a signal to the driver to stream result sets row-by-row. After this, any result
        // sets
        // created with the statement will be retrieved row-by-row.
        //
        // There are some caveats with this approach. You must read all of the rows in the result
        // set (or close it)
        // before you can issue any other queries on the connection, or an exception will be thrown.
        stmt.setFetchSize(Integer.MIN_VALUE);
      } else {
        try {
          stmt.setFetchSize(fetchSize);
        } catch (final SQLException e) {
          Logger.alert(e, "Unable to set {} fetch size value for {}", getDbType(), tableName);
        }
      }
      return stmt;
    } catch (final SQLException e) {
      // Don't reuse it for the pool
      this.pool.set(null);
      closeQuietly(stmt);
      throw e;
    }
  }

  public CallableStatement prepareCall(final String sql) throws SQLException {
    try {
      final CallableStatement stmt = this.connection.prepareCall(sql);
      queryPerTxn++;
      return new DbCallableStatement(dbRefConChecker, dbStats, stmt, sql);
    } catch (final SQLException e) {
      // Don't reuse it for the pool
      this.pool.set(null);
      throw e;
    }
  }


  // ====================================================================================================
  //  DbConnection statements, resultsets
  // ====================================================================================================
  private static final class DbCallableStatement extends FilterCallableStatement {
    private final DbConnectionRefChecker refChecker;
    private final DbStats dbStats;
    private final String sql;

    public DbCallableStatement(final DbConnectionRefChecker refChecker, final DbStats dbStats,
        final CallableStatement stmt, final String sql) {
      super(stmt);
      this.refChecker = refChecker;
      this.dbStats = dbStats;
      this.sql = sql;
      refChecker.addStatement(this);
    }

    @Override
    public void close() throws SQLException {
      refChecker.removeStatement(this);
      super.close();
    }

    @Override
    public String toString() {
      return "DbCallableStatement [" + Integer.toHexString(System.identityHashCode(this)) + ", sql=" + sql + "]";
    }
  }

  private static final class DbPreparedStatement extends FilterPreparedStatement {
    private final DbConnectionRefChecker refChecker;
    private final DbStats dbStats;
    private final String sql;

    public DbPreparedStatement(final DbConnectionRefChecker refChecker, final DbStats dbStats,
        final PreparedStatement stmt, final String sql) {
      super(stmt);
      this.refChecker = refChecker;
      this.dbStats = dbStats;
      this.sql = sql;
      refChecker.addStatement(this);
    }

    @Override
    public void close() throws SQLException {
      refChecker.removeStatement(this);
      super.close();
    }

    public ResultSet getResultSet() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
      final long startTime = System.nanoTime();
      try {
        final ResultSet rs = super.getGeneratedKeys();
        return new DbResultSet(refChecker, dbStats, rs, "autogenKeys: " + sql, startTime);
      } catch (final SQLException e) {
        dbStats.addExecuteQueryFailure("autogenKeys: " + sql, System.nanoTime() - startTime);
        throw e;
      }
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
      final long startTime = System.nanoTime();
      try {
        final ResultSet rs = super.executeQuery();
        return new DbResultSet(refChecker, dbStats, rs, sql, startTime);
      } catch (final SQLException e) {
        dbStats.addExecuteQueryFailure(sql, System.nanoTime() - startTime);
        throw e;
      }
    }

    @Override
    public int executeUpdate() throws SQLException {
      final long startTime = System.nanoTime();
      try {
        final int r = super.executeUpdate();
        dbStats.addExecuteUpdate(sql, System.nanoTime() - startTime);
        return r;
      } catch (final SQLException e) {
        dbStats.addExecuteUpdateFailure(sql, System.nanoTime() - startTime);
        throw e;
      }
    }

    @Override
    public int[] executeBatch() throws SQLException {
      final long startTime = System.nanoTime();
      try {
        final int[] r = super.executeBatch();
        dbStats.addExecuteUpdate(sql, System.nanoTime() - startTime);
        return r;
      } catch (final SQLException e) {
        dbStats.addExecuteUpdateFailure(sql, System.nanoTime() - startTime);
        throw e;
      }
    }

    @Override
    public String toString() {
      return "DbPreparedStatement [" + Integer.toHexString(System.identityHashCode(this)) + ", sql=" + sql + "]";
    }
  }

  private static final class DbResultSet extends FilterResultSet {
    private final DbConnectionRefChecker refChecker;
    private final DbStats dbStats;
    private final String sql;

    private final long startTime;
    private long rowCount;

    public DbResultSet(final DbConnectionRefChecker refChecker, final DbStats dbStats,
      final ResultSet rs, final String sql, final long startTime) {
      super(rs);
      this.refChecker = refChecker;
      this.dbStats = dbStats;
      this.sql = sql;
      this.startTime = startTime;
      this.rowCount = 0;
      refChecker.addResultSet(this);
    }

    @Override
    public void close() throws SQLException {
      if (refChecker.removeResultSet(this)) {
        dbStats.addExecuteQuery(sql, System.nanoTime() - startTime, rowCount);
      }
      super.close();
    }

    @Override
    public boolean next() throws SQLException {
      final boolean hasMore = super.next();
      rowCount += hasMore ? 1 : 0;
      return hasMore;
    }

    @Override
    public String toString() {
      return "DbResultSet [" + Integer.toHexString(System.identityHashCode(this)) + ", sql=" + sql + "]";
    }
  }

  // ====================================================================================================
  // Raw close() helpers
  // ====================================================================================================
  static void closeQuietly(final Connection connection) {
    if (connection == null) return;
    try {
      connection.close();
    } catch (final SQLException e) {
      Logger.error(e, "unable to close the Connection {}: {}", e.getSQLState(), connection);
    }
  }

  public static void closeQuietly(final Statement stmt) {
    if (stmt == null) return;
    try {
      if (!(stmt instanceof DbPreparedStatement)) {
        Logger.warn("closing unmanaged Statement {}", stmt);
      }
      stmt.close();
    } catch (final SQLException e) {
      Logger.error(e, "unable to close the Statement");
    }
  }

  public static void closeQuietly(final ResultSet rs) {
    if (rs == null) return;
    try {
      if (!(rs instanceof DbResultSet)) {
        Logger.warn("closing unmanaged ResultSet {}", rs);
      }
      rs.close();
    } catch (final SQLException e) {
      Logger.error(e, "unable to close the ResultSet");
    }
  }
}
