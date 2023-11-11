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

import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge.MaxAvgTimeRangeGaugeSnapshot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ClockProvider;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ManualClockProvider;

@Execution(ExecutionMode.SAME_THREAD)
public class TestMaxAvgTimeRangeGauge {
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
    final MaxAvgTimeRangeGauge trc = MaxAvgTimeRangeGauge.newSingleThreaded(3, 1, TimeUnit.MINUTES);
    MaxAvgTimeRangeGaugeSnapshot snapshot;

    // inc to slot 0
    manualClock.reset();
    trc.sample(10);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(0, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {  1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 10 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 10 }, snapshot.max());

    // add 3 to slot 0, we are in the same minute
    manualClock.incTime(5, TimeUnit.SECONDS);
    trc.sample(3);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(0, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {  2 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 13 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 10 }, snapshot.max());

    // add a new slot (slot 1) and inc
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.sample(5);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(60_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {  2, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 13, 5 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 10, 5 }, snapshot.max());

    // add a new slot (slot 3) and add 2
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.sample(2);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(120_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {  2, 1, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 13, 5, 2 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 10, 5, 2 }, snapshot.max());

    // update slot in the past
    trc.sample(500, 7);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(120_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] {  3, 1, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 20, 5, 2 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 10, 5, 2 }, snapshot.max());

    // add a new slot (slot 3), slot 0 will be removed
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.sample(3);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(180_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1, 1, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 5, 2, 3 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 5, 2, 3 }, snapshot.max());

    // jump in time and add slot 5
    manualClock.incTime(2, TimeUnit.MINUTES);
    trc.sample(4);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(30_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1, 0, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 3, 0, 4 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 3, 0, 4 }, snapshot.max());

    // jump in time and add slot 8
    manualClock.incTime(2, TimeUnit.MINUTES);
    trc.sample(5);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(42_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1, 0, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 4, 0, 5 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 4, 0, 5 }, snapshot.max());

    // jump far far away in time
    manualClock.incTime(20, TimeUnit.MINUTES);
    trc.sample(6);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(162_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 0, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 0, 0, 6 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 0, 0, 6 }, snapshot.max());

    // add a new slot
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.sample(7);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 1, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 0, 6, 7 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 0, 6, 7 }, snapshot.max());

    // update with same timestamp
    trc.sample(manualClock.epochMillis(), 2);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 1, 2 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 0, 6, 9 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 0, 6, 7 }, snapshot.max());

    // update previous slot
    trc.sample(162_5000, 1);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 2, 2 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 0, 7, 9 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 0, 6, 7 }, snapshot.max());

    // update too far in the past
    trc.sample(42_0000, 1000);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 2, 2 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 0, 7, 9 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 0, 6, 7 }, snapshot.max());

    // update too far in the past
    trc.sample(150_0000, 1000);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 2, 2 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 0, 7, 9 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 0, 6, 7 }, snapshot.max());

    // add a new slot
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.sample(1);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(174_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 2, 2, 1 }, snapshot.count());
    Assertions.assertArrayEquals(new long[] { 7, 9, 1 }, snapshot.sum());
    Assertions.assertArrayEquals(new long[] { 6, 7, 1 }, snapshot.max());
  }
}
