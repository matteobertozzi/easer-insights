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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public class NoOpPreparedStatement implements PreparedStatement {
  private boolean isClosed = false;

  public NoOpPreparedStatement(final String sql) {
    //TODO Auto-generated constructor stub
  }

  @Override
  public ResultSet executeQuery(final String sql) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeQuery'");
  }

  @Override
  public int executeUpdate(final String sql) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeUpdate'");
  }

  @Override
  public void close() throws SQLException {
    isClosed = true;
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getMaxFieldSize'");
  }

  @Override
  public void setMaxFieldSize(final int max) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setMaxFieldSize'");
  }

  @Override
  public int getMaxRows() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getMaxRows'");
  }

  @Override
  public void setMaxRows(final int max) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setMaxRows'");
  }

  @Override
  public void setEscapeProcessing(final boolean enable) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setEscapeProcessing'");
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getQueryTimeout'");
  }

  @Override
  public void setQueryTimeout(final int seconds) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setQueryTimeout'");
  }

  @Override
  public void cancel() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'cancel'");
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getWarnings'");
  }

  @Override
  public void clearWarnings() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clearWarnings'");
  }

  @Override
  public void setCursorName(final String name) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setCursorName'");
  }

  @Override
  public boolean execute(final String sql) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'execute'");
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getResultSet'");
  }

  @Override
  public int getUpdateCount() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getUpdateCount'");
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getMoreResults'");
  }

  @Override
  public void setFetchDirection(final int direction) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setFetchDirection'");
  }

  @Override
  public int getFetchDirection() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getFetchDirection'");
  }

  @Override
  public void setFetchSize(final int rows) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setFetchSize'");
  }

  @Override
  public int getFetchSize() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getFetchSize'");
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getResultSetConcurrency'");
  }

  @Override
  public int getResultSetType() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getResultSetType'");
  }

  @Override
  public void addBatch(final String sql) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'addBatch'");
  }

  @Override
  public void clearBatch() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clearBatch'");
  }

  @Override
  public int[] executeBatch() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeBatch'");
  }

  @Override
  public Connection getConnection() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getConnection'");
  }

  @Override
  public boolean getMoreResults(final int current) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getMoreResults'");
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getGeneratedKeys'");
  }

  @Override
  public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeUpdate'");
  }

  @Override
  public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeUpdate'");
  }

  @Override
  public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeUpdate'");
  }

  @Override
  public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'execute'");
  }

  @Override
  public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'execute'");
  }

  @Override
  public boolean execute(final String sql, final String[] columnNames) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'execute'");
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getResultSetHoldability'");
  }

  @Override
  public boolean isClosed() throws SQLException {
    return isClosed;
  }

  @Override
  public void setPoolable(final boolean poolable) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setPoolable'");
  }

  @Override
  public boolean isPoolable() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isPoolable'");
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'closeOnCompletion'");
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isCloseOnCompletion'");
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'unwrap'");
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isWrapperFor'");
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeQuery'");
  }

  @Override
  public int executeUpdate() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'executeUpdate'");
  }

  @Override
  public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNull'");
  }

  @Override
  public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBoolean'");
  }

  @Override
  public void setByte(final int parameterIndex, final byte x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setByte'");
  }

  @Override
  public void setShort(final int parameterIndex, final short x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setShort'");
  }

  @Override
  public void setInt(final int parameterIndex, final int x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setInt'");
  }

  @Override
  public void setLong(final int parameterIndex, final long x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setLong'");
  }

  @Override
  public void setFloat(final int parameterIndex, final float x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setFloat'");
  }

  @Override
  public void setDouble(final int parameterIndex, final double x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setDouble'");
  }

  @Override
  public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBigDecimal'");
  }

  @Override
  public void setString(final int parameterIndex, final String x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setString'");
  }

  @Override
  public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBytes'");
  }

  @Override
  public void setDate(final int parameterIndex, final Date x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setDate'");
  }

  @Override
  public void setTime(final int parameterIndex, final Time x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setTime'");
  }

  @Override
  public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setTimestamp'");
  }

  @Override
  public void setAsciiStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setAsciiStream'");
  }

  @Override
  public void setUnicodeStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setUnicodeStream'");
  }

  @Override
  public void setBinaryStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBinaryStream'");
  }

  @Override
  public void clearParameters() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'clearParameters'");
  }

  @Override
  public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setObject'");
  }

  @Override
  public void setObject(final int parameterIndex, final Object x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setObject'");
  }

  @Override
  public boolean execute() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'execute'");
  }

  @Override
  public void addBatch() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'addBatch'");
  }

  @Override
  public void setCharacterStream(final int parameterIndex, final Reader reader, final int length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setCharacterStream'");
  }

  @Override
  public void setRef(final int parameterIndex, final Ref x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setRef'");
  }

  @Override
  public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBlob'");
  }

  @Override
  public void setClob(final int parameterIndex, final Clob x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setClob'");
  }

  @Override
  public void setArray(final int parameterIndex, final Array x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setArray'");
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getMetaData'");
  }

  @Override
  public void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setDate'");
  }

  @Override
  public void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setTime'");
  }

  @Override
  public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setTimestamp'");
  }

  @Override
  public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNull'");
  }

  @Override
  public void setURL(final int parameterIndex, final URL x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setURL'");
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getParameterMetaData'");
  }

  @Override
  public void setRowId(final int parameterIndex, final RowId x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setRowId'");
  }

  @Override
  public void setNString(final int parameterIndex, final String value) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNString'");
  }

  @Override
  public void setNCharacterStream(final int parameterIndex, final Reader value, final long length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNCharacterStream'");
  }

  @Override
  public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNClob'");
  }

  @Override
  public void setClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setClob'");
  }

  @Override
  public void setBlob(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBlob'");
  }

  @Override
  public void setNClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNClob'");
  }

  @Override
  public void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setSQLXML'");
  }

  @Override
  public void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scaleOrLength) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setObject'");
  }

  @Override
  public void setAsciiStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setAsciiStream'");
  }

  @Override
  public void setBinaryStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBinaryStream'");
  }

  @Override
  public void setCharacterStream(final int parameterIndex, final Reader reader, final long length) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setCharacterStream'");
  }

  @Override
  public void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setAsciiStream'");
  }

  @Override
  public void setBinaryStream(final int parameterIndex, final InputStream x) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBinaryStream'");
  }

  @Override
  public void setCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setCharacterStream'");
  }

  @Override
  public void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNCharacterStream'");
  }

  @Override
  public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setClob'");
  }

  @Override
  public void setBlob(final int parameterIndex, final InputStream inputStream) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setBlob'");
  }

  @Override
  public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setNClob'");
  }

}
