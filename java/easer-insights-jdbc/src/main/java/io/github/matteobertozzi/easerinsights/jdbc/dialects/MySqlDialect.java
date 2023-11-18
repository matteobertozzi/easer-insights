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

public final class MySqlDialect {
  private MySqlDialect() {
    // no-op
  }

  // ====================================================================================================
  //  Parse JDBC URL
  //   - jdbc:mysql://localhost/mydb?user=username&password=password
  // ====================================================================================================
  private static final String JDBC_PREFIX_AWS_MYSQL = "jdbc:mysql:aws://";
  private static final String JDBC_PREFIX_MARIADB = "jdbc:mariadb://";
  private static final String JDBC_PREFIX_MYSQL = "jdbc:mysql://";

  private static String jdbcUrlPrefix(final DbType type) {
    return switch (type) {
      case AWS_MYSQL -> JDBC_PREFIX_AWS_MYSQL;
      case MARIADB -> JDBC_PREFIX_MARIADB;
      case MYSQL -> JDBC_PREFIX_MYSQL;
      default -> throw new IllegalArgumentException("unexpected type for mysq: " + type);
    };
  }

  public static DbInfo parseJdbcUrl(final String url) {
    if (url.startsWith(JDBC_PREFIX_AWS_MYSQL)) {
      return DbInfo.parseStandardJdbcUrl(DbType.AWS_MYSQL, JDBC_PREFIX_AWS_MYSQL, url);
    } else if (url.startsWith(JDBC_PREFIX_MYSQL)) {
      return DbInfo.parseStandardJdbcUrl(DbType.MYSQL, JDBC_PREFIX_MYSQL, url);
    } else if (url.startsWith(JDBC_PREFIX_MARIADB)) {
      return DbInfo.parseStandardJdbcUrl(DbType.MARIADB, JDBC_PREFIX_MARIADB, url);
    }
    return null;
  }

  // ====================================================================================================
  //  Build JDBC URL
  // ====================================================================================================
  private static final String[] DEFAULT_PROPERTIES = new String[] {
    // MySQL
    "autoReconnect", "true",
    "useServerPrepStmts", "true",
    "rewriteBatchedStatements", "true",
    "cachePrepStmts", "true",
    "prepStmtCacheSize", "250",
    "prepStmtCacheSqlLimit", "2048",
    "useLocalSessionState", "true",

    // AWS Aurora
    "enableClusterAwareFailover", "false",
  };

  public static DbInfo dbInfoFrom(final DbType type, final String host, final String dbName, final Properties properties) {
    for (int i = 0; i < DEFAULT_PROPERTIES.length; i += 2) {
      if (!properties.containsKey(DEFAULT_PROPERTIES[i])) {
        properties.setProperty(DEFAULT_PROPERTIES[i], DEFAULT_PROPERTIES[i + 1]);
      }
    }

    final String jdbcUrl = jdbcUrlPrefix(type) + host + "/" + dbName;
    return new DbInfo(type, jdbcUrl, host, dbName, properties);
  }
}
