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

import io.github.matteobertozzi.easerinsights.metrics.collectors.TopK.TopEntrySnapshot;
import io.github.matteobertozzi.rednaco.time.TimeUtil;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ClockProvider;
import io.github.matteobertozzi.rednaco.time.TimeUtil.ManualClockProvider;

@Execution(ExecutionMode.SAME_THREAD)
public class TestTopK {
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
  public void testSingleSlot() {
    manualClock.setTime(1, TimeUnit.MILLISECONDS);

    TopEntrySnapshot[] snapshot;
    final TopK topK = TopK.newSingleThreaded(4, 10, 1, TimeUnit.MINUTES);
    Assertions.assertEquals(TopK.TopKSnapshot.EMPTY_SNAPSHOT, topK.dataSnapshot());

    Assertions.assertEquals(1, manualClock.epochMillis());
    topK.sample("k10", 1, 10);
    topK.sample("k5", 1, 5);
    snapshot = topK.dataSnapshot().entries();
    Assertions.assertEquals(2, snapshot.length);
    assertEntry(snapshot[0], "k10", 1, 10, 10, 10, 100);
    assertEntry(snapshot[1], "k5", 1, 5, 5, 5, 25);

    manualClock.incTime(1, TimeUnit.MILLISECONDS);
    Assertions.assertEquals(2, manualClock.epochMillis());
    topK.sample("k3", 2, 3);
    topK.sample("k7", 2, 7);
    snapshot = topK.dataSnapshot().entries();
    Assertions.assertEquals(4, snapshot.length);
    assertEntry(snapshot[0], "k10", 1, 10, 10, 10, 100);
    assertEntry(snapshot[1], "k7", 2, 7, 7, 7, 49);
    assertEntry(snapshot[2], "k5", 1, 5, 5, 5, 25);
    assertEntry(snapshot[3], "k3", 2, 3, 3, 3, 9);

    manualClock.incTime(1, TimeUnit.MILLISECONDS);
    Assertions.assertEquals(3, manualClock.epochMillis());
    topK.sample("k3", 3, 33);
    snapshot = topK.dataSnapshot().entries();
    Assertions.assertEquals(4, snapshot.length);
    assertEntry(snapshot[0], "k3", 3, 33, 3, 36, 1098);
    assertEntry(snapshot[1], "k10", 1, 10, 10, 10, 100);
    assertEntry(snapshot[2], "k7", 2, 7, 7, 7, 49);
    assertEntry(snapshot[3], "k5", 1, 5, 5, 5, 25);

    manualClock.incTime(1, TimeUnit.MILLISECONDS);
    Assertions.assertEquals(4, manualClock.epochMillis());
    topK.sample("k8", 4, 8);
    snapshot = topK.dataSnapshot().entries();
    Assertions.assertEquals(4, snapshot.length);
    assertEntry(snapshot[0], "k3", 3, 33, 3, 36, 1098);
    assertEntry(snapshot[1], "k10", 1, 10, 10, 10, 100);
    assertEntry(snapshot[2], "k8", 4, 8, 8, 8, 64);
    assertEntry(snapshot[3], "k7", 2, 7, 7, 7, 49);
  }

  @Test
  public void testMultiSlot() {
    manualClock.setTime(1, TimeUnit.MILLISECONDS);

    TopEntrySnapshot[] snapshot;
    final TopK topK = TopK.newSingleThreaded(4, 10, 1, TimeUnit.MINUTES);
    Assertions.assertEquals(TopK.TopKSnapshot.EMPTY_SNAPSHOT, topK.dataSnapshot());

    Assertions.assertEquals(1, manualClock.epochMillis());
    topK.sample("k10", 1, 10);
    topK.sample("k20", 1, 20);
    topK.sample("k30", 1, 30);
    topK.sample("k40", 1, 40);
    snapshot = topK.dataSnapshot().entries();
    Assertions.assertEquals(4, snapshot.length);
    assertEntry(snapshot[0], "k40", 1, 40, 40, 40, 1600);
    assertEntry(snapshot[1], "k30", 1, 30, 30, 30, 900);
    assertEntry(snapshot[2], "k20", 1, 20, 20, 20, 400);
    assertEntry(snapshot[3], "k10", 1, 10, 10, 10, 100);

    System.out.println("==========================================");
    manualClock.incTime(1, TimeUnit.MINUTES);
    Assertions.assertEquals(60001, manualClock.epochMillis());
    topK.sample("k15", 60001, 15);
    topK.sample("k20", 60001, 19);
    topK.sample("k35", 60001, 35);
    topK.sample("k40", 60001, 41);
    System.out.println("========================================== SNAPSHOT");
    snapshot = topK.dataSnapshot().entries();
    Assertions.assertEquals(4, snapshot.length);
    assertEntry(snapshot[0], "k40", 60001, 41, 40, 81, 3281);
    assertEntry(snapshot[1], "k35", 60001, 35, 35, 35, 1225);
    assertEntry(snapshot[2], "k30", 1, 30, 30, 30, 900);
    assertEntry(snapshot[3], "k20", 1, 20, 19, 39, 761);

  }

  private static void assertEntry(final TopEntrySnapshot entry, final String key, final long timestamp, final long maxValue, final long minValue, final long sum, final long sumSquare) {
    Assertions.assertEquals(maxValue, entry.maxValue());
    Assertions.assertEquals(minValue, entry.minValue());
    Assertions.assertEquals(timestamp, entry.maxTimestamp());
    Assertions.assertEquals(sum, entry.sum());
    Assertions.assertEquals(sumSquare, entry.sumSquares());
  }
}
