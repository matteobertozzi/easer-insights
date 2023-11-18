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

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;
import io.github.matteobertozzi.easerinsights.jdbc.DbType;

public final class OracleDialect {
  private OracleDialect() {
    // no-op
  }

  // ====================================================================================================
  //  Parse JDBC URL
  // ====================================================================================================
  private static final String JDBC_PREFIX = "jdbc:oracle:thin:@//";

  public static DbInfo parseJdbcUrl(final String url) {
    if (url.startsWith(JDBC_PREFIX)) {
      // jdbc:oracle:thin:@//myoracle.db.server:1521/my_servicename
      return DbInfo.parseStandardJdbcUrl(DbType.ORACLE, JDBC_PREFIX, url);
    }
    // TODO: "jdbc:oracle:thin:@myoracle.db.server:1521:my_sid",
    return null;
  }
}
