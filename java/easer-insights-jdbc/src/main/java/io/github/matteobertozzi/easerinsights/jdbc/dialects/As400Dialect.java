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

public final class As400Dialect {
  private As400Dialect() {
    // no-op
  }

  // ====================================================================================================
  //  Parse JDBC URL
  // ====================================================================================================
  private static final String JDBC_PREFIX = "jdbc:as400://";

  public static DbInfo parseJdbcUrl(final String url) {
    if (url.startsWith(JDBC_PREFIX)) {
      return DbInfo.parseStandardJdbcUrl(DbType.AS400, JDBC_PREFIX, url);
    }
    return null;
  }

  // ====================================================================================================
  //  Build JDBC URL
  // ====================================================================================================
  private static final String[] DEFAULT_PROPERTIES = new String[] {};

  public static DbInfo dbInfoFrom(final DbType type, final String host, final String dbName, final Properties properties) {
    for (int i = 0; i < DEFAULT_PROPERTIES.length; i += 2) {
      if (!properties.containsKey(DEFAULT_PROPERTIES[i])) {
        properties.setProperty(DEFAULT_PROPERTIES[i], DEFAULT_PROPERTIES[i + 1]);
      }
    }

    final String jdbcUrl = JDBC_PREFIX + host + "/" + dbName;
    return new DbInfo(type, jdbcUrl, host, dbName, properties);
  }
}
