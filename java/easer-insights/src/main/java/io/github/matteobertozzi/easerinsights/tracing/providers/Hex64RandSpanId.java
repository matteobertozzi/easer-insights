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

import io.github.matteobertozzi.easerinsights.tracing.SpanId;
import io.github.matteobertozzi.easerinsights.tracing.SpanIdProvider;
import io.github.matteobertozzi.rednaco.bytes.encoding.IntDecoder;
import io.github.matteobertozzi.rednaco.strings.Base16;
import io.github.matteobertozzi.rednaco.strings.StringUtil;
import io.github.matteobertozzi.rednaco.util.RandData;

public final class Hex64RandSpanId implements SpanId {
  public static final SpanIdProvider PROVIDER = new Hex64RandSpanIdProvider();

  private final long id;
  private String hex;

  public Hex64RandSpanId(final long id) {
    this.id = id;
  }

  public boolean isNull() {
    return id == 0;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  @Override
  public boolean equals(final Object o) {
    return (o instanceof final Hex64RandSpanId other) && id == other.id;
  }

  @Override
  public String toString() {
    if (hex != null) return hex;

    hex = buildString();
    return hex;
  }

  private String buildString() {
    final StringBuilder builder = new StringBuilder(16);
    Base16.base16().encodeInt64(builder, id);
    return builder.toString();
  }

  private static final class Hex64RandSpanIdProvider implements SpanIdProvider {
    private static final SpanId NULL_SPAN_ID = new Hex64RandSpanId(0);

    private Hex64RandSpanIdProvider() {
      // no-op
    }

    @Override
    public SpanId nullSpanId() {
      return NULL_SPAN_ID;
    }

    @Override
    public SpanId newSpanId() {
      final byte[] randomBytes = new byte[8];
      RandData.generateNonZeroBytes(randomBytes);
      return parseSpanId(randomBytes);
    }

    @Override
    public SpanId parseSpanId(final String spanId) {
      if (StringUtil.isEmpty(spanId)) return null;
      if (spanId.length() == 16) {
        return new Hex64RandSpanId(Base16.base16().decodeInt32(spanId));
      }
      throw new IllegalArgumentException("expected a 64bit SpanId.");
    }

    @Override
    public SpanId parseSpanId(final byte[] spanId) {
      if (spanId == null || spanId.length != 8) {
        throw new IllegalArgumentException("expected a 64bit SpanId");
      }

      final long v = IntDecoder.BIG_ENDIAN.readFixed64(spanId, 0);
      return new Hex64RandSpanId(v);
    }
  }
}
