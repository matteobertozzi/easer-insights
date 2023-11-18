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

public final class PostgreSqlDialect {
  private PostgreSqlDialect() {
    // no-op
  }

  // ====================================================================================================
  //  Parse JDBC URL
  // ====================================================================================================
  private static final String JDBC_PREFIX_AWS_POSTGRESQL = "jdbc:postgresql:aws://";
  private static final String JDBC_PREFIX_POSTGRESQL = "jdbc:postgresql://";

  private static String jdbcUrlPrefix(final DbType type) {
    return switch (type) {
      case AWS_POSTGRESQL -> JDBC_PREFIX_AWS_POSTGRESQL;
      case POSTGRESQL -> JDBC_PREFIX_POSTGRESQL;
      default -> throw new IllegalArgumentException("unexpected type for mysq: " + type);
    };
  }

  public static DbInfo parseJdbcUrl(final String url) {
    if (url.startsWith(JDBC_PREFIX_AWS_POSTGRESQL)) {
      return DbInfo.parseStandardJdbcUrl(DbType.AWS_POSTGRESQL, JDBC_PREFIX_AWS_POSTGRESQL, url);
    } else if (url.startsWith(JDBC_PREFIX_POSTGRESQL)) {
      return DbInfo.parseStandardJdbcUrl(DbType.POSTGRESQL, JDBC_PREFIX_POSTGRESQL, url);
    }
    return null;
  }

  // ====================================================================================================
  //  Build JDBC URL
  // ====================================================================================================
  private static final String[] DEFAULT_PROPERTIES = new String[] {
    // PostgreSQL

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
