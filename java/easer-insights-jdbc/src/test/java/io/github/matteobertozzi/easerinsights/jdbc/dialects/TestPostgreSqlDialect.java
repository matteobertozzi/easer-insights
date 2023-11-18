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

import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.jdbc.DbInfo;

public class TestPostgreSqlDialect {
  @Test
  public void testJdbcUrl() {
    final String[] jdbcUrl = new String[] {
      "jdbc:postgresql://localhost/test?user=uzer&password=secret&ssl=true",
      "jdbc:postgresql://localhost:5432/postgres?options=-c%20search_path=test,public,pg_catalog%20-c%20statement_timeout=90000",
      "jdbc:postgresql://host1:port1,host2:port2/database",
      "jdbc:postgresql:aws://10.10.10.10:5432/database",
    };
    for (final String url : jdbcUrl) {
      final DbInfo dbInfo = DbInfo.parseJdbcUrl(url);
      System.out.println(dbInfo.host() + " " + dbInfo.dbName() + " -> " + dbInfo.properties());
    }
  }
}
