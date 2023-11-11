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
public class TestTimeRangeDrag {
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
    final TimeRangeDrag trq = TimeRangeDrag.newSingleThreaded(3, 1, TimeUnit.MINUTES);
    TimeRangeCounterSnapshot snapshot;

    // inc to slot 0
    manualClock.reset();
    trq.inc();
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(0, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1 }, snapshot.counters());

    // add a new slot (slot 1) and inc
    manualClock.incTime(1, TimeUnit.MINUTES);
    trq.inc();
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(60_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1, 2 }, snapshot.counters());

    // add a new slot (slot 2) and dec
    manualClock.incTime(1, TimeUnit.MINUTES);
    trq.dec();
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(120_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1, 2, 1 }, snapshot.counters());

    // add a new slot (slot 3) and inc
    manualClock.incTime(1, TimeUnit.MINUTES);
    trq.add(2);
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(180_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 2, 1, 3 }, snapshot.counters());

    // move too far in the future
    manualClock.incTime(13, TimeUnit.MINUTES);
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(960_000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 3, 3, 3 }, snapshot.counters());

    // add a new slot and dec
    manualClock.incTime(1, TimeUnit.MINUTES);
    trq.add(-2);
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(1020000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 3, 3, 1 }, snapshot.counters());

    // update a slot in the past
    trq.dec(900000);
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(1020000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 2, 2, 0 }, snapshot.counters());

    // update a slot too far in the past
    trq.dec(100);
    snapshot = trq.dataSnapshot();
    Assertions.assertEquals(1020000, snapshot.lastInterval());
    Assertions.assertEquals(60_000, snapshot.window());
    Assertions.assertArrayEquals(new long[] { 1, 1, -1 }, snapshot.counters());
  }
}
