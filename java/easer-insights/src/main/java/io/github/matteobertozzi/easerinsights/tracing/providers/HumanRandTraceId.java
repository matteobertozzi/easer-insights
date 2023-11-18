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

package io.github.matteobertozzi.easerinsights.tracing.providers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

import io.github.matteobertozzi.easerinsights.tracing.TraceId;
import io.github.matteobertozzi.easerinsights.tracing.TraceIdProvider;
import io.github.matteobertozzi.rednaco.bytes.encoding.IntDecoder;
import io.github.matteobertozzi.rednaco.strings.Base32;
import io.github.matteobertozzi.rednaco.util.RandData;

public class HumanRandTraceId implements TraceId {
  public static final TraceIdProvider PROVIDER = new HumanRandTraceIdProvider(9);

  private final int date;
  private final int time;
  private final byte[] rand;
  private String str;

  public HumanRandTraceId(final int date, final int time, final byte[] rand) {
    this.date = date;
    this.time = time;
    this.rand = rand;
  }

  public boolean isNull() {
    return date == 0 && time == 0;
  }

  @Override
  public int hashCode() {
    int hash = 1;
    hash = 31 * hash + Long.hashCode(date);
    hash = 31 * hash + Long.hashCode(time);
    hash = 31 * hash + Arrays.hashCode(rand);
    return hash;
  }

  @Override
  public boolean equals(final Object o) {
    return (o instanceof final HumanRandTraceId other)
      && date == other.date && time == other.time
      && Arrays.equals(rand, other.rand);
  }

  @Override
  public String toString() {
    if (str != null) return str;

    str = buildString();
    return str;
  }

  private String buildString() {
    final StringBuilder builder = new StringBuilder(32);
    builder.append(date);
    builder.append('-');
    builder.append(time);
    builder.append('-');
    Base32.crockfordBase32().encode(builder, rand, 0, rand.length);
    return builder.toString();
  }

  public static final class HumanRandTraceIdProvider implements TraceIdProvider {
    private static final TraceId NULL_TRACE_ID = new HumanRandTraceId(0, 0, new byte[] { 0, 0 });

    private final int randLength;

    public HumanRandTraceIdProvider(final int randLength) {
      this.randLength = randLength;
    }

    @Override
    public TraceId nullTraceId() {
      return NULL_TRACE_ID;
    }

    @Override
    public TraceId newTraceId() {
      final byte[] randomBytes = new byte[randLength];
      RandData.generateNonZeroBytes(randomBytes);
      final ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
      final int date = (zdt.getYear() % 100) * 10000
                     + zdt.getMonthValue() * 100
                     + zdt.getDayOfMonth();
      final int time = zdt.getHour() * 10000
                     + zdt.getMinute() * 100
                     + zdt.getSecond();
      return new HumanRandTraceId(date, time, randomBytes);
    }

    @Override
    public TraceId parseTraceId(final String traceId) {
      if (traceId != null && traceId.length() == 32) {
        final int date = Integer.parseInt(traceId, 0, 6, 10);
        final int time = Integer.parseInt(traceId, 7, 13, 10);
        final byte[] rand = Base32.crockfordBase32().decode(traceId, 14, traceId.length() - 14);
        return new HumanRandTraceId(date, time, rand);
      }
      throw new IllegalArgumentException("expected a 64bit SpanId.");
    }

    @Override
    public TraceId parseTraceId(final byte[] traceId) {
      if (traceId == null || traceId.length != 16) {
        throw new IllegalArgumentException("expected a 128bit TraceId");
      }

      final int date = IntDecoder.BIG_ENDIAN.readFixed32(traceId, 0);
      final int time = IntDecoder.BIG_ENDIAN.readFixed32(traceId, 4);
      final byte[] rand = Arrays.copyOfRange(traceId, 8, traceId.length);
      return new HumanRandTraceId(date, time, rand);
    }
  }
}
