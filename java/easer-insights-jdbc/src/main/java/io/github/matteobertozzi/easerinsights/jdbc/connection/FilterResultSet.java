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
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class FilterResultSet implements ResultSet {
  private final ResultSet rs;

  public FilterResultSet(final ResultSet rs) {
    this.rs = rs;
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    return rs.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return rs.isWrapperFor(iface);
  }

  @Override
  public boolean next() throws SQLException {
    return rs.next();
  }

  @Override
  public void close() throws SQLException {
    rs.close();
  }

  @Override
  public boolean wasNull() throws SQLException {
    return rs.wasNull();
  }

  @Override
  public String getString(final int columnIndex) throws SQLException {
    return rs.getString(columnIndex);
  }

  @Override
  public boolean getBoolean(final int columnIndex) throws SQLException {
    return rs.getBoolean(columnIndex);
  }

  @Override
  public byte getByte(final int columnIndex) throws SQLException {
    return rs.getByte(columnIndex);
  }

  @Override
  public short getShort(final int columnIndex) throws SQLException {
    return rs.getShort(columnIndex);
  }

  @Override
  public int getInt(final int columnIndex) throws SQLException {
    return rs.getInt(columnIndex);
  }

  @Override
  public long getLong(final int columnIndex) throws SQLException {
    return rs.getLong(columnIndex);
  }

  @Override
  public float getFloat(final int columnIndex) throws SQLException {
    return rs.getFloat(columnIndex);
  }

  @Override
  public double getDouble(final int columnIndex) throws SQLException {
    return rs.getDouble(columnIndex);
  }

  @Override
  @Deprecated
  public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
    return rs.getBigDecimal(columnIndex, scale);
  }

  @Override
  public byte[] getBytes(final int columnIndex) throws SQLException {
    return rs.getBytes(columnIndex);
  }

  @Override
  public Date getDate(final int columnIndex) throws SQLException {
    return rs.getDate(columnIndex);
  }

  @Override
  public Time getTime(final int columnIndex) throws SQLException {
    return rs.getTime(columnIndex);
  }

  @Override
  public Timestamp getTimestamp(final int columnIndex) throws SQLException {
    return rs.getTimestamp(columnIndex);
  }

  @Override
  public InputStream getAsciiStream(final int columnIndex) throws SQLException {
    return rs.getAsciiStream(columnIndex);
  }

  @Override
  @Deprecated
  public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
    return rs.getUnicodeStream(columnIndex);
  }

  @Override
  public InputStream getBinaryStream(final int columnIndex) throws SQLException {
    return rs.getBinaryStream(columnIndex);
  }

  @Override
  public String getString(final String columnLabel) throws SQLException {
    return rs.getString(columnLabel);
  }

  @Override
  public boolean getBoolean(final String columnLabel) throws SQLException {
    return rs.getBoolean(columnLabel);
  }

  @Override
  public byte getByte(final String columnLabel) throws SQLException {
    return rs.getByte(columnLabel);
  }

  @Override
  public short getShort(final String columnLabel) throws SQLException {
    return rs.getShort(columnLabel);
  }

  @Override
  public int getInt(final String columnLabel) throws SQLException {
    return rs.getInt(columnLabel);
  }

  @Override
  public long getLong(final String columnLabel) throws SQLException {
    return rs.getLong(columnLabel);
  }

  @Override
  public float getFloat(final String columnLabel) throws SQLException {
    return rs.getFloat(columnLabel);
  }

  @Override
  public double getDouble(final String columnLabel) throws SQLException {
    return rs.getDouble(columnLabel);
  }

  @Override
  @Deprecated
  public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
    return rs.getBigDecimal(columnLabel, scale);
  }

  @Override
  public byte[] getBytes(final String columnLabel) throws SQLException {
    return rs.getBytes(columnLabel);
  }

  @Override
  public Date getDate(final String columnLabel) throws SQLException {
    return rs.getDate(columnLabel);
  }

  @Override
  public Time getTime(final String columnLabel) throws SQLException {
    return rs.getTime(columnLabel);
  }

  @Override
  public Timestamp getTimestamp(final String columnLabel) throws SQLException {
    return rs.getTimestamp(columnLabel);
  }

  @Override
  public InputStream getAsciiStream(final String columnLabel) throws SQLException {
    return rs.getAsciiStream(columnLabel);
  }

  @Override
  @Deprecated
  public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
    return rs.getUnicodeStream(columnLabel);
  }

  @Override
  public InputStream getBinaryStream(final String columnLabel) throws SQLException {
    return rs.getBinaryStream(columnLabel);
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return rs.getWarnings();
  }

  @Override
  public void clearWarnings() throws SQLException {
    rs.clearWarnings();
  }

  @Override
  public String getCursorName() throws SQLException {
    return rs.getCursorName();
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return rs.getMetaData();
  }

  @Override
  public Object getObject(final int columnIndex) throws SQLException {
    return rs.getObject(columnIndex);
  }

  @Override
  public Object getObject(final String columnLabel) throws SQLException {
    return rs.getObject(columnLabel);
  }

  @Override
  public int findColumn(final String columnLabel) throws SQLException {
    return rs.findColumn(columnLabel);
  }

  @Override
  public Reader getCharacterStream(final int columnIndex) throws SQLException {
    return rs.getCharacterStream(columnIndex);
  }

  @Override
  public Reader getCharacterStream(final String columnLabel) throws SQLException {
    return rs.getCharacterStream(columnLabel);
  }

  @Override
  public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
    return rs.getBigDecimal(columnIndex);
  }

  @Override
  public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
    return rs.getBigDecimal(columnLabel);
  }

  @Override
  public boolean isBeforeFirst() throws SQLException {
    return rs.isBeforeFirst();
  }

  @Override
  public boolean isAfterLast() throws SQLException {
    return rs.isAfterLast();
  }

  @Override
  public boolean isFirst() throws SQLException {
    return rs.isFirst();
  }

  @Override
  public boolean isLast() throws SQLException {
    return rs.isLast();
  }

  @Override
  public void beforeFirst() throws SQLException {
    rs.beforeFirst();
  }

  @Override
  public void afterLast() throws SQLException {
    rs.afterLast();
  }

  @Override
  public boolean first() throws SQLException {
    return rs.first();
  }

  @Override
  public boolean last() throws SQLException {
    return rs.last();
  }

  @Override
  public int getRow() throws SQLException {
    return rs.getRow();
  }

  @Override
  public boolean absolute(final int row) throws SQLException {
    return rs.absolute(row);
  }

  @Override
  public boolean relative(final int rows) throws SQLException {
    return rs.relative(rows);
  }

  @Override
  public boolean previous() throws SQLException {
    return rs.previous();
  }

  @Override
  public void setFetchDirection(final int direction) throws SQLException {
    rs.setFetchDirection(direction);
  }

  @Override
  public int getFetchDirection() throws SQLException {
    return rs.getFetchDirection();
  }

  @Override
  public void setFetchSize(final int rows) throws SQLException {
    rs.setFetchSize(rows);
  }

  @Override
  public int getFetchSize() throws SQLException {
    return rs.getFetchSize();
  }

  @Override
  public int getType() throws SQLException {
    return rs.getType();
  }

  @Override
  public int getConcurrency() throws SQLException {
    return rs.getConcurrency();
  }

  @Override
  public boolean rowUpdated() throws SQLException {
    return rs.rowUpdated();
  }

  @Override
  public boolean rowInserted() throws SQLException {
    return rs.rowInserted();
  }

  @Override
  public boolean rowDeleted() throws SQLException {
    return rs.rowDeleted();
  }

  @Override
  public void updateNull(final int columnIndex) throws SQLException {
    rs.updateNull(columnIndex);
  }

  @Override
  public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
    rs.updateBoolean(columnIndex, x);
  }

  @Override
  public void updateByte(final int columnIndex, final byte x) throws SQLException {
    rs.updateByte(columnIndex, x);
  }

  @Override
  public void updateShort(final int columnIndex, final short x) throws SQLException {
    rs.updateShort(columnIndex, x);
  }

  @Override
  public void updateInt(final int columnIndex, final int x) throws SQLException {
    rs.updateInt(columnIndex, x);
  }

  @Override
  public void updateLong(final int columnIndex, final long x) throws SQLException {
    rs.updateLong(columnIndex, x);
  }

  @Override
  public void updateFloat(final int columnIndex, final float x) throws SQLException {
    rs.updateFloat(columnIndex, x);
  }

  @Override
  public void updateDouble(final int columnIndex, final double x) throws SQLException {
    rs.updateDouble(columnIndex, x);
  }

  @Override
  public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
    rs.updateBigDecimal(columnIndex, x);
  }

  @Override
  public void updateString(final int columnIndex, final String x) throws SQLException {
    rs.updateString(columnIndex, x);
  }

  @Override
  public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
    rs.updateBytes(columnIndex, x);
  }

  @Override
  public void updateDate(final int columnIndex, final Date x) throws SQLException {
    rs.updateDate(columnIndex, x);
  }

  @Override
  public void updateTime(final int columnIndex, final Time x) throws SQLException {
    rs.updateTime(columnIndex, x);
  }

  @Override
  public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
    rs.updateTimestamp(columnIndex, x);
  }

  @Override
  public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
    rs.updateAsciiStream(columnIndex, x, length);
  }

  @Override
  public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
    rs.updateBinaryStream(columnIndex, x, length);
  }

  @Override
  public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
    rs.updateCharacterStream(columnIndex, x, length);
  }

  @Override
  public void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {
    rs.updateObject(columnIndex, x, scaleOrLength);
  }

  @Override
  public void updateObject(final int columnIndex, final Object x) throws SQLException {
    rs.updateObject(columnIndex, x);
  }

  @Override
  public void updateNull(final String columnLabel) throws SQLException {
    rs.updateNull(columnLabel);
  }

  @Override
  public void updateBoolean(final String columnLabel, final boolean x) throws SQLException {
    rs.updateBoolean(columnLabel, x);
  }

  @Override
  public void updateByte(final String columnLabel, final byte x) throws SQLException {
    rs.updateByte(columnLabel, x);
  }

  @Override
  public void updateShort(final String columnLabel, final short x) throws SQLException {
    rs.updateShort(columnLabel, x);
  }

  @Override
  public void updateInt(final String columnLabel, final int x) throws SQLException {
    rs.updateInt(columnLabel, x);
  }

  @Override
  public void updateLong(final String columnLabel, final long x) throws SQLException {
    rs.updateLong(columnLabel, x);
  }

  @Override
  public void updateFloat(final String columnLabel, final float x) throws SQLException {
    rs.updateFloat(columnLabel, x);
  }

  @Override
  public void updateDouble(final String columnLabel, final double x) throws SQLException {
    rs.updateDouble(columnLabel, x);
  }

  @Override
  public void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {
    rs.updateBigDecimal(columnLabel, x);
  }

  @Override
  public void updateString(final String columnLabel, final String x) throws SQLException {
    rs.updateString(columnLabel, x);
  }

  @Override
  public void updateBytes(final String columnLabel, final byte[] x) throws SQLException {
    rs.updateBytes(columnLabel, x);
  }

  @Override
  public void updateDate(final String columnLabel, final Date x) throws SQLException {
    rs.updateDate(columnLabel, x);
  }

  @Override
  public void updateTime(final String columnLabel, final Time x) throws SQLException {
    rs.updateTime(columnLabel, x);
  }

  @Override
  public void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {
    rs.updateTimestamp(columnLabel, x);
  }

  @Override
  public void updateAsciiStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
    rs.updateAsciiStream(columnLabel, x, length);
  }

  @Override
  public void updateBinaryStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
    rs.updateBinaryStream(columnLabel, x, length);
  }

  @Override
  public void updateCharacterStream(final String columnLabel, final Reader reader, final int length) throws SQLException {
    rs.updateCharacterStream(columnLabel, reader, length);
  }

  @Override
  public void updateObject(final String columnLabel, final Object x, final int scaleOrLength) throws SQLException {
    rs.updateObject(columnLabel, x, scaleOrLength);
  }

  @Override
  public void updateObject(final String columnLabel, final Object x) throws SQLException {
    rs.updateObject(columnLabel, x);
  }

  @Override
  public void insertRow() throws SQLException {
    rs.insertRow();
  }

  @Override
  public void updateRow() throws SQLException {
    rs.updateRow();
  }

  @Override
  public void deleteRow() throws SQLException {
    rs.deleteRow();
  }

  @Override
  public void refreshRow() throws SQLException {
    rs.refreshRow();
  }

  @Override
  public void cancelRowUpdates() throws SQLException {
    rs.cancelRowUpdates();
  }

  @Override
  public void moveToInsertRow() throws SQLException {
    rs.moveToInsertRow();
  }

  @Override
  public void moveToCurrentRow() throws SQLException {
    rs.moveToCurrentRow();
  }

  @Override
  public Statement getStatement() throws SQLException {
    return rs.getStatement();
  }

  @Override
  public Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
    return rs.getObject(columnIndex, map);
  }

  @Override
  public Ref getRef(final int columnIndex) throws SQLException {
    return rs.getRef(columnIndex);
  }

  @Override
  public Blob getBlob(final int columnIndex) throws SQLException {
    return rs.getBlob(columnIndex);
  }

  @Override
  public Clob getClob(final int columnIndex) throws SQLException {
    return rs.getClob(columnIndex);
  }

  @Override
  public Array getArray(final int columnIndex) throws SQLException {
    return rs.getArray(columnIndex);
  }

  @Override
  public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
    return rs.getObject(columnLabel, map);
  }

  @Override
  public Ref getRef(final String columnLabel) throws SQLException {
    return rs.getRef(columnLabel);
  }

  @Override
  public Blob getBlob(final String columnLabel) throws SQLException {
    return rs.getBlob(columnLabel);
  }

  @Override
  public Clob getClob(final String columnLabel) throws SQLException {
    return rs.getClob(columnLabel);
  }

  @Override
  public Array getArray(final String columnLabel) throws SQLException {
    return rs.getArray(columnLabel);
  }

  @Override
  public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
    return rs.getDate(columnIndex, cal);
  }

  @Override
  public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
    return rs.getDate(columnLabel, cal);
  }

  @Override
  public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
    return rs.getTime(columnIndex, cal);
  }

  @Override
  public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
    return rs.getTime(columnLabel, cal);
  }

  @Override
  public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
    return rs.getTimestamp(columnIndex, cal);
  }

  @Override
  public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
    return rs.getTimestamp(columnLabel, cal);
  }

  @Override
  public URL getURL(final int columnIndex) throws SQLException {
    return rs.getURL(columnIndex);
  }

  @Override
  public URL getURL(final String columnLabel) throws SQLException {
    return rs.getURL(columnLabel);
  }

  @Override
  public void updateRef(final int columnIndex, final Ref x) throws SQLException {
    rs.updateRef(columnIndex, x);
  }

  @Override
  public void updateRef(final String columnLabel, final Ref x) throws SQLException {
    rs.updateRef(columnLabel, x);
  }

  @Override
  public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
    rs.updateBlob(columnIndex, x);
  }

  @Override
  public void updateBlob(final String columnLabel, final Blob x) throws SQLException {
    rs.updateBlob(columnLabel, x);
  }

  @Override
  public void updateClob(final int columnIndex, final Clob x) throws SQLException {
    rs.updateClob(columnIndex, x);
  }

  @Override
  public void updateClob(final String columnLabel, final Clob x) throws SQLException {
    rs.updateClob(columnLabel, x);
  }

  @Override
  public void updateArray(final int columnIndex, final Array x) throws SQLException {
    rs.updateArray(columnIndex, x);
  }

  @Override
  public void updateArray(final String columnLabel, final Array x) throws SQLException {
    rs.updateArray(columnLabel, x);
  }

  @Override
  public RowId getRowId(final int columnIndex) throws SQLException {
    return rs.getRowId(columnIndex);
  }

  @Override
  public RowId getRowId(final String columnLabel) throws SQLException {
    return rs.getRowId(columnLabel);
  }

  @Override
  public void updateRowId(final int columnIndex, final RowId x) throws SQLException {
    rs.updateRowId(columnIndex, x);
  }

  @Override
  public void updateRowId(final String columnLabel, final RowId x) throws SQLException {
    rs.updateRowId(columnLabel, x);
  }

  @Override
  public int getHoldability() throws SQLException {
    return rs.getHoldability();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return rs.isClosed();
  }

  @Override
  public void updateNString(final int columnIndex, final String nString) throws SQLException {
    rs.updateNString(columnIndex, nString);
  }

  @Override
  public void updateNString(final String columnLabel, final String nString) throws SQLException {
    rs.updateNString(columnLabel, nString);
  }

  @Override
  public void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {
    rs.updateNClob(columnIndex, nClob);
  }

  @Override
  public void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {
    rs.updateNClob(columnLabel, nClob);
  }

  @Override
  public NClob getNClob(final int columnIndex) throws SQLException {
    return rs.getNClob(columnIndex);
  }

  @Override
  public NClob getNClob(final String columnLabel) throws SQLException {
    return rs.getNClob(columnLabel);
  }

  @Override
  public SQLXML getSQLXML(final int columnIndex) throws SQLException {
    return rs.getSQLXML(columnIndex);
  }

  @Override
  public SQLXML getSQLXML(final String columnLabel) throws SQLException {
    return rs.getSQLXML(columnLabel);
  }

  @Override
  public void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {
    rs.updateSQLXML(columnIndex, xmlObject);
  }

  @Override
  public void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {
    rs.updateSQLXML(columnLabel, xmlObject);
  }

  @Override
  public String getNString(final int columnIndex) throws SQLException {
    return rs.getNString(columnIndex);
  }

  @Override
  public String getNString(final String columnLabel) throws SQLException {
    return rs.getNString(columnLabel);
  }

  @Override
  public Reader getNCharacterStream(final int columnIndex) throws SQLException {
    return rs.getNCharacterStream(columnIndex);
  }

  @Override
  public Reader getNCharacterStream(final String columnLabel) throws SQLException {
    return rs.getNCharacterStream(columnLabel);
  }

  @Override
  public void updateNCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
    rs.updateNCharacterStream(columnIndex, x, length);
  }

  @Override
  public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
    rs.updateNCharacterStream(columnLabel, reader, length);
  }

  @Override
  public void updateAsciiStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
    rs.updateAsciiStream(columnIndex, x, length);
  }

  @Override
  public void updateBinaryStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
    rs.updateBinaryStream(columnIndex, x, length);
  }

  @Override
  public void updateCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
    rs.updateCharacterStream(columnIndex, x, length);
  }

  @Override
  public void updateAsciiStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
    rs.updateAsciiStream(columnLabel, x, length);
  }

  @Override
  public void updateBinaryStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
    rs.updateBinaryStream(columnLabel, x, length);
  }

  @Override
  public void updateCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
    rs.updateCharacterStream(columnLabel, reader, length);
  }

  @Override
  public void updateBlob(final int columnIndex, final InputStream inputStream, final long length) throws SQLException {
    rs.updateBlob(columnIndex, inputStream, length);
  }

  @Override
  public void updateBlob(final String columnLabel, final InputStream inputStream, final long length) throws SQLException {
    rs.updateBlob(columnLabel, inputStream, length);
  }

  @Override
  public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
    rs.updateClob(columnIndex, reader, length);
  }

  @Override
  public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
    rs.updateClob(columnLabel, reader, length);
  }

  @Override
  public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
    rs.updateNClob(columnIndex, reader, length);
  }

  @Override
  public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
    rs.updateNClob(columnLabel, reader, length);
  }

  @Override
  public void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {
    rs.updateNCharacterStream(columnIndex, x);
  }

  @Override
  public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
    rs.updateNCharacterStream(columnLabel, reader);
  }

  @Override
  public void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {
    rs.updateAsciiStream(columnIndex, x);
  }

  @Override
  public void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {
    rs.updateBinaryStream(columnIndex, x);
  }

  @Override
  public void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {
    rs.updateCharacterStream(columnIndex, x);
  }

  @Override
  public void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {
    rs.updateAsciiStream(columnLabel, x);
  }

  @Override
  public void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {
    rs.updateBinaryStream(columnLabel, x);
  }

  @Override
  public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
    rs.updateCharacterStream(columnLabel, reader);
  }

  @Override
  public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
    rs.updateBlob(columnIndex, inputStream);
  }

  @Override
  public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
    rs.updateBlob(columnLabel, inputStream);
  }

  @Override
  public void updateClob(final int columnIndex, final Reader reader) throws SQLException {
    rs.updateClob(columnIndex, reader);
  }

  @Override
  public void updateClob(final String columnLabel, final Reader reader) throws SQLException {
    rs.updateClob(columnLabel, reader);
  }

  @Override
  public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
    rs.updateNClob(columnIndex, reader);
  }

  @Override
  public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
    rs.updateNClob(columnLabel, reader);
  }

  @Override
  public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
    return rs.getObject(columnIndex, type);
  }

  @Override
  public <T> T getObject(final String columnLabel, final Class<T> type) throws SQLException {
    return rs.getObject(columnLabel, type);
  }
}
