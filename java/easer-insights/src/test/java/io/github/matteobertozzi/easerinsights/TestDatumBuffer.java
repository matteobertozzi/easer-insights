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

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferWriter;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

public class TestDatumBuffer {
  @Test
  public void randData() {
    final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    final int ALIGN_MS = 10;
    final int TIME_DELTA = 5 * 60_000;
    final int NITEMS = 1_000_000;
    final int PAGE_SIZE = 512;

    final Random random = new Random();
    final long seed = System.nanoTime();
    System.out.println("TestDatumBuffer.randData() seed: " + seed);

    // Generate data
    random.setSeed(seed);
    final long now = System.currentTimeMillis();
    final DatumBufferWriter writer = DatumBuffer.newWriter(ALIGN_MS, PAGE_SIZE, queue::add);
    for (int i = 0; i < NITEMS; ++i) {
      writer.add(i & 0xffffff, now + random.nextInt(TIME_DELTA), random.nextLong());
    }
    writer.flush();
    Assertions.assertTrue(queue.size() > 1);

    // Read & Verify data
    int index = 0;
    random.setSeed(seed);
    final DatumBufferReader reader = DatumBuffer.newReader();
    while (!queue.isEmpty()) {
      reader.resetPage(queue.poll());
      while (reader.hasNext()) {
        final DatumBufferEntry entry = reader.next();
        Assertions.assertEquals(index & 0xffffff, entry.metricId(), "unexpected metricId");
        Assertions.assertEquals(TimeUtil.alignToWindow(now + random.nextInt(TIME_DELTA), ALIGN_MS), entry.timestamp(), "unexpected timestamp");
        Assertions.assertEquals(random.nextLong(), entry.value(), "unexpected value");
        index++;
      }
    }
    Assertions.assertEquals(NITEMS, index);
  }
}
