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

import java.sql.SQLException;
import java.sql.Savepoint;

import io.github.matteobertozzi.easerinsights.logging.Logger;

public class DbAutoRollback implements AutoCloseable {
  private final DbConnection connection;
  private final Savepoint savepoint;
  private final boolean originalAutoCommit;
  private boolean committed;

  public DbAutoRollback(final DbConnection conn) throws SQLException {
    this(conn, true);
  }

  public DbAutoRollback(final DbConnection conn, final boolean unsetAutoCommit)
      throws SQLException {
    this.connection = conn;
    if (unsetAutoCommit) {
      this.originalAutoCommit = conn.getAutoCommit();
      if (this.originalAutoCommit) {
        conn.setAutoCommit(false);
      }
    } else {
      this.originalAutoCommit = false;
    }
    this.savepoint = conn.setSavepoint();
  }

  public DbConnection getConnection() {
    return connection;
  }

  public void commit() throws SQLException {
    if (originalAutoCommit) {
      connection.commit();
    }
    committed = true;
  }

  @Override
  public void close() throws SQLException {
    try {
      if (!committed) {
        Logger.debug("transaction was not committed, rolling back!");
        connection.rollback(savepoint);
      }
    } finally {
      connection.releaseSavepoint(savepoint);
      if (originalAutoCommit) {
        connection.setAutoCommit(true);
      }
    }
  }
}
