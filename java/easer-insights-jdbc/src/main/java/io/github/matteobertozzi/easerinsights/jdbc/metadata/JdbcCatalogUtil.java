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

package io.github.matteobertozzi.easerinsights.jdbc.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.matteobertozzi.easerinsights.jdbc.DbType;
import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.strings.StringUtil;

public final class JdbcCatalogUtil {
  private static final Comparator<JdbcTableColumn> TABLE_COLUMN_NAME_COMPARATOR = Comparator.comparing(JdbcTableColumn::name);
  private static final Comparator<JdbcIndexedField> INDEX_FIELD_COMPARATOR = Comparator.comparingInt(JdbcIndexedField::position);

  private JdbcCatalogUtil() {
    // no-op
  }

  public static Set<String> getCatalogs(final DatabaseMetaData metaData) throws SQLException {
    try (ResultSet rs = metaData.getCatalogs()) {
      final HashSet<String> catalogNames = new HashSet<>();
      while (rs.next()) {
        final String catalogName = rs.getString("TABLE_CAT");
        catalogNames.add(catalogName);
      }
      return catalogNames;
    }
  }

  private static final String[] TABLE_TYPES = new String[] { "TABLE", "VIEW", "BASE TABLE" };
  private static final Set<String> SYS_TABLE_SCHEM = Set.of("sys", "INFORMATION_SCHEMA");
  public static List<JdbcTableSchema> getTables(final DbType dbType, final DatabaseMetaData metaData, final String catalogName) throws SQLException {
    final ArrayList<JdbcTableSchema> tables = new ArrayList<>();
    try (ResultSet rs = metaData.getTables(catalogName, null, null, TABLE_TYPES)) {
      while (rs.next()) {
        final JdbcTableType tableType = parseTableType(rs.getString("TABLE_TYPE"));
        final String tableName = rs.getString("TABLE_NAME");
        final String tableSchema = rs.getString("TABLE_SCHEM");

        if (tableSchema != null && SYS_TABLE_SCHEM.contains(tableSchema)) {
          Logger.debug("ignore system {}: {} {}", tableType, tableSchema, tableName);
          continue;
        }

        tables.add(new JdbcTableSchema(tableType, tableSchema, tableName));
      }
    }

    for (final JdbcTableSchema schema: tables) {
      //System.out.println(schema.tableName());
      fetchTableColumns(schema.columns(), metaData, catalogName, schema.tableSchema(), schema.tableName());
      fetchPrimaryKeys(schema.primaryKey(), metaData, catalogName, schema.tableSchema(), schema.tableName());
      fetchIndexedFields(schema.indexes(), metaData, catalogName, schema.tableSchema(), schema.tableName());
    }
    return tables;
  }

  public static JdbcTableSchema getTable(final DbType dbType, final DatabaseMetaData metaData, final String catalogName, final String tableName) throws SQLException {
    try (ResultSet rs = metaData.getTables(catalogName, null, tableName, TABLE_TYPES)) {
      if (!rs.next()) return null;

      final JdbcTableType tableType = parseTableType(rs.getString("TABLE_TYPE"));
      final String tableSchema = rs.getString("TABLE_SCHEM");

      final JdbcTableSchema schema = new JdbcTableSchema(tableType, tableSchema, tableName);
      fetchTableColumns(schema.columns(), metaData, catalogName, schema.tableSchema(), schema.tableName());
      fetchPrimaryKeys(schema.primaryKey(), metaData, catalogName, schema.tableSchema(), schema.tableName());
      fetchIndexedFields(schema.indexes(), metaData, catalogName, schema.tableSchema(), schema.tableName());
      return schema;
    }
  }

  private static void fetchTableColumns(final List<JdbcTableColumn> columns, final DatabaseMetaData metadata,
      final String catalog, final String schema, final String tableName) throws SQLException {
    try (ResultSet rs = metadata.getColumns(catalog, schema, tableName, null)) {
      final int columnNameIndex = rs.findColumn("COLUMN_NAME");
      final int dataTypeIndex = rs.findColumn("DATA_TYPE");
      final int nullableIndex = rs.findColumn("NULLABLE");
      final int autoIncIndex = rs.findColumn("IS_AUTOINCREMENT");
      final int columnSizeIndex = rs.findColumn("COLUMN_SIZE");
      //final int columnDefIndex = rs.findColumn("COLUMN_DEF");
      while (rs.next()) {
        final String name = rs.getString(columnNameIndex);
        final int sqlType = rs.getInt(dataTypeIndex);
        final boolean nullable = rs.getInt(nullableIndex) != ResultSetMetaData.columnNoNulls;
        final boolean autoIncrement = StringUtil.equalsIgnoreCase("YES", rs.getString(autoIncIndex));
        final int columnSize = rs.getInt(columnSizeIndex);
        //final String defaultValue = rs.getString(columnDefIndex);

        /*
        System.out.println(" -> " + name
                         + " -> type:" + JdbcTypeUtil.sqlTypeName(sqlType)
                         + " -> nullable:" + nullable
                         + " -> autoInc:" + autoIncrement
                         + " -> size:" + columnSize);
        */
        columns.add(new JdbcTableColumn(name, sqlType, columnSize, nullable, autoIncrement));
      }
      columns.sort(TABLE_COLUMN_NAME_COMPARATOR);
    }
  }

  private static void fetchPrimaryKeys(final List<JdbcIndexedField> keys, final DatabaseMetaData meta,
      final String catalog, final String schema, final String tableName) throws SQLException {
    try (final ResultSet rs = meta.getPrimaryKeys(catalog, schema, tableName)) {
      if (!rs.next()) return;

      final int keySeqIndex = rs.findColumn("KEY_SEQ");
      final int columnNameIndex = rs.findColumn("COLUMN_NAME");
      do {
        keys.add(new JdbcIndexedField(rs.getShort(keySeqIndex), rs.getString(columnNameIndex)));
      } while (rs.next());
      keys.sort(INDEX_FIELD_COMPARATOR);
    }
  }

  private static void fetchIndexedFields(final List<JdbcIndex> indexes, final DatabaseMetaData meta,
      final String catalog, final String schema, final String tableName) throws SQLException {
    try (final ResultSet rs = meta.getIndexInfo(catalog, schema, tableName, false, false)) {
      if (!rs.next()) return;

      final int nameIndex = rs.findColumn("INDEX_NAME");
      final int seqIndex = rs.findColumn("ORDINAL_POSITION");
      final int columnNameIndex = rs.findColumn("COLUMN_NAME");
      final int nonUniqueIndex = rs.findColumn("NON_UNIQUE");
      final int typeIndex = rs.findColumn("TYPE");

      final HashMap<String, JdbcIndex> indexMap = new HashMap<>();
      do {
        final String indexName = rs.getString(nameIndex);
        if (StringUtil.isEmpty(indexName)) {
          Logger.warn("skipping {table} index {type} does not have a name.", tableName, rs.getShort(typeIndex));
          continue;
        }

        JdbcIndex index = indexMap.get(indexName);
        if (index == null) {
          index = new JdbcIndex(indexName, rs.getShort(typeIndex), !rs.getBoolean(nonUniqueIndex));
          indexMap.put(indexName, index);
        }

        index.fields().add(new JdbcIndexedField(rs.getShort(seqIndex), rs.getString(columnNameIndex)));
      } while (rs.next());

      for (final JdbcIndex index: indexMap.values()) {
        index.fields().sort(INDEX_FIELD_COMPARATOR);
        indexes.add(index);
      }
    } catch (final SQLFeatureNotSupportedException e) {
      Logger.warn("unsupported feature {}", e.getMessage());
    }
  }

  public static List<JdbcProcedureField> fetchProcedureParams(final DatabaseMetaData metadata,
        final String catalog, final String schema, final String procName) throws SQLException {
    List<JdbcProcedureField> params = fetchProcedureColumns(metadata, catalog, schema, procName);
    if (params != null) return params;

    Logger.warn("fallback on no-params stored procedures for {} {}", schema, procName);
    params = fetchProcedureWithoutColumns(metadata, catalog, schema, procName);
    if (params != null) return params;

    throw new SQLException("store procedure schema=" + schema + " name=" + procName + " not found");
  }

  private static List<JdbcProcedureField> fetchProcedureColumns(final DatabaseMetaData metadata,
        final String catalog, final String schema, final String procName) throws SQLException {
    try (ResultSet rs = metadata.getProcedureColumns(catalog, schema, procName, null)) {
      if (rs == null || !rs.next()) {
        return null;
      }

      final int nameIndex = rs.findColumn("COLUMN_NAME");
      final int seqIndex = rs.findColumn("ORDINAL_POSITION");
      final int columnTypeIndex = rs.findColumn("COLUMN_TYPE");
      final int dataTypeIndex = rs.findColumn("DATA_TYPE");
      final int nullableIndex = rs.findColumn("NULLABLE");

      final int procedureCatIndex = rs.findColumn("PROCEDURE_CAT");
      final int procedureSchemIndex = rs.findColumn("PROCEDURE_SCHEM");
      final int procedureNameIndex = rs.findColumn("PROCEDURE_NAME");

      String key;
      final HashMap<String, ArrayList<JdbcProcedureField>> procedures = new HashMap<>();
      do {
        key = String.format("%s.%s.%s()", rs.getString(procedureCatIndex), rs.getString(procedureSchemIndex), rs.getString(procedureNameIndex));

        final String name = rs.getString(nameIndex);
        final int seq = rs.getInt(seqIndex);
        final int type = rs.getShort(columnTypeIndex);
        final boolean nullable = rs.getShort(nullableIndex) != ResultSetMetaData.columnNoNulls;
        final int dataType = rs.getInt(dataTypeIndex);

        procedures.computeIfAbsent(key, k -> new ArrayList<>()).add(new JdbcProcedureField(name, seq, type, dataType, nullable));
      } while (rs.next());

      if (procedures.size() > 1) {
        throw new SQLException("found multiple stored procedures: " + procedures.keySet());
      }

      return procedures.get(key);
    }
  }

  private static List<JdbcProcedureField> fetchProcedureWithoutColumns(final DatabaseMetaData metadata,
        final String catalog, final String schema, final String procName) throws SQLException {
    try (ResultSet rs = metadata.getProcedures(catalog, schema, procName)) {
      if (rs == null || !rs.next()) {
        return null;
      }

      switch (rs.getShort("PROCEDURE_TYPE")) {
        case DatabaseMetaData.procedureNoResult:
          return List.of();
        case DatabaseMetaData.procedureResultUnknown:
        case DatabaseMetaData.procedureReturnsResult:
          return List.of(new JdbcProcedureField(procName, 1, DatabaseMetaData.functionReturn, Types.OTHER, true));
      }
      return null;
    }
  }

  public record JdbcProcedureField (String name, int index, int type, int jdbcType, boolean isNullable) {}

  public record JdbcTableSchema(JdbcTableType tableType, String tableSchema, String tableName,
      List<JdbcTableColumn> columns, List<JdbcIndexedField> primaryKey, List<JdbcIndex> indexes) {
    private JdbcTableSchema(final JdbcTableType tableType, final String tableSchema, final String tableName) {
      this(tableType, tableSchema, tableName, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public JdbcTableColumn column(final String name) {
      for (final JdbcTableColumn column: columns) {
        if (column.name().equals(name)) {
          return column;
        }
      }
      return null;
    }
  }

  public record JdbcTableColumn(String name, int sqlType, int columnSize, boolean nullable, boolean autoIncrement) {}

  public record JdbcFieldInfo (String name, int column, int jdbcType, boolean isAutoIncrement, boolean isNullable, int precision, int scale) {
    public static JdbcFieldInfo fromMetadata(final ResultSetMetaData rsmd, final int column) throws SQLException {
      final String name = rsmd.getColumnName(column);
      final int jdbcType = rsmd.getColumnType(column);
      final boolean isAutoIncrement = rsmd.isAutoIncrement(column);
      final boolean isNullable = rsmd.isNullable(column) != ResultSetMetaData.columnNoNulls;
      final int precision = rsmd.getPrecision(column);
      final int scale = rsmd.getScale(column);
      return new JdbcFieldInfo(name, column, jdbcType, isAutoIncrement, isNullable, precision, scale);
    }

    public static JdbcFieldInfo[] fromMetadata(final ResultSetMetaData rsmd) throws SQLException {
      final JdbcFieldInfo[] fields = new JdbcFieldInfo[rsmd.getColumnCount()];
      for (int i = 0; i < fields.length; ++i) {
        fields[i] = fromMetadata(rsmd, i + 1);
      }
      return fields;
    }
  }

  public record JdbcIndexedField (int position, String name) {}
  public record JdbcIndex (String name, JdbcIndexType type, boolean unique, List<JdbcIndexedField> fields) {
    private JdbcIndex(final String name, final int jdbcType, final boolean unique) {
      this(name, parseIndexType(jdbcType), unique, new ArrayList<>());
    }
  }

  public enum JdbcTableType { TABLE, VIEW, SYSTEM_TABLE, GLOBAL_TEMPORARY, LOCAL_TEMPORARY, ALIAS, SYNONYM, BASE_TABLE }
  private static JdbcTableType parseTableType(final String tableType) {
    try {
      final String type = tableType.replace(' ', '_');
      return JdbcTableType.valueOf(type);
    } catch (final IllegalArgumentException e) {
      Logger.error(e, "unknown table type {}", tableType);
      throw e;
    }
  }

  public enum JdbcIndexType { CLUSTERED, HASHED, STATISTICS, OTHER }
  private static JdbcIndexType parseIndexType(final int indexType) {
    return switch (indexType) {
      case DatabaseMetaData.tableIndexClustered -> JdbcIndexType.CLUSTERED;
      case DatabaseMetaData.tableIndexHashed -> JdbcIndexType.HASHED;
      case DatabaseMetaData.tableIndexStatistic -> JdbcIndexType.STATISTICS;
      case DatabaseMetaData.tableIndexOther -> JdbcIndexType.OTHER;
      default -> throw new UnsupportedOperationException("unsupported index type: " + indexType);
    };
  }
}
