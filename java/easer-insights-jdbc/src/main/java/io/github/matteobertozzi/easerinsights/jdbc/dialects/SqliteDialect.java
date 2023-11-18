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

package io.github.matteobertozzi.easerinsights.jdbc.dialects;

import java.util.Properties;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.DbType;

public final class SqliteDialect {
  private SqliteDialect() {
    // no-op
  }

  // ====================================================================================================
  //  Parse JDBC URL
  //   - jdbc:sqlite::memory:
  //   - jdbc:sqlite:/path/to/mydatabase.db
  //   - jdbc:sqlite:mydatabase.db?journal_mode=WAL&foreign_keys=true
  // ====================================================================================================
  private static final String JDBC_PREFIX = "jdbc:sqlite:";

  public static DbInfo parseJdbcUrl(final String url) {
    if (url.startsWith(JDBC_PREFIX)) {
      final String dbName = url.substring(JDBC_PREFIX.length());
      return new DbInfo(DbType.SQLITE, url, null, dbName, null, new Properties());
    }
    return null;
  }
}
