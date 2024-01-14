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

package io.github.matteobertozzi.easerinsights;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.metrics.MetricDefinition;
import io.github.matteobertozzi.easerinsights.metrics.MetricDefinitionUtil;
import io.github.matteobertozzi.easerinsights.metrics.MetricKey;
import io.github.matteobertozzi.easerinsights.metrics.MetricKeyUtil;

public class TestMetricKeyUtil {
  @Test
  public void testKeyEquals() {
    final MetricKey a = MetricKeyUtil.newMetricKey("test_foo");
    final MetricKey b = MetricKeyUtil.newMetricKey("test_bar");
    final MetricKey c = MetricKeyUtil.newMetricKey("test_foo", new String[] { "x", "y" }, new String[] { "X1", "Y1" });
    final MetricKey d = MetricKeyUtil.newMetricKey("test_bar", new String[] { "x", "y" }, new String[] { "X1", "Y1" });

    Assertions.assertEquals(a, MetricKeyUtil.newMetricKey("test_foo"));
    Assertions.assertEquals(b, MetricKeyUtil.newMetricKey("test_bar"));
    Assertions.assertEquals(c, MetricKeyUtil.newMetricKey("test_foo", new String[] { "x", "y" }, new String[] { "X1", "Y1" }));
    Assertions.assertEquals(d, MetricKeyUtil.newMetricKey("test_bar", new String[] { "x", "y" }, new String[] { "X1", "Y1" }));
    Assertions.assertNotEquals(a, b);
    Assertions.assertNotEquals(a, c);
    Assertions.assertNotEquals(a, d);
  }

  @Test
  public void testKeyEqDefinition() {
    final MetricKey ak = MetricKeyUtil.newMetricKey("test_foo");
    final MetricKey bk = MetricKeyUtil.newMetricKey("test_bar");
    final MetricKey ck = MetricKeyUtil.newMetricKey("test_foo", new String[] { "x", "y" }, new String[] { "X1", "Y1" });
    final MetricKey dk = MetricKeyUtil.newMetricKey("test_bar", new String[] { "x", "y" }, new String[] { "X1", "Y1" });
    final MetricDefinition ad = MetricDefinitionUtil.of("test_foo", DatumUnit.COUNT, "test foo", null);
    final MetricDefinition bd = MetricDefinitionUtil.of("test_bar", DatumUnit.SECONDS, "test bar", null);
    final MetricDefinition cd = MetricDefinitionUtil.of("test_foo", new String[] { "x", "y" }, new String[] { "X1", "Y1" }, DatumUnit.SECONDS, "test foo", null);
    final MetricDefinition dd = MetricDefinitionUtil.of("test_bar", new String[] { "x", "y" }, new String[] { "X1", "Y1" }, DatumUnit.SECONDS, "test bar", null);

    Assertions.assertEquals(ak, ad);
    Assertions.assertEquals(bk, bd);
    Assertions.assertEquals(ck, cd);
    Assertions.assertEquals(dk, dd);
  }
}
