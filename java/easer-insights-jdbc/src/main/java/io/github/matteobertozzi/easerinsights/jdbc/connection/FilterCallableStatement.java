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
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
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
import java.util.Map;

public class FilterCallableStatement implements CallableStatement {
  private final CallableStatement stmt;

  public FilterCallableStatement(final CallableStatement stmt) {
    this.stmt = stmt;
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    return stmt.executeQuery();
  }

  @Override
  public int executeUpdate() throws SQLException {
    return stmt.executeUpdate();
  }

  @Override
  public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
    stmt.setNull(parameterIndex, sqlType);
  }

  @Override
  public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
    stmt.setBoolean(parameterIndex, x);
  }

  @Override
  public void setByte(final int parameterIndex, final byte x) throws SQLException {
    stmt.setByte(parameterIndex, x);
  }

  @Override
  public void setShort(final int parameterIndex, final short x) throws SQLException {
    stmt.setShort(parameterIndex, x);
  }

  @Override
  public void setInt(final int parameterIndex, final int x) throws SQLException {
    stmt.setInt(parameterIndex, x);
  }

  @Override
  public void setLong(final int parameterIndex, final long x) throws SQLException {
    stmt.setLong(parameterIndex, x);
  }

  @Override
  public void setFloat(final int parameterIndex, final float x) throws SQLException {
    stmt.setFloat(parameterIndex, x);
  }

  @Override
  public void setDouble(final int parameterIndex, final double x) throws SQLException {
    stmt.setDouble(parameterIndex, x);
  }

  @Override
  public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
    stmt.setBigDecimal(parameterIndex, x);
  }

  @Override
  public void setString(final int parameterIndex, final String x) throws SQLException {
    stmt.setString(parameterIndex, x);
  }

  @Override
  public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
    stmt.setBytes(parameterIndex, x);
  }

  @Override
  public void setDate(final int parameterIndex, final Date x) throws SQLException {
    stmt.setDate(parameterIndex, x);
  }

  @Override
  public void setTime(final int parameterIndex, final Time x) throws SQLException {
    stmt.setTime(parameterIndex, x);
  }

  @Override
  public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
    stmt.setTimestamp(parameterIndex, x);
  }

  @Override
  public void setAsciiStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
    stmt.setAsciiStream(parameterIndex, x, length);
  }

  @Override
  @Deprecated
  public void setUnicodeStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
    stmt.setUnicodeStream(parameterIndex, x, length);
  }

  @Override
  public void setBinaryStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
    stmt.setBinaryStream(parameterIndex, x, length);
  }

  @Override
  public void clearParameters() throws SQLException {
    stmt.clearParameters();
  }

  @Override
  public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
    stmt.setObject(parameterIndex, x, targetSqlType);
  }

  @Override
  public void setObject(final int parameterIndex, final Object x) throws SQLException {
    stmt.setObject(parameterIndex, x);
  }

  @Override
  public boolean execute() throws SQLException {
    return stmt.execute();
  }

  @Override
  public void addBatch() throws SQLException {
    stmt.addBatch();
  }

  @Override
  public void setCharacterStream(final int parameterIndex, final Reader reader, final int length) throws SQLException {
    stmt.setCharacterStream(parameterIndex, reader, length);
  }

  @Override
  public void setRef(final int parameterIndex, final Ref x) throws SQLException {
    stmt.setRef(parameterIndex, x);
  }

  @Override
  public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
    stmt.setBlob(parameterIndex, x);
  }

  @Override
  public void setClob(final int parameterIndex, final Clob x) throws SQLException {
    stmt.setClob(parameterIndex, x);
  }

  @Override
  public void setArray(final int parameterIndex, final Array x) throws SQLException {
    stmt.setArray(parameterIndex, x);
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return stmt.getMetaData();
  }

  @Override
  public void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
    stmt.setDate(parameterIndex, x, cal);
  }

  @Override
  public void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
    stmt.setTime(parameterIndex, x);
  }

  @Override
  public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
    stmt.setTimestamp(parameterIndex, x, cal);
  }

  @Override
  public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
    stmt.setNull(parameterIndex, sqlType, typeName);
  }

  @Override
  public void setURL(final int parameterIndex, final URL x) throws SQLException {
    stmt.setURL(parameterIndex, x);
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    return stmt.getParameterMetaData();
  }

  @Override
  public void setRowId(final int parameterIndex, final RowId x) throws SQLException {
    stmt.setRowId(parameterIndex, x);
  }

  @Override
  public void setNString(final int parameterIndex, final String value) throws SQLException {
    stmt.setNString(parameterIndex, value);
  }

  @Override
  public void setNCharacterStream(final int parameterIndex, final Reader value, final long length) throws SQLException {
    stmt.setNCharacterStream(parameterIndex, value, length);
  }

  @Override
  public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
    stmt.setNClob(parameterIndex, value);
  }

  @Override
  public void setClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
    stmt.setClob(parameterIndex, reader, length);
  }

  @Override
  public void setBlob(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
    stmt.setBlob(parameterIndex, inputStream, length);
  }

  @Override
  public void setNClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
    stmt.setNClob(parameterIndex, reader, length);
  }

  @Override
  public void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
    stmt.setSQLXML(parameterIndex, xmlObject);
  }

  @Override
  public void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scaleOrLength) throws SQLException {
    stmt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
  }

  @Override
  public void setAsciiStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
    stmt.setAsciiStream(parameterIndex, x, length);
  }

  @Override
  public void setBinaryStream(final int parameterIndex, final InputStream x, final long length) throws SQLException {
    stmt.setBinaryStream(parameterIndex, x, length);
  }

  @Override
  public void setCharacterStream(final int parameterIndex, final Reader reader, final long length) throws SQLException {
    stmt.setCharacterStream(parameterIndex, reader, length);
  }

  @Override
  public void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
    stmt.setAsciiStream(parameterIndex, x);
  }

  @Override
  public void setBinaryStream(final int parameterIndex, final InputStream x) throws SQLException {
    stmt.setBinaryStream(parameterIndex, x);
  }

  @Override
  public void setCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
    stmt.setCharacterStream(parameterIndex, reader);
  }

  @Override
  public void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
    stmt.setNCharacterStream(parameterIndex, value);
  }

  @Override
  public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
    stmt.setClob(parameterIndex, reader);
  }

  @Override
  public void setBlob(final int parameterIndex, final InputStream inputStream) throws SQLException {
    stmt.setBlob(parameterIndex, inputStream);
  }

  @Override
  public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
    stmt.setNClob(parameterIndex, reader);
  }

  @Override
  public ResultSet executeQuery(final String sql) throws SQLException {
    return stmt.executeQuery(sql);
  }

  @Override
  public int executeUpdate(final String sql) throws SQLException {
    return stmt.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    stmt.close();
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    return stmt.getMaxFieldSize();
  }

  @Override
  public void setMaxFieldSize(final int max) throws SQLException {
    stmt.setMaxFieldSize(max);
  }

  @Override
  public int getMaxRows() throws SQLException {
    return stmt.getMaxRows();
  }

  @Override
  public void setMaxRows(final int max) throws SQLException {
    stmt.setMaxRows(max);
  }

  @Override
  public void setEscapeProcessing(final boolean enable) throws SQLException {
    stmt.setEscapeProcessing(enable);
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    return stmt.getQueryTimeout();
  }

  @Override
  public void setQueryTimeout(final int seconds) throws SQLException {
    stmt.setQueryTimeout(seconds);
  }

  @Override
  public void cancel() throws SQLException {
    stmt.cancel();
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return stmt.getWarnings();
  }

  @Override
  public void clearWarnings() throws SQLException {
    stmt.clearWarnings();
  }

  @Override
  public void setCursorName(final String name) throws SQLException {
    stmt.setCursorName(name);
  }

  @Override
  public boolean execute(final String sql) throws SQLException {
    return stmt.execute(sql);
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    return stmt.getResultSet();
  }

  @Override
  public int getUpdateCount() throws SQLException {
    return stmt.getUpdateCount();
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    return stmt.getMoreResults();
  }

  @Override
  public void setFetchDirection(final int direction) throws SQLException {
    stmt.setFetchDirection(direction);
  }

  @Override
  public int getFetchDirection() throws SQLException {
    return stmt.getFetchDirection();
  }

  @Override
  public void setFetchSize(final int rows) throws SQLException {
    stmt.setFetchSize(rows);
  }

  @Override
  public int getFetchSize() throws SQLException {
    return stmt.getFetchSize();
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    return stmt.getResultSetConcurrency();
  }

  @Override
  public int getResultSetType() throws SQLException {
    return stmt.getResultSetType();
  }

  @Override
  public void addBatch(final String sql) throws SQLException {
    stmt.addBatch();
  }

  @Override
  public void clearBatch() throws SQLException {
    stmt.clearBatch();
  }

  @Override
  public int[] executeBatch() throws SQLException {
    return stmt.executeBatch();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return stmt.getConnection();
  }

  @Override
  public boolean getMoreResults(final int current) throws SQLException {
    return stmt.getMoreResults(current);
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    return stmt.getGeneratedKeys();
  }

  @Override
  public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
    return stmt.executeUpdate(sql, autoGeneratedKeys);
  }

  @Override
  public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
    return stmt.executeUpdate(sql, columnIndexes);
  }

  @Override
  public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
    return stmt.executeUpdate(sql, columnNames);
  }

  @Override
  public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
    return stmt.execute(sql, autoGeneratedKeys);
  }

  @Override
  public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
    return stmt.execute(sql, columnIndexes);
  }

  @Override
  public boolean execute(final String sql, final String[] columnNames) throws SQLException {
    return stmt.execute(sql, columnNames);
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    return stmt.getResultSetHoldability();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return stmt.isClosed();
  }

  @Override
  public void setPoolable(final boolean poolable) throws SQLException {
    stmt.setPoolable(poolable);
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return stmt.isPoolable();
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    stmt.closeOnCompletion();
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    return stmt.isCloseOnCompletion();
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    return stmt.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return stmt.isWrapperFor(iface);
  }

  @Override
  public String toString() {
    return stmt.toString();
  }

  @Override
  public void registerOutParameter(final int parameterIndex, final int sqlType) throws SQLException {
    stmt.registerOutParameter(parameterIndex, sqlType);
  }

  @Override
  public void registerOutParameter(final int parameterIndex, final int sqlType, final int scale) throws SQLException {
    stmt.registerOutParameter(parameterIndex, sqlType, scale);
  }

  @Override
  public boolean wasNull() throws SQLException {
    return stmt.wasNull();
  }

  @Override
  public String getString(final int parameterIndex) throws SQLException {
    return stmt.getString(parameterIndex);
  }

  @Override
  public boolean getBoolean(final int parameterIndex) throws SQLException {
    return stmt.getBoolean(parameterIndex);
  }

  @Override
  public byte getByte(final int parameterIndex) throws SQLException {
    return stmt.getByte(parameterIndex);
  }

  @Override
  public short getShort(final int parameterIndex) throws SQLException {
    return stmt.getShort(parameterIndex);
  }

  @Override
  public int getInt(final int parameterIndex) throws SQLException {
    return stmt.getInt(parameterIndex);
  }

  @Override
  public long getLong(final int parameterIndex) throws SQLException {
    return stmt.getLong(parameterIndex);
  }

  @Override
  public float getFloat(final int parameterIndex) throws SQLException {
    return stmt.getFloat(parameterIndex);
  }

  @Override
  public double getDouble(final int parameterIndex) throws SQLException {
    return stmt.getDouble(parameterIndex);
  }

  @Override
  @Deprecated
  public BigDecimal getBigDecimal(final int parameterIndex, final int scale) throws SQLException {
    return stmt.getBigDecimal(parameterIndex, scale);
  }

  @Override
  public byte[] getBytes(final int parameterIndex) throws SQLException {
    return stmt.getBytes(parameterIndex);
  }

  @Override
  public Date getDate(final int parameterIndex) throws SQLException {
    return stmt.getDate(parameterIndex);
  }

  @Override
  public Time getTime(final int parameterIndex) throws SQLException {
    return stmt.getTime(parameterIndex);
  }

  @Override
  public Timestamp getTimestamp(final int parameterIndex) throws SQLException {
    return stmt.getTimestamp(parameterIndex);
  }

  @Override
  public Object getObject(final int parameterIndex) throws SQLException {
    return stmt.getObject(parameterIndex);
  }

  @Override
  public BigDecimal getBigDecimal(final int parameterIndex) throws SQLException {
    return stmt.getBigDecimal(parameterIndex);
  }

  @Override
  public Object getObject(final int parameterIndex, final Map<String, Class<?>> map) throws SQLException {
    return stmt.getObject(parameterIndex, map);
  }

  @Override
  public Ref getRef(final int parameterIndex) throws SQLException {
    return stmt.getRef(parameterIndex);
  }

  @Override
  public Blob getBlob(final int parameterIndex) throws SQLException {
    return stmt.getBlob(parameterIndex);
  }

  @Override
  public Clob getClob(final int parameterIndex) throws SQLException {
    return stmt.getClob(parameterIndex);
  }

  @Override
  public Array getArray(final int parameterIndex) throws SQLException {
    return stmt.getArray(parameterIndex);
  }

  @Override
  public Date getDate(final int parameterIndex, final Calendar cal) throws SQLException {
    return stmt.getDate(parameterIndex, cal);
  }

  @Override
  public Time getTime(final int parameterIndex, final Calendar cal) throws SQLException {
    return stmt.getTime(parameterIndex, cal);
  }

  @Override
  public Timestamp getTimestamp(final int parameterIndex, final Calendar cal) throws SQLException {
    return stmt.getTimestamp(parameterIndex, cal);
  }

  @Override
  public void registerOutParameter(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
    stmt.registerOutParameter(parameterIndex, sqlType, typeName);
  }

  @Override
  public void registerOutParameter(final String parameterName, final int sqlType) throws SQLException {
    stmt.registerOutParameter(parameterName, sqlType);
  }

  @Override
  public void registerOutParameter(final String parameterName, final int sqlType, final int scale) throws SQLException {
    stmt.registerOutParameter(parameterName, sqlType, scale);
  }

  @Override
  public void registerOutParameter(final String parameterName, final int sqlType, final String typeName) throws SQLException {
    stmt.registerOutParameter(parameterName, sqlType, typeName);
  }

  @Override
  public URL getURL(final int parameterIndex) throws SQLException {
    return stmt.getURL(parameterIndex);
  }

  @Override
  public void setURL(final String parameterName, final URL val) throws SQLException {
    stmt.setURL(parameterName, val);
  }

  @Override
  public void setNull(final String parameterName, final int sqlType) throws SQLException {
    stmt.setNull(parameterName, sqlType);
  }

  @Override
  public void setBoolean(final String parameterName, final boolean x) throws SQLException {
    stmt.setBoolean(parameterName, x);
  }

  @Override
  public void setByte(final String parameterName, final byte x) throws SQLException {
    stmt.setByte(parameterName, x);
  }

  @Override
  public void setShort(final String parameterName, final short x) throws SQLException {
    stmt.setShort(parameterName, x);
  }

  @Override
  public void setInt(final String parameterName, final int x) throws SQLException {
    stmt.setInt(parameterName, x);
  }

  @Override
  public void setLong(final String parameterName, final long x) throws SQLException {
    stmt.setLong(parameterName, x);
  }

  @Override
  public void setFloat(final String parameterName, final float x) throws SQLException {
    stmt.setFloat(parameterName, x);
  }

  @Override
  public void setDouble(final String parameterName, final double x) throws SQLException {
    stmt.setDouble(parameterName, x);
  }

  @Override
  public void setBigDecimal(final String parameterName, final BigDecimal x) throws SQLException {
    stmt.setBigDecimal(parameterName, x);
  }

  @Override
  public void setString(final String parameterName, final String x) throws SQLException {
    stmt.setString(parameterName, x);
  }

  @Override
  public void setBytes(final String parameterName, final byte[] x) throws SQLException {
    stmt.setBytes(parameterName, x);
  }

  @Override
  public void setDate(final String parameterName, final Date x) throws SQLException {
    stmt.setDate(parameterName, x);
  }

  @Override
  public void setTime(final String parameterName, final Time x) throws SQLException {
    stmt.setTime(parameterName, x);
  }

  @Override
  public void setTimestamp(final String parameterName, final Timestamp x) throws SQLException {
    stmt.setTimestamp(parameterName, x);
  }

  @Override
  public void setAsciiStream(final String parameterName, final InputStream x, final int length) throws SQLException {
    stmt.setAsciiStream(parameterName, x);
  }

  @Override
  public void setBinaryStream(final String parameterName, final InputStream x, final int length) throws SQLException {
    stmt.setBinaryStream(parameterName, x, length);
  }

  @Override
  public void setObject(final String parameterName, final Object x, final int targetSqlType, final int scale) throws SQLException {
    stmt.setObject(parameterName, x, targetSqlType, scale);
  }

  @Override
  public void setObject(final String parameterName, final Object x, final int targetSqlType) throws SQLException {
    stmt.setObject(parameterName, x, targetSqlType);
  }

  @Override
  public void setObject(final String parameterName, final Object x) throws SQLException {
    stmt.setObject(parameterName, x);
  }

  @Override
  public void setCharacterStream(final String parameterName, final Reader reader, final int length) throws SQLException {
    stmt.setCharacterStream(parameterName, reader, length);
  }

  @Override
  public void setDate(final String parameterName, final Date x, final Calendar cal) throws SQLException {
    stmt.setDate(parameterName, x, cal);
  }

  @Override
  public void setTime(final String parameterName, final Time x, final Calendar cal) throws SQLException {
    stmt.setTime(parameterName, x, cal);
  }

  @Override
  public void setTimestamp(final String parameterName, final Timestamp x, final Calendar cal) throws SQLException {
    stmt.setTimestamp(parameterName, x, cal);
  }

  @Override
  public void setNull(final String parameterName, final int sqlType, final String typeName) throws SQLException {
    stmt.setNull(parameterName, sqlType, typeName);
  }

  @Override
  public String getString(final String parameterName) throws SQLException {
    return stmt.getString(parameterName);
  }

  @Override
  public boolean getBoolean(final String parameterName) throws SQLException {
    return stmt.getBoolean(parameterName);
  }

  @Override
  public byte getByte(final String parameterName) throws SQLException {
    return stmt.getByte(parameterName);
  }

  @Override
  public short getShort(final String parameterName) throws SQLException {
    return stmt.getShort(parameterName);
  }

  @Override
  public int getInt(final String parameterName) throws SQLException {
    return stmt.getInt(parameterName);
  }

  @Override
  public long getLong(final String parameterName) throws SQLException {
    return stmt.getLong(parameterName);
  }

  @Override
  public float getFloat(final String parameterName) throws SQLException {
    return stmt.getFloat(parameterName);
  }

  @Override
  public double getDouble(final String parameterName) throws SQLException {
    return stmt.getDouble(parameterName);
  }

  @Override
  public byte[] getBytes(final String parameterName) throws SQLException {
    return stmt.getBytes(parameterName);
  }

  @Override
  public Date getDate(final String parameterName) throws SQLException {
    return stmt.getDate(parameterName);
  }

  @Override
  public Time getTime(final String parameterName) throws SQLException {
    return stmt.getTime(parameterName);
  }

  @Override
  public Timestamp getTimestamp(final String parameterName) throws SQLException {
    return stmt.getTimestamp(parameterName);
  }

  @Override
  public Object getObject(final String parameterName) throws SQLException {
    return stmt.getObject(parameterName);
  }

  @Override
  public BigDecimal getBigDecimal(final String parameterName) throws SQLException {
    return stmt.getBigDecimal(parameterName);
  }

  @Override
  public Object getObject(final String parameterName, final Map<String, Class<?>> map) throws SQLException {
    return stmt.getObject(parameterName, map);
  }

  @Override
  public Ref getRef(final String parameterName) throws SQLException {
    return stmt.getRef(parameterName);
  }

  @Override
  public Blob getBlob(final String parameterName) throws SQLException {
    return stmt.getBlob(parameterName);
  }

  @Override
  public Clob getClob(final String parameterName) throws SQLException {
    return stmt.getClob(parameterName);
  }

  @Override
  public Array getArray(final String parameterName) throws SQLException {
    return stmt.getArray(parameterName);
  }

  @Override
  public Date getDate(final String parameterName, final Calendar cal) throws SQLException {
    return stmt.getDate(parameterName, cal);
  }

  @Override
  public Time getTime(final String parameterName, final Calendar cal) throws SQLException {
    return stmt.getTime(parameterName, cal);
  }

  @Override
  public Timestamp getTimestamp(final String parameterName, final Calendar cal) throws SQLException {
    return stmt.getTimestamp(parameterName, cal);
  }

  @Override
  public URL getURL(final String parameterName) throws SQLException {
    return stmt.getURL(parameterName);
  }

  @Override
  public RowId getRowId(final int parameterIndex) throws SQLException {
    return stmt.getRowId(parameterIndex);
  }

  @Override
  public RowId getRowId(final String parameterName) throws SQLException {
    return stmt.getRowId(parameterName);
  }

  @Override
  public void setRowId(final String parameterName, final RowId x) throws SQLException {
    stmt.setRowId(parameterName, x);
  }

  @Override
  public void setNString(final String parameterName, final String value) throws SQLException {
    stmt.setNString(parameterName, value);
  }

  @Override
  public void setNCharacterStream(final String parameterName, final Reader value, final long length) throws SQLException {
    stmt.setNCharacterStream(parameterName, value, length);
  }

  @Override
  public void setNClob(final String parameterName, final NClob value) throws SQLException {
    stmt.setNClob(parameterName, value);
  }

  @Override
  public void setClob(final String parameterName, final Reader reader, final long length) throws SQLException {
    stmt.setClob(parameterName, reader, length);
  }

  @Override
  public void setBlob(final String parameterName, final InputStream inputStream, final long length) throws SQLException {
    stmt.setBlob(parameterName, inputStream, length);
  }

  @Override
  public void setNClob(final String parameterName, final Reader reader, final long length) throws SQLException {
    stmt.setNClob(parameterName, reader, length);
  }

  @Override
  public NClob getNClob(final int parameterIndex) throws SQLException {
    return stmt.getNClob(parameterIndex);
  }

  @Override
  public NClob getNClob(final String parameterName) throws SQLException {
    return stmt.getNClob(parameterName);
  }

  @Override
  public void setSQLXML(final String parameterName, final SQLXML xmlObject) throws SQLException {
    stmt.setSQLXML(parameterName, xmlObject);
  }

  @Override
  public SQLXML getSQLXML(final int parameterIndex) throws SQLException {
    return stmt.getSQLXML(parameterIndex);
  }

  @Override
  public SQLXML getSQLXML(final String parameterName) throws SQLException {
    return stmt.getSQLXML(parameterName);
  }

  @Override
  public String getNString(final int parameterIndex) throws SQLException {
    return stmt.getNString(parameterIndex);
  }

  @Override
  public String getNString(final String parameterName) throws SQLException {
    return stmt.getNString(parameterName);
  }

  @Override
  public Reader getNCharacterStream(final int parameterIndex) throws SQLException {
    return stmt.getNCharacterStream(parameterIndex);
  }

  @Override
  public Reader getNCharacterStream(final String parameterName) throws SQLException {
    return stmt.getNCharacterStream(parameterName);
  }

  @Override
  public Reader getCharacterStream(final int parameterIndex) throws SQLException {
    return stmt.getCharacterStream(parameterIndex);
  }

  @Override
  public Reader getCharacterStream(final String parameterName) throws SQLException {
    return stmt.getCharacterStream(parameterName);
  }

  @Override
  public void setBlob(final String parameterName, final Blob x) throws SQLException {
    stmt.setBlob(parameterName, x);
  }

  @Override
  public void setClob(final String parameterName, final Clob x) throws SQLException {
    stmt.setClob(parameterName, x);
  }

  @Override
  public void setAsciiStream(final String parameterName, final InputStream x, final long length) throws SQLException {
    stmt.setAsciiStream(parameterName, x, length);
  }

  @Override
  public void setBinaryStream(final String parameterName, final InputStream x, final long length) throws SQLException {
    stmt.setBinaryStream(parameterName, x, length);
  }

  @Override
  public void setCharacterStream(final String parameterName, final Reader reader, final long length) throws SQLException {
    stmt.setCharacterStream(parameterName, reader, length);
  }

  @Override
  public void setAsciiStream(final String parameterName, final InputStream x) throws SQLException {
    stmt.setAsciiStream(parameterName, x);
  }

  @Override
  public void setBinaryStream(final String parameterName, final InputStream x) throws SQLException {
    stmt.setBinaryStream(parameterName, x);
  }

  @Override
  public void setCharacterStream(final String parameterName, final Reader reader) throws SQLException {
    stmt.setCharacterStream(parameterName, reader);
  }

  @Override
  public void setNCharacterStream(final String parameterName, final Reader value) throws SQLException {
    stmt.setNCharacterStream(parameterName, value);
  }

  @Override
  public void setClob(final String parameterName, final Reader reader) throws SQLException {
    stmt.setClob(parameterName, reader);
  }

  @Override
  public void setBlob(final String parameterName, final InputStream inputStream) throws SQLException {
    stmt.setBlob(parameterName, inputStream);
  }

  @Override
  public void setNClob(final String parameterName, final Reader reader) throws SQLException {
    stmt.setNClob(parameterName, reader);
  }

  @Override
  public <T> T getObject(final int parameterIndex, final Class<T> type) throws SQLException {
    return stmt.getObject(parameterIndex, type);
  }

  @Override
  public <T> T getObject(final String parameterName, final Class<T> type) throws SQLException {
    return stmt.getObject(parameterName, type);
  }
}
