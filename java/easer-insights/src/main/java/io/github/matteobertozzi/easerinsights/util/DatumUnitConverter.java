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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import io.github.matteobertozzi.easerinsights.DatumUnit;

@FunctionalInterface
public interface DatumUnitConverter {
  String asHumanString(long value);

  static String asHumanString(final DatumUnit unit, final long value) {
    return humanConverter(unit).asHumanString(value);
  }

  static DatumUnitConverter humanConverter(final DatumUnit unit) {
    return switch (unit) {
      case BITS -> DatumUnitConverter::humanBits;
      case BYTES -> DatumUnitConverter::humanSize;
      case COUNT -> DatumUnitConverter::humanCount;
      case PERCENT -> DatumUnitConverter::humanPercent;
      case MICROSECONDS -> DatumUnitConverter::humanTimeMicros;
      case MILLISECONDS -> DatumUnitConverter::humanTimeMillis;
      case NANOSECONDS -> DatumUnitConverter::humanTimeNanos;
      case SECONDS -> DatumUnitConverter::humanTimeSeconds;
    };
  }

  static String humanPercent(final long value) {
    return value + "%";
  }

  // ================================================================================
  //  Size related
  // ================================================================================
  static String humanBits(final long size) {
    if (size >= 1000_000_000_000_000_000L) return String.format("%.2fEbit", (float) size / 1000_000_000_000_000_000L);
    if (size >= 1000_000_000_000_000L) return String.format("%.2fPbit", (float) size / 1000_000_000_000_000L);
    if (size >= 1000_000_000_000L) return String.format("%.2fTbit", (float) size / 1000_000_000_000L);
    if (size >= 1000_000_000L) return String.format("%.2fGbit", (float) size / 1000_000_000L);
    if (size >= 1000_000L) return String.format("%.2fMbit", (float) size / 1000_000L);
    if (size >= 1000L) return String.format("%.2fKbit", (float) size / 1000L);
    return size > 0 ? size + "bit" : "0";
  }

  static String humanSize(final long size) {
    if (size >= (1L << 60)) return String.format("%.2fEiB", (float) size / (1L << 60));
    if (size >= (1L << 50)) return String.format("%.2fPiB", (float) size / (1L << 50));
    if (size >= (1L << 40)) return String.format("%.2fTiB", (float) size / (1L << 40));
    if (size >= (1L << 30)) return String.format("%.2fGiB", (float) size / (1L << 30));
    if (size >= (1L << 20)) return String.format("%.2fMiB", (float) size / (1L << 20));
    if (size >= (1L << 10)) return String.format("%.2fKiB", (float) size / (1L << 10));
    return size > 0 ? size + "bytes" : "0";
  }

  static String humanCount(final long size) {
    if (size >= 1000000) return String.format("%.2fM", (float) size / 1000000);
    if (size >= 1000) return String.format("%.2fK", (float) size / 1000);
    return Long.toString(size);
  }

  // ================================================================================
  //  Time related
  // ================================================================================
  static String humanTimeSince(final long timeNano) {
    return humanTime(System.nanoTime() - timeNano, TimeUnit.NANOSECONDS);
  }

  static String humanTimeNanos(final long timeNs) {
    if (timeNs < 1000) return (timeNs < 0) ? "unkown" : timeNs + "ns";
    return humanTimeMicros(timeNs / 1000);
  }

  static String humanTimeMicros(final long timeUs) {
    if (timeUs < 1000) return (timeUs < 0) ? "unkown" : timeUs + "us";
    return humanTimeMillis(timeUs / 1000);
  }

  static String humanTimeMillis(final long timeDiff) {
    return humanTime(timeDiff, TimeUnit.MILLISECONDS);
  }

  static String humanTimeSeconds(final long timeDiff) {
    return humanTime(timeDiff, TimeUnit.SECONDS);
  }

  static String humanTime(final long timeDiff, final TimeUnit unit) {
    final long msec = unit.toMillis(timeDiff);
    if (msec == 0) {
      final long micros = unit.toMicros(timeDiff);
      if (micros > 0) return micros + "us";
      return unit.toNanos(timeDiff) + "ns";
    }

    if (msec < 1000) {
      return msec + "ms";
    }

    final long hours = msec / (60 * 60 * 1000);
    long rem = (msec % (60 * 60 * 1000));
    final long minutes = rem / (60 * 1000);
    rem = rem % (60 * 1000);
    final float seconds = rem / 1000.0f;

    if ((hours > 0) || (minutes > 0)) {
      final StringBuilder buf = new StringBuilder(32);
      if (hours > 0) {
        buf.append(hours);
        buf.append("hrs, ");
      }
      if (minutes > 0) {
        buf.append(minutes);
        buf.append("min, ");
      }

      final String humanTime;
      if (seconds > 0) {
        buf.append(String.format("%.2fsec", seconds));
        humanTime = buf.toString();
      } else {
        humanTime = buf.substring(0, buf.length() - 2);
      }

      if (hours > 24) {
        return String.format("%s (%.1f days)", humanTime, (hours / 24.0));
      }
      return humanTime;
    }

    return String.format((seconds % 1) != 0 ? "%.4fsec" : "%.0fsec", seconds);
  }

  static String humanDateFromEpochMillis(final long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toString();
  }
}
