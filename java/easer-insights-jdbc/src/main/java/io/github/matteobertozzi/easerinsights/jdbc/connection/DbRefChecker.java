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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.strings.StringConverter;

interface DbRefChecker {
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

  class NoOpConnectionRefChecker implements DbConnectionRefChecker {
    private static final NoOpConnectionRefChecker INSTANCE = new NoOpConnectionRefChecker();

    @Override public void destroy() {}
    @Override public void addStatement(final Statement statement) {}
    @Override public boolean removeStatement(final Statement statement) { return true; }
    @Override public void addResultSet(final ResultSet rs) {}
    @Override public boolean removeResultSet(final ResultSet rs) { return true; }
  }

  final class SimpleRefChecker implements DbRefChecker {
    private static final ConcurrentHashMap<DbInfo, DbRefChecker> refsMap = new ConcurrentHashMap<>(512);

    private final ConcurrentHashMap<DbConnection, DbConnectionRefChecker> connectionRefs = new ConcurrentHashMap<>(64);

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
      return connectionRefs.computeIfAbsent(connection, SimpleConnectionRefChecker::new);
    }

    @Override
    public void closeConnection(final DbConnection connection) {
      final DbConnectionRefChecker refChecker = connectionRefs.remove(connection);
      if (refChecker != null) refChecker.destroy();
    }
  }

  final class SimpleConnectionRefChecker implements DbConnectionRefChecker {
    private final Set<Statement> statementRefs = ConcurrentHashMap.newKeySet(32);
    private final Set<ResultSet> resultSetRefs = ConcurrentHashMap.newKeySet(32);

    private SimpleConnectionRefChecker(final DbConnection connection) {
      // no-op
    }

    @Override
    public void destroy() {
      if (statementRefs.isEmpty() && resultSetRefs.isEmpty()) {
        return;
      }

      Logger.error("connection closed with {} resultSet active and {} statements active", resultSetRefs.size(), statementRefs.size());
      for (final ResultSet rs: resultSetRefs) {
        Logger.warn("RESULT-SET NOT CLOSED: {}", rs);
        DbConnection.closeQuietly(rs);
      }
      resultSetRefs.clear();
      for (final Statement stmt: statementRefs) {
        Logger.warn("STATEMENT NOT CLOSED: {}", stmt);
        DbConnection.closeQuietly(stmt);
      }
      statementRefs.clear();
    }

    @Override
    public void addStatement(final Statement statement) {
      statementRefs.add(statement);
    }

    @Override
    public boolean removeStatement(final Statement statement) {
      return statementRefs.remove(statement);
    }

    @Override
    public void addResultSet(final ResultSet rs) {
      resultSetRefs.add(rs);
    }

    @Override
    public boolean removeResultSet(final ResultSet rs) {
      return resultSetRefs.remove(rs);
    }
  }
}
