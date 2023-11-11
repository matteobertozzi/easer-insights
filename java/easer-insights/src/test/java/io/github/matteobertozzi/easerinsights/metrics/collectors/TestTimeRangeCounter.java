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

import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter.TimeRangeCounterSnapshot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ClockProvider;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ManualClockProvider;

@Execution(ExecutionMode.SAME_THREAD)
public class TestTimeRangeCounter {
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
    final TimeRangeCounter trc = TimeRangeCounter.newSingleThreaded(3, 1, TimeUnit.MINUTES);
    TimeRangeCounterSnapshot snapshot;

    // inc to slot 0
    manualClock.reset();
    trc.inc();
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(0, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1 }, snapshot.counters());

    // add 3 to slot 0, we are in the same minute
    manualClock.incTime(5, TimeUnit.SECONDS);
    trc.add(3);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(0, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 4 }, snapshot.counters());

    // add a new slot (slot 1) and inc
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.inc();
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(60_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 4, 1 }, snapshot.counters());

    // add a new slot (slot 3) and add 2
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.add(2);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(120_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 4, 1, 2 }, snapshot.counters());

    // update slot in the past
    trc.inc(0);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(120_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 5, 1, 2 }, snapshot.counters());

    // add a new slot (slot 3), slot 0 will be removed
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.add(3);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(180_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1, 2, 3 }, snapshot.counters());

    // jump in time and add slot 5
    manualClock.incTime(2, TimeUnit.MINUTES);
    trc.add(4);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(30_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 3, 0, 4 }, snapshot.counters());

    // jump in time and add slot 8
    manualClock.incTime(2, TimeUnit.MINUTES);
    trc.add(5);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(42_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 4, 0, 5 }, snapshot.counters());

    // jump far far away in time
    manualClock.incTime(20, TimeUnit.MINUTES);
    trc.add(6);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(162_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 0, 6 }, snapshot.counters());

    // add a new slot
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.add(7);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 6, 7 }, snapshot.counters());

    // update with same timestamp
    trc.add(manualClock.epochMillis(), 2);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 6, 9 }, snapshot.counters());

    // update previous slot
    trc.dec(162_5000);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 5, 9 }, snapshot.counters());

    // update too far in the past
    trc.add(42_0000, 1000);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 5, 9 }, snapshot.counters());

    // update too far in the past
    trc.add(150_0000, 1000);
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(168_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 0, 5, 9 }, snapshot.counters());

    // add a new slot
    manualClock.incTime(1, TimeUnit.MINUTES);
    trc.inc();
    snapshot = trc.dataSnapshot();
    Assertions.assertEquals(174_0000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 5, 9, 1 }, snapshot.counters());
  }
}
