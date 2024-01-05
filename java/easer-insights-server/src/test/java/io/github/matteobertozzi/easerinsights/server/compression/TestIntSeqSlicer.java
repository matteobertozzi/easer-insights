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

package io.github.matteobertozzi.easerinsights.server.compression;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.matteobertozzi.easerinsights.server.compression.IntSeqCoding.IntSeqEncoder;
import io.github.matteobertozzi.rednaco.bytes.PagedByteArray;
import io.github.matteobertozzi.rednaco.collections.IntegerValue;

public class TestIntSeqSlicer {
  @Test
  public void testRleEncodeDecode() {
    testRandEncodeDecode(() -> {
      final Random rand = ThreadLocalRandom.current();
      final long[] serie = new long[rand.nextInt(1, 2 << 20)];
      Arrays.fill(serie, rand.nextInt(0, Integer.MAX_VALUE));
      return serie;
    });
  }

  @Test
  public void testLinEncodeDecode() {
    testRandEncodeDecode(() -> {
      final Random rand = ThreadLocalRandom.current();
      final long[] serie = new long[rand.nextInt(1, 2 << 20)];
      final int initValue = rand.nextInt(0, Short.MAX_VALUE);
      final int step = rand.nextInt(1, Short.MAX_VALUE);
      final int groupLen = rand.nextInt(2, Short.MAX_VALUE);
      for (int i = 0; i < serie.length;) {
        for (int g = 0; g < groupLen && i < serie.length; ++g) {
          serie[i++] = initValue + (g * step);
        }
      }
      return serie;
    });
  }

  @Test
  public void testShortRandEncodeDecode() {
    testRandEncodeDecode(() -> generateSerie(Short.MAX_VALUE));
  }

  @Test
  public void testIntRandEncodeDecode() {
    testRandEncodeDecode(() -> generateSerie(Integer.MAX_VALUE));
  }

  @Test
  public void testLongRandEncodeDecode() {
    testRandEncodeDecode(() -> generateSerie(Long.MAX_VALUE));
  }

  private void testRandEncodeDecode(final SerieGenerator generator) {
    final PagedByteArray buf = new PagedByteArray(8192);
    for (int i = 0; i < 10; ++i) {
      final long[] serie = generator.generate();
      buf.clear();

      final IntSeqEncoder encoder = IntSeqCoding.newByteArrayEncoder(buf);
      IntSeqSlicer.slice(encoder, serie);
      encoder.flush();

      final IntegerValue index = new IntegerValue(0);
      IntSeqCoding.decode(buf.toByteArray(), 0, value -> {
        final int serieIdx = index.getAndIncrement();
        Assertions.assertEquals(serie[serieIdx], value);
      });
      Assertions.assertEquals(serie.length, index.get());
    }
  }

  private static long[] generateSerie(final long maxValue) {
    final ThreadLocalRandom rand = ThreadLocalRandom.current();
    final long[] serie = new long[rand.nextInt(1, 2 << 20)];
    for (int i = 0; i < serie.length; ++i) {
      serie[i] = rand.nextLong(0, maxValue);
    }
    return serie;
  }

  interface SerieGenerator {
    long[] generate();
  }
}
