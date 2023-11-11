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

import io.github.matteobertozzi.rednaco.strings.HumansUtil;

public enum DatumUnit {
  BITS,
  BYTES,

  COUNT,
  PERCENT,

  // Time
  NANOSECONDS,
  MICROSECONDS,
  MILLISECONDS,
  SECONDS,
  ;

  public String asHumanString(final long value) {
    return humanConverter(this).asHumanString(value);
  }

  public DatumUnitConverter humanConverter() {
    return humanConverter(this);
  }

  public static DatumUnitConverter humanConverter(final DatumUnit unit) {
    return switch (unit) {
      case BITS -> HumansUtil::humanBits;
      case BYTES -> HumansUtil::humanBytes;
      case COUNT -> HumansUtil::humanCount;
      case PERCENT -> HumansUtil::humanPercent;
      case MICROSECONDS -> HumansUtil::humanTimeMicros;
      case MILLISECONDS -> HumansUtil::humanTimeMillis;
      case NANOSECONDS -> HumansUtil::humanTimeNanos;
      case SECONDS -> HumansUtil::humanTimeSeconds;
    };
  }

  @FunctionalInterface
  public interface DatumUnitConverter {
    String asHumanString(long value);
  }
}
