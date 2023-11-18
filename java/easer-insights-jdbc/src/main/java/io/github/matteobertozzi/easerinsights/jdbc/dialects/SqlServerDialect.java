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

public final class SqlServerDialect {
  private SqlServerDialect() {
    // no-op
  }

  // ====================================================================================================
  //  Parse JDBC URL
  //   - jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;
  // ====================================================================================================
  private static final String JDBC_PREFIX = "jdbc:sqlserver://";

  public static DbInfo parseJdbcUrl(final String url) {
    if (!url.startsWith(JDBC_PREFIX)) {
      return null;
    }

    int offset = 17;
    final int hostEndIndex = url.indexOf(';', offset);
    final String host = url.substring(offset, hostEndIndex);
    offset = hostEndIndex + 1;

    final Properties properties = new Properties();
    while (true) {
      final int keyEndIndex = url.indexOf('=', offset);
      if (keyEndIndex < 0) {
        break;
      }

      int valEndIndex = url.indexOf(';', keyEndIndex);
      if (valEndIndex < 0) {
        valEndIndex = url.length();
      }

      final String key = url.substring(offset, keyEndIndex);
      final String val = url.substring(keyEndIndex + 1, valEndIndex);
      properties.put(key, val);

      offset = valEndIndex + 1;
    }
    return new DbInfo(DbType.SQLSERVER, url, host, properties.getProperty("databaseName"), properties);
  }

  // ====================================================================================================
  //  Build JDBC URL
  // ====================================================================================================
  private static final String[] DEFAULT_PROPERTIES = new String[] {};

  public static DbInfo dbInfoFrom(final DbType type, final String host, final String dbName, final Properties properties) {
    properties.put("databaseName", dbName);
    for (int i = 0; i < DEFAULT_PROPERTIES.length; i += 2) {
      if (!properties.containsKey(DEFAULT_PROPERTIES[i])) {
        properties.setProperty(DEFAULT_PROPERTIES[i], DEFAULT_PROPERTIES[i + 1]);
      }
    }

    final String jdbcUrl = JDBC_PREFIX + host;
    return new DbInfo(type, jdbcUrl, host, properties.getProperty("databaseName"), properties);
  }
}
