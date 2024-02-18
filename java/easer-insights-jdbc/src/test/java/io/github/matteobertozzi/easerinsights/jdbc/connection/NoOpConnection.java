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
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import io.github.matteobertozzi.easerinsights.logging.Logger;

public class NoOpConnection implements Connection {
  private final long id;

  private int transactionIsolation = 0;
  private boolean hasAutoCommit = false;
  private boolean isClosed = false;

  public NoOpConnection(final long id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "NoOpConnection [" + id + "]";
  }

  @Override
  public <T> T unwrap(final Class<T> iface) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'unwrap'");
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isWrapperFor'");
  }

  @Override
  public Statement createStatement() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createStatement'");
  }

  @Override
  public PreparedStatement prepareStatement(final String sql) {
    return new NoOpPreparedStatement(sql);
  }

  @Override
  public CallableStatement prepareCall(final String sql) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'prepareCall'");
  }

  @Override
  public String nativeSQL(final String sql) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'nativeSQL'");
  }

  @Override
  public void setAutoCommit(final boolean autoCommit) {
    this.hasAutoCommit = autoCommit;
  }

  @Override
  public boolean getAutoCommit() {
    return hasAutoCommit;
  }

  @Override
  public void commit() {
    Logger.debug("commit");
  }

  @Override
  public void rollback() {
    Logger.debug("rollback");
  }

  @Override
  public void close() {
    isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public DatabaseMetaData getMetaData() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getMetaData'");
  }

  @Override
  public void setReadOnly(final boolean readOnly) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setReadOnly'");
  }

  @Override
  public boolean isReadOnly() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isReadOnly'");
  }

  @Override
  public void setCatalog(final String catalog) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setCatalog'");
  }

  @Override
  public String getCatalog() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getCatalog'");
  }

  @Override
  public void setTransactionIsolation(final int level) {
    this.transactionIsolation = level;
  }

  @Override
  public int getTransactionIsolation() {
    return transactionIsolation;
  }

  @Override
  public SQLWarning getWarnings() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getWarnings'");
  }

  @Override
  public void clearWarnings() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clearWarnings'");
  }

  @Override
  public Statement createStatement(final int resultSetType, final int resultSetConcurrency) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createStatement'");
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) {
    return new NoOpPreparedStatement(sql);
  }

  @Override
  public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'prepareCall'");
  }

  @Override
  public Map<String, Class<?>> getTypeMap() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getTypeMap'");
  }

  @Override
  public void setTypeMap(final Map<String, Class<?>> map) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setTypeMap'");
  }

  @Override
  public void setHoldability(final int holdability) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setHoldability'");
  }

  @Override
  public int getHoldability() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getHoldability'");
  }

  @Override
  public Savepoint setSavepoint() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setSavepoint'");
  }

  @Override
  public Savepoint setSavepoint(final String name) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setSavepoint'");
  }

  @Override
  public void rollback(final Savepoint savepoint) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'rollback'");
  }

  @Override
  public void releaseSavepoint(final Savepoint savepoint) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'releaseSavepoint'");
  }

  @Override
  public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createStatement'");
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
      final int resultSetHoldability) {
    return new NoOpPreparedStatement(sql);
  }

  @Override
  public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
      final int resultSetHoldability) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'prepareCall'");
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) {
    return new NoOpPreparedStatement(sql);
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'prepareStatement'");
  }

  @Override
  public PreparedStatement prepareStatement(final String sql, final String[] columnNames) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'prepareStatement'");
  }

  @Override
  public Clob createClob() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createClob'");
  }

  @Override
  public Blob createBlob() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createBlob'");
  }

  @Override
  public NClob createNClob() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createNClob'");
  }

  @Override
  public SQLXML createSQLXML() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createSQLXML'");
  }

  @Override
  public boolean isValid(final int timeout) {
    return true;
  }

  @Override
  public void setClientInfo(final String name, final String value) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setClientInfo'");
  }

  @Override
  public void setClientInfo(final Properties properties) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setClientInfo'");
  }

  @Override
  public String getClientInfo(final String name) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getClientInfo'");
  }

  @Override
  public Properties getClientInfo() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getClientInfo'");
  }

  @Override
  public Array createArrayOf(final String typeName, final Object[] elements) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createArrayOf'");
  }

  @Override
  public Struct createStruct(final String typeName, final Object[] attributes) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createStruct'");
  }

  @Override
  public void setSchema(final String schema) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setSchema'");
  }

  @Override
  public String getSchema() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getSchema'");
  }

  @Override
  public void abort(final Executor executor) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'abort'");
  }

  @Override
  public void setNetworkTimeout(final Executor executor, final int milliseconds) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNetworkTimeout'");
  }

  @Override
  public int getNetworkTimeout() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getNetworkTimeout'");
  }
}
