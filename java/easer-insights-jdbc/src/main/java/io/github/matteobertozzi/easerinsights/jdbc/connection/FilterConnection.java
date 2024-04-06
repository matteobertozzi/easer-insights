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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class FilterConnection implements Connection {
  private final Connection con;

  public FilterConnection(final Connection con) {
    this.con = con;
  }

  public Connection getRawConnection() {
    return con;
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    return con.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return con.isWrapperFor(iface);
  }

  @Override
  public Statement createStatement() throws SQLException {
    return con.createStatement();
  }

  @Override
  public PreparedStatement prepareStatement(final String sql) throws SQLException {
    return con.prepareStatement(sql);
  }

  @Override
  public CallableStatement prepareCall(final String sql) throws SQLException {
    return con.prepareCall(sql);
  }

  @Override
  public String nativeSQL(final String sql) throws SQLException {
    return con.nativeSQL(sql);
  }

  @Override
  public void setAutoCommit(final boolean autoCommit) throws SQLException {
    con.setAutoCommit(autoCommit);
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return con.getAutoCommit();
  }

  @Override
  public void commit() throws SQLException {
    con.commit();
  }

  @Override
  public void rollback() throws SQLException {
    con.rollback();
  }

  @Override
  public void close() throws SQLException {
    con.close();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return con.isClosed();
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return con.getMetaData();
  }

  @Override
  public void setReadOnly(final boolean readOnly) throws SQLException {
    con.setReadOnly(readOnly);
  }

  @Override
  public boolean isReadOnly() throws SQLException {
    return con.isReadOnly();
  }

  @Override
  public void setCatalog(final String catalog) throws SQLException {
    con.setCatalog(catalog);
  }

  @Override
  public String getCatalog() throws SQLException {
    return con.getCatalog();
  }

  @Override
  public void setTransactionIsolation(final int level) throws SQLException {
    con.setTransactionIsolation(level);
  }

  @Override
  public int getTransactionIsolation() throws SQLException {
    return con.getTransactionIsolation();
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return con.getWarnings();
  }

  @Override
  public void clearWarnings() throws SQLException {
    con.clearWarnings();
  }

  @Override
  public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
    return con.createStatement(resultSetType, resultSetConcurrency);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
      throws SQLException {
    return con.prepareStatement(sql, resultSetType, resultSetConcurrency);
  }

  @Override
  public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
    return con.prepareCall(sql, resultSetType, resultSetConcurrency);
  }

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    return con.getTypeMap();
  }

  @Override
  public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
    con.setTypeMap(map);
  }

  @Override
  public void setHoldability(final int holdability) throws SQLException {
    con.setHoldability(holdability);
  }

  @Override
  public int getHoldability() throws SQLException {
    return con.getHoldability();
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {
    return con.setSavepoint();
  }

  @Override
  public Savepoint setSavepoint(final String name) throws SQLException {
    return con.setSavepoint(name);
  }

  @Override
  public void rollback(final Savepoint savepoint) throws SQLException {
    con.rollback(savepoint);
  }

  @Override
  public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
    con.releaseSavepoint(savepoint);
  }

  @Override
  public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability)
      throws SQLException {
    return con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
      final int resultSetHoldability) throws SQLException {
    return con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
      final int resultSetHoldability) throws SQLException {
    return con.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
    return con.prepareStatement(sql, autoGeneratedKeys);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
    return con.prepareStatement(sql, columnIndexes);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
    return con.prepareStatement(sql, columnNames);
  }

  @Override
  public Clob createClob() throws SQLException {
    return con.createClob();
  }

  @Override
  public Blob createBlob() throws SQLException {
    return con.createBlob();
  }

  @Override
  public NClob createNClob() throws SQLException {
    return con.createNClob();
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    return con.createSQLXML();
  }

  @Override
  public boolean isValid(final int timeout) throws SQLException {
    return con.isValid(timeout);
  }

  @Override
  public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
    con.setClientInfo(name, value);
  }

  @Override
  public void setClientInfo(final Properties properties) throws SQLClientInfoException {
    con.setClientInfo(properties);
  }

  @Override
  public String getClientInfo(final String name) throws SQLException {
    return con.getClientInfo(name);
  }

  @Override
  public Properties getClientInfo() throws SQLException {
    return con.getClientInfo();
  }

  @Override
  public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
    return con.createArrayOf(typeName, elements);
  }

  @Override
  public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
    return con.createStruct(typeName, attributes);
  }

  @Override
  public void setSchema(final String schema) throws SQLException {
    con.setSchema(schema);
  }

  @Override
  public String getSchema() throws SQLException {
    return con.getSchema();
  }

  @Override
  public void abort(final Executor executor) throws SQLException {
    con.abort(executor);
  }

  @Override
  public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
    con.setNetworkTimeout(executor, milliseconds);
  }

  @Override
  public int getNetworkTimeout() throws SQLException {
    return con.getNetworkTimeout();
  }
}
