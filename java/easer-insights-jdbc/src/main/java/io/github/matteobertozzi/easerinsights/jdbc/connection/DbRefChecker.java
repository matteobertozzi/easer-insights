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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.strings.StringConverter;

interface DbRefChecker {
  int SUSPICIOUS_OPEN_RESULT_SET_COUNT = StringConverter.toInt(System.getProperty("easer.insights.jdbc.suspicious.open.resultset.count"), 10);
  int SUSPICIOUS_OPEN_STATEMENT_COUNT = StringConverter.toInt(System.getProperty("easer.insights.jdbc.suspicious.open.statement.count"), 20);

  boolean ENABLE_REF_CHECKER = StringConverter.toBoolean(System.getProperty("easer.insights.jdbc.ref.checker.enabled"), false);
  Function<DbInfo, DbRefChecker> REF_CHECKER_FACTORY = ENABLE_REF_CHECKER ? SimpleRefChecker::get : NoOpRefChecker::get;

  static DbRefChecker get(final DbInfo dbInfo) {
    return REF_CHECKER_FACTORY.apply(dbInfo);
  }

  DbConnectionRefChecker addConnection(DbConnection connection);
  void closeConnection(DbConnection connection);

  interface DbConnectionRefChecker {
    void destroy();

    void addStatement(Statement statement);
    boolean removeStatement(Statement statement);

    void addResultSet(ResultSet rs);
    boolean removeResultSet(ResultSet rs);
  }

  // ==========================================================================================
  //  No-Op Ref Checker
  // ==========================================================================================
  final class NoOpRefChecker implements DbRefChecker {
    private static final NoOpRefChecker INSTANCE = new NoOpRefChecker();

    static DbRefChecker get(final DbInfo dbInfo) {
      return NoOpRefChecker.INSTANCE;
    }

    @Override
    public DbConnectionRefChecker addConnection(final DbConnection connection) {
      return NoOpConnectionRefChecker.INSTANCE;
    }

    @Override
    public void closeConnection(final DbConnection connection) {
      // no-op
    }
  }

  final class NoOpConnectionRefChecker implements DbConnectionRefChecker {
    private static final NoOpConnectionRefChecker INSTANCE = new NoOpConnectionRefChecker();

    @Override public void destroy() {}
    @Override public void addStatement(final Statement statement) {}
    @Override public boolean removeStatement(final Statement statement) { return true; }
    @Override public void addResultSet(final ResultSet rs) {}
    @Override public boolean removeResultSet(final ResultSet rs) { return true; }
  }

  // ==========================================================================================
  //  Simple Ref Checker
  // ==========================================================================================
  final class SimpleRefChecker implements DbRefChecker {
    private static final ConcurrentHashMap<DbInfo, DbRefChecker> refsMap = new ConcurrentHashMap<>(32);

    private final ConcurrentHashMap<DbConnection, DbConnectionRefChecker> connectionRefs = new ConcurrentHashMap<>(64);
    //private final Set<DbConnection> pooledRefs = ConcurrentHashMap.newKeySet(64);

    private SimpleRefChecker(final DbInfo dbInfo) {
      // no-op
    }

    static DbRefChecker get(final DbInfo dbInfo) {
      final DbRefChecker refChecker = refsMap.get(dbInfo);
      if (refChecker != null) return refChecker;
      return refsMap.computeIfAbsent(dbInfo, SimpleRefChecker::new);
    }

    @Override
    public DbConnectionRefChecker addConnection(final DbConnection connection) {
      //pooledRefs.remove(connection);

      final SimpleConnectionRefChecker refChecker = new SimpleConnectionRefChecker(connection);
      if (connectionRefs.put(connection, refChecker) != null) {
        Logger.warn("CONNECTION ALREADY REGISTERED: {}", connection);
      }
      return refChecker;
    }

    @Override
    public void closeConnection(final DbConnection connection) {
      final DbConnectionRefChecker refChecker = connectionRefs.remove(connection);
      if (refChecker != null) {
        refChecker.destroy();
      //} else if (!pooledRefs.remove(connection)) {
      } else {
        Logger.warn("CONNECTION ALREADY CLOSED: {}", connection);
      }

      //if (connection.hasPool()) {
        //pooledRefs.add(connection);
      //}
    }
  }

  final class SimpleConnectionRefChecker implements DbConnectionRefChecker {
    private final ConcurrentHashMap<Statement, String> statementRefs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ResultSet, String> resultSetRefs = new ConcurrentHashMap<>();
    private final String creationStackTrace;
    private long lastUpdate;

    private SimpleConnectionRefChecker(final DbConnection connection) {
      this.lastUpdate = System.currentTimeMillis();
      this.creationStackTrace = buildStackTraceForRef(connection);
    }

    @Override
    public void destroy() {
      if (statementRefs.isEmpty() && resultSetRefs.isEmpty()) {
        return;
      }

      Logger.error("connection closed with {} resultSet active and {} statements active", resultSetRefs.size(), statementRefs.size());
      for (final Entry<ResultSet, String> entry: resultSetRefs.entrySet()) {
        Logger.warn("RESULT-SET NOT CLOSED: {}", entry.getKey());
        DbConnection.closeQuietly(entry.getKey());
      }
      resultSetRefs.clear();

      for (final Entry<Statement, String> entry: statementRefs.entrySet()) {
        Logger.warn("STATEMENT NOT CLOSED: {}", entry.getKey());
        DbConnection.closeQuietly(entry.getKey());
      }
      statementRefs.clear();
    }

    @Override
    public void addStatement(final Statement statement) {
      lastUpdate = System.nanoTime();
      final String oldTrace = statementRefs.put(statement, buildStackTraceForRef(statement));
      if (oldTrace != null) {
        Logger.error(new IllegalStateException(), "STATEMENT REGISTERED TWICE: {}, oldTrace: {}", statement, oldTrace);
      }

      if (statementRefs.size() >= SUSPICIOUS_OPEN_STATEMENT_COUNT) {
        Logger.warn("suspicious open Statement {count}: {}", statementRefs.size(), String.join("-----\n", statementRefs.values()));
      }
    }

    @Override
    public boolean removeStatement(final Statement statement) {
      lastUpdate = System.nanoTime();
      if (statementRefs.remove(statement) == null) {
        Logger.error(new IllegalStateException(), "STATEMENT NOT REGISTERED: {}", statement);
        return false;
      }
      return true;
    }

    @Override
    public void addResultSet(final ResultSet rs) {
      lastUpdate = System.nanoTime();
      final String oldTrace = resultSetRefs.put(rs, buildStackTraceForRef(rs));
      if (oldTrace != null) {
        Logger.error(new IllegalStateException(), "RESULT-SET REGISTERED TWICE {}, oldTrace: {}", rs, oldTrace);
      }

      if (resultSetRefs.size() >= SUSPICIOUS_OPEN_RESULT_SET_COUNT) {
        Logger.warn("suspicious open ResultSet {count}", resultSetRefs.size(), String.join("-----\n", statementRefs.values()));
      }
    }

    @Override
    public boolean removeResultSet(final ResultSet rs) {
      lastUpdate = System.nanoTime();
      if (resultSetRefs.remove(rs) == null) {
        Logger.error(new IllegalStateException(), "RESULT-SET NOT REGISTERED: {}", rs);
        return false;
      }
      return true;
    }
  }

  private static String buildStackTraceForRef(final Object ref) {
    final StringBuilder buffer = new StringBuilder(512);
    buffer.append(Thread.currentThread()).append(" - ").append(ref).append(System.lineSeparator());
    StackWalker.getInstance().forEach(s -> buffer.append("  ").append(s).append(System.lineSeparator()));
    return buffer.toString();
  }
}
