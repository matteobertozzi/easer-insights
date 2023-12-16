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

import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.tracing.TraceId;
import io.github.matteobertozzi.easerinsights.tracing.TraceIdProvider;
import io.github.matteobertozzi.rednaco.strings.Base16;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

public final class SnowflakeTraceId implements TraceId {
  private final long id;
  private String hex;

  private SnowflakeTraceId(final long id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  @Override
  public boolean equals(final Object o) {
    return (o instanceof final SnowflakeTraceId other) && id == other.id;
  }

  @Override
  public String toString() {
    if (hex != null) return hex;
    return hex = Base16.base16().encodeInt64(id);
  }

  public static TraceIdProvider newProvider(final int shardBits, final int shard) {
    return new SnowflakeTraceIdProvider(shardBits, shard);
  }

  private static class SnowflakeTraceIdProvider implements TraceIdProvider {
    private static final SnowflakeTraceId NULL_TRACE_ID = new SnowflakeTraceId(0);

    private final SnowflakeId idGenerator;

    private SnowflakeTraceIdProvider(final int shardBits, final int shard) {
      this.idGenerator = new SnowflakeId(1702497600000L, shardBits, shard);
    }

    @Override
    public TraceId nullTraceId() {
      return NULL_TRACE_ID;
    }

    @Override
    public TraceId newTraceId() {
      return new SnowflakeTraceId(idGenerator.next());
    }

    @Override
    public TraceId parseTraceId(final String traceId) {
      final long id = Base16.base16().decodeInt64(traceId);
      return new SnowflakeTraceId(id);
    }

    @Override
    public TraceId parseTraceId(final byte[] traceId) {
      final long id = Base16.base16().decodeInt64(traceId);
      return new SnowflakeTraceId(id);
    }
  }

  private static final class SnowflakeId {
    private final ReentrantLock lock = new ReentrantLock();
    private final long refTime;
    private final int seqMax;
    private final int seqBits;
    private final int shard;

    private long lastTs;
    private long seq;

    public SnowflakeId(final long refTimeMillis, final int shardBits, final int shard) {
      if (shard < 0 || shard >= (1 << shardBits)) {
        throw new IllegalArgumentException("invalid shard: " + shard);
      }

      this.shard = shard;
      this.seqBits = 64 - (41 + shardBits);
      this.seqMax = (1 << seqBits) - 1;
      this.refTime = refTimeMillis;
      this.lastTs = 0;
      this.seq = 0;
    }

    public long next() {
      lock.lock();
      try {
        while (true) {
          final long timestamp = TimeUtil.currentEpochMillis() - refTime;
          if (timestamp < lastTs) {
            Thread.onSpinWait();
            continue;
          }

          if (timestamp != lastTs) {
            lastTs = timestamp;
            seq = 0;
          } else if (seq++ >= seqMax) {
            Thread.onSpinWait();
            continue;
          }

          return (timestamp << 23) | ((long) shard << seqBits) | seq;
        }
      } finally {
        lock.unlock();
      }
    }
  }
}
