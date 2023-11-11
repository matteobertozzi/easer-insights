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

package io.github.matteobertozzi.easerinsights.metrics.collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.metrics.collectors.Histogram.HistogramSnapshot;

public class TestHistogram {
  private static final long[] TEST_FIXED_VALUES_BOUNDS = new long[] { 10, 20, 30, 40 };

  @Test
  public  void testFixedValuesSingleThread() {
    testFixedValues(Histogram.newSingleThreaded(TEST_FIXED_VALUES_BOUNDS));
  }

  private void testFixedValues(final Histogram histo) {
    HistogramSnapshot snapshot;

    histo.sample(5);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(5, snapshot.minValue());
    Assertions.assertEquals(5, snapshot.maxValue());
    Assertions.assertEquals(5, snapshot.sum());
    Assertions.assertEquals(25, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 5 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 1 }, snapshot.events());

    histo.sample(15);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(5, snapshot.minValue());
    Assertions.assertEquals(15, snapshot.maxValue());
    Assertions.assertEquals(20, snapshot.sum());
    Assertions.assertEquals(250, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 15 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] {  1,  1 }, snapshot.events());

    histo.sample(35);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(5, snapshot.minValue());
    Assertions.assertEquals(35, snapshot.maxValue());
    Assertions.assertEquals(55, snapshot.sum());
    Assertions.assertEquals(1475, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 35 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] {  1,  1,  0,  1 }, snapshot.events());

    histo.sample(25);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(5, snapshot.minValue());
    Assertions.assertEquals(35, snapshot.maxValue());
    Assertions.assertEquals(80, snapshot.sum());
    Assertions.assertEquals(2100, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 35 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] {  1,  1,  1,  1 }, snapshot.events());

    histo.sample(123);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(5, snapshot.minValue());
    Assertions.assertEquals(123, snapshot.maxValue());
    Assertions.assertEquals(203, snapshot.sum());
    Assertions.assertEquals(17229, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 123 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] {  1,  1,  1,  1,   1 }, snapshot.events());

    histo.sample(3);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(3, snapshot.minValue());
    Assertions.assertEquals(123, snapshot.maxValue());
    Assertions.assertEquals(206, snapshot.sum());
    Assertions.assertEquals(17238, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 123 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] {  2,  1,  1,  1,   1 }, snapshot.events());

    histo.sample(34);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(3, snapshot.minValue());
    Assertions.assertEquals(123, snapshot.maxValue());
    Assertions.assertEquals(240, snapshot.sum());
    Assertions.assertEquals(18394, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 123 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] {  2,  1,  1,  2,   1 }, snapshot.events());

    histo.sample(360);
    snapshot = histo.dataSnapshot();
    Assertions.assertEquals(3, snapshot.minValue());
    Assertions.assertEquals(360, snapshot.maxValue());
    Assertions.assertEquals(600, snapshot.sum());
    Assertions.assertEquals(147994, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 360 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] {  2,  1,  1,  2,   2 }, snapshot.events());
  }
}
