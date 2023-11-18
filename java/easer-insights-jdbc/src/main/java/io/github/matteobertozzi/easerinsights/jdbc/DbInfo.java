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

package io.github.matteobertozzi.easerinsights.jdbc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import io.github.matteobertozzi.easerinsights.jdbc.dialects.As400Dialect;
import io.github.matteobertozzi.easerinsights.jdbc.dialects.DuckDbDialect;
import io.github.matteobertozzi.easerinsights.jdbc.dialects.MySqlDialect;
import io.github.matteobertozzi.easerinsights.jdbc.dialects.OracleDialect;
import io.github.matteobertozzi.easerinsights.jdbc.dialects.PostgreSqlDialect;
import io.github.matteobertozzi.easerinsights.jdbc.dialects.SqlServerDialect;
import io.github.matteobertozzi.easerinsights.jdbc.dialects.SqliteDialect;

public record DbInfo(DbType type, String url, String host, String dbName, String driver, Properties properties) {
  public DbInfo(final DbType type, final String url, final String host, final String dbName, final Properties properties) {
    this(type, url, host, dbName, null, properties);
  }

  @FunctionalInterface
  interface JdbcUrlParser {
    DbInfo parseJdbcUrl(String url);
  }

  @FunctionalInterface
  interface DbInfoFrom {
    DbInfo dbInfoFrom(DbType type, String host, String dbName, Properties properties);
  }

  private static final JdbcUrlParser[] JDBC_URL_PARSERS = new JdbcUrlParser[]{
    MySqlDialect::parseJdbcUrl,
    SqlServerDialect::parseJdbcUrl,
    PostgreSqlDialect::parseJdbcUrl,
    OracleDialect::parseJdbcUrl,
    As400Dialect::parseJdbcUrl,
    DuckDbDialect::parseJdbcUrl,
    SqliteDialect::parseJdbcUrl,
  };

  private static final EnumMap<DbType, DbInfoFrom> DB_INFO_FROM_FACTORY = new EnumMap<>(DbType.class);
  static {
    DB_INFO_FROM_FACTORY.put(DbType.AWS_MYSQL, MySqlDialect::dbInfoFrom);
    DB_INFO_FROM_FACTORY.put(DbType.MARIADB, MySqlDialect::dbInfoFrom);
    DB_INFO_FROM_FACTORY.put(DbType.MYSQL, MySqlDialect::dbInfoFrom);

    DB_INFO_FROM_FACTORY.put(DbType.SQLSERVER, SqlServerDialect::dbInfoFrom);
  }

  public static DbInfo from(final DbType type, final String host, final String dbName, final Map<String, String> properties) {
    final Properties jdbcProperties = new Properties(properties.size());
    for (final Map.Entry<String, String> entry: properties.entrySet()) {
      jdbcProperties.setProperty(entry.getKey(), entry.getValue());
    }
    return from(type, host, dbName, jdbcProperties);
  }

  public static DbInfo from(final DbType type, final String host, final String dbName, final Properties properties) {
    final DbInfoFrom factory = DB_INFO_FROM_FACTORY.get(type);
    if (factory != null) {
      return factory.dbInfoFrom(type, host, dbName, properties);
    }
    throw new UnsupportedOperationException(type + " JDBC URL creation not supported yet");
  }

  public boolean hasProperties() {
    return properties != null && !properties.isEmpty();
  }

  public String getProperty(final String key) {
    return properties.getProperty(key);
  }

  public String getProperty(final String key, final String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  public int getTransactionIsolation() {
    return type == DbType.SQLITE ? Connection.TRANSACTION_SERIALIZABLE : Connection.TRANSACTION_READ_COMMITTED;
  }

  // ====================================================================================================
  //  Parse JDBC URL
  // ====================================================================================================
  public static DbInfo parseJdbcUrl(final String url) {
    final int jdbcTypeIndex = url.indexOf("jdbc:");
    if (jdbcTypeIndex < 0) {
      throw new IllegalArgumentException("expected url to start with 'jdbc:' got: " + url);
    }

    for (final JdbcUrlParser urlParser: JDBC_URL_PARSERS) {
      final DbInfo dbInfo = urlParser.parseJdbcUrl(url);
      if (dbInfo != null) {
        return dbInfo;
      }
    }

    throw new UnsupportedOperationException("unable to parser jdbc url: " + url);
  }

  public static DbInfo parseStandardJdbcUrl(final DbType type, final String jdbcPrefix, final String url) {
    int offset = jdbcPrefix.length();
    int hostEndIndex = url.indexOf('/', offset);
    if (hostEndIndex < 0) hostEndIndex = url.length();
    final String host = url.substring(offset, hostEndIndex);

    offset = hostEndIndex + 1;
    int paramIndex = url.indexOf('?', offset);
    if (paramIndex < 0) paramIndex = url.length();
    final String dbName = url.substring(offset, paramIndex);

    offset = paramIndex + 1;
    final Properties properties = new Properties();
    while (true) {
      final int keyEndIndex = url.indexOf('=', offset);
      if (keyEndIndex < 0) break;

      paramIndex = url.indexOf('&', offset);
      if (paramIndex < 0) paramIndex = url.length();

      final String key = URLDecoder.decode(url.substring(offset, keyEndIndex), StandardCharsets.UTF_8);
      final String val = URLDecoder.decode(url.substring(keyEndIndex + 1, paramIndex), StandardCharsets.UTF_8);
      properties.put(key, val);

      offset = paramIndex + 1;
    }
    return new DbInfo(type, url, host, dbName, null, properties);
  }

  public static DbInfo fromUrl(final String url, final String user, final String password) {
    final DbInfo dbInfo = DbInfo.parseJdbcUrl(url);
    final Properties properties = dbInfo.properties();

    if (user != null && properties.getProperty("user") == null) {
      properties.put("user", user);
    }
    if (password != null && properties.getProperty("password") == null) {
      properties.put("password", password);
    }
    return dbInfo;
  }
}
