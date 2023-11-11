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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import io.github.matteobertozzi.easerinsights.metrics.collectors.Heatmap.HeatmapSnapshot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ClockProvider;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ManualClockProvider;

@Execution(ExecutionMode.SAME_THREAD)
public class TestHeatmap {
  private static final ManualClockProvider manualClock = new ManualClockProvider(0);
  private static ClockProvider oldClockProvider;

  @BeforeAll
  public static void beforeAll() {
    oldClockProvider = TimeUtil.getClockProvider();
    TimeUtil.setClockProvider(manualClock);
  }

  @AfterAll
  public static void afterAll() {
    TimeUtil.setClockProvider(oldClockProvider);
  }

  @Test
  public void testFixedValues() {
    final Heatmap heatmap = Heatmap.newSingleThreaded(3, 1, TimeUnit.MINUTES, new long[] { 10, 20, 30, 40, 50 });
    HeatmapSnapshot snapshot;

    manualClock.reset();
    heatmap.sample(5);
    heatmap.sample(15);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(0, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {   5 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] {  15 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] {  20 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 250 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 15 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 1, 1 }, snapshot.events());

    // add a new slot
    manualClock.incTime(1, TimeUnit.MINUTES);
    heatmap.sample(24);
    heatmap.sample(25);
    heatmap.sample(35);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(60_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {   5,   24 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] {  15,   35 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] {  20,   84 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 250, 2426 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 35 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 1, 1, 0, 0, 0, 0, 2, 1 }, snapshot.events());

    // add a new slot
    manualClock.incTime(1, TimeUnit.MINUTES);
    heatmap.sample(2);
    heatmap.sample(37);
    heatmap.sample(36);
    heatmap.sample(3);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(120000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {   5,   24,    2 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] {  15,   35,   37 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] {  20,   84,   78 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 250, 2426, 2678 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 37 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 1, 1, 0, 0,  0, 0, 2, 1,  2, 0, 0, 2 }, snapshot.events());

    // add a new slot (expire the first)
    manualClock.incTime(1, TimeUnit.MINUTES);
    heatmap.sample(45);
    heatmap.sample(55);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(180000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {   24,    2,   45 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] {   35,   37,   55 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] {   84,   78,  100 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 2426, 2678, 5050 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 50, 55 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 0, 0, 2, 1, 0, 0,  2, 0, 0, 2, 0, 0,  0, 0, 0, 0, 1, 1 }, snapshot.events());

    // jump in time and add a new slot
    manualClock.incTime(2, TimeUnit.MINUTES);
    heatmap.sample(5);
    heatmap.sample(7);
    heatmap.sample(15);
    heatmap.sample(25);
    heatmap.sample(35);
    heatmap.sample(45);
    heatmap.sample(145);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(300000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {   45, 0,     5 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] {   55, 0,   145 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] {  100, 0,   277 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 5050, 0, 25199 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 50, 145 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 0, 0, 0, 0, 1, 1,  0, 0, 0, 0, 0, 0,  2, 1, 1, 1, 1, 1 }, snapshot.events());

    // update a slot in the past
    heatmap.sample(180000, 1);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(300000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {    1, 0,     5 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] {   55, 0,   145 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] {  101, 0,   277 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 5051, 0, 25199 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 50, 145 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 1, 0, 0, 0, 1, 1,  0, 0, 0, 0, 0, 0,  2, 1, 1, 1, 1, 1 }, snapshot.events());

    // update a slot too far in the past
    heatmap.sample(50, 1234);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(300000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {    1, 0,     5 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] {   55, 0,   145 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] {  101, 0,   277 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 5051, 0, 25199 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 30, 40, 50, 145 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 1, 0, 0, 0, 1, 1,  0, 0, 0, 0, 0, 0,  2, 1, 1, 1, 1, 1 }, snapshot.events());

    // jump into a distante future
    manualClock.incTime(20, TimeUnit.MINUTES);
    heatmap.sample(23);
    snapshot = heatmap.dataSnapshot();
    Assertions.assertEquals(1500000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 0,  23 }, snapshot.minValue());
    Assertions.assertArrayEquals(new long[] { 0, 0,  23 }, snapshot.maxValue());
    Assertions.assertArrayEquals(new long[] { 0, 0,  23 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 0, 0, 529 }, snapshot.sumSquares());
    Assertions.assertArrayEquals(new long[] { 10, 20, 23 }, snapshot.bounds());
    Assertions.assertArrayEquals(new long[] { 0, 0, 0,  0, 0, 0,  0, 0, 1 }, snapshot.events());
  }
}
