/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.matteobertozzi.easerinsights.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class TimeUtil {
  private static ClockProvider CLOCK_INSTANCE = SystemClock.INSTANCE;

  private TimeUtil() {
    // no-op
  }

  public static long alignToWindow(final long timestamp, final int window) {
    return timestamp - (timestamp % window);
  }

  // =====================================================================================
  //  Clock Util
  // =====================================================================================
  public interface ClockProvider {
    long epochMillis();
    long epochNanos();
  }

  public static ClockProvider getClockProvider() {
    return TimeUtil.CLOCK_INSTANCE;
  }

  public static void setClockProvider(final ClockProvider provider) {
    TimeUtil.CLOCK_INSTANCE = provider;
  }

  public static void setSystemClockProvider() {
    setClockProvider(SystemClock.INSTANCE);
  }

  public static long currentEpochMillis() {
    return CLOCK_INSTANCE.epochMillis();
  }

  private static final class SystemClock implements ClockProvider {
    private static final SystemClock INSTANCE = new SystemClock();

    @Override
    public long epochMillis() {
      return System.currentTimeMillis();
    }

    @Override
    public long epochNanos() {
      final Instant now = Instant.now();
      final long seconds = now.getEpochSecond();
      final long nanosFromSecond = now.getNano();
      return (seconds * 1_000_000_000L) + nanosFromSecond;
    }
  }

  public static class ManualClockProvider implements ClockProvider {
    private long nanos;

    public ManualClockProvider(final long nanos) {
      this.nanos = nanos;
    }

    @Override
    public long epochMillis() {
      return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    @Override
    public long epochNanos() {
      return nanos;
    }

    public void reset() {
      this.nanos = 0;
    }

    public void setTime(final long value, final TimeUnit unit) {
      this.nanos = unit.toNanos(value);
    }

    public void incTime(final long value, final TimeUnit unit) {
      this.nanos += unit.toNanos(value);
    }

    public void incTime(final Duration duration) {
      this.nanos += duration.toNanos();
    }

    public void decTime(final long value, final TimeUnit unit) {
      this.nanos -= unit.toNanos(value);
    }

    public void decTime(final Duration duration) {
      this.nanos += duration.toNanos();
    }
  }
}
