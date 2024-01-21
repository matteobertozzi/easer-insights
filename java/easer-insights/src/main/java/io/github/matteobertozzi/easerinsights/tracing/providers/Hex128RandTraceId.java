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

import io.github.matteobertozzi.easerinsights.tracing.TraceId;
import io.github.matteobertozzi.easerinsights.tracing.TraceIdProvider;
import io.github.matteobertozzi.rednaco.bytes.encoding.IntDecoder;
import io.github.matteobertozzi.rednaco.strings.Base16;
import io.github.matteobertozzi.rednaco.util.RandData;

public final class Hex128RandTraceId implements TraceId {
  public static final TraceIdProvider PROVIDER = new Hex128RandTraceIdProvider();

  private final long hi;
  private final long lo;
  private String hex;

  public Hex128RandTraceId(final long hi, final long lo) {
    this.hi = hi;
    this.lo = lo;
  }

  public boolean isNull() {
    return hi == 0 && lo == 0;
  }

  @Override
  public int hashCode() {
    int hash = 1;
    hash = 31 * hash + Long.hashCode(hi);
    hash = 31 * hash + Long.hashCode(lo);
    return hash;
  }

  @Override
  public boolean equals(final Object o) {
    return (o instanceof final Hex128RandTraceId other)
      && hi == other.hi && lo == other.lo;
  }

  @Override
  public String toString() {
    if (hex != null) return hex;
    return hex = buildString();
  }

  private String buildString() {
    final StringBuilder builder = new StringBuilder(32);
    final Base16 b16 = Base16.base16();
    b16.encodeInt64(builder, hi);
    b16.encodeInt64(builder, lo);
    return builder.toString();
  }

  private static final class Hex128RandTraceIdProvider implements TraceIdProvider {
    private static final TraceId NULL_TRACE_ID = new Hex128RandTraceId(0, 0);

    private Hex128RandTraceIdProvider() {
      // no-op
    }

    @Override
    public TraceId nullTraceId() {
      return NULL_TRACE_ID;
    }

    @Override
    public TraceId newTraceId() {
      final byte[] randomBytes = new byte[16];
      RandData.generateNonZeroBytes(randomBytes);
      return parseTraceId(randomBytes);
    }

    @Override
    public TraceId parseTraceId(final String traceId) {
      if (traceId != null && traceId.length() == 32) {
        final long hi = Base16.base16().decodeInt64(traceId, 0, 16);
        final long lo = Base16.base16().decodeInt64(traceId, 16, 32);
        return new Hex128RandTraceId(hi, lo);
      }
      throw new IllegalArgumentException("expected a 64bit SpanId.");
    }

    @Override
    public TraceId parseTraceId(final byte[] traceId) {
      if (traceId == null || traceId.length != 16) {
        throw new IllegalArgumentException("expected a 128bit TraceId");
      }

      final long hi = IntDecoder.BIG_ENDIAN.readFixed64(traceId, 0);
      final long lo = IntDecoder.BIG_ENDIAN.readFixed64(traceId, 8);
      return new Hex128RandTraceId(hi, lo);
    }
  }
}
