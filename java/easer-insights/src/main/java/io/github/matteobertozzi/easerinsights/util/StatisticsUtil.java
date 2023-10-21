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

package io.github.matteobertozzi.easerinsights.util;

public final class StatisticsUtil {
  private StatisticsUtil() {
    // no-op
  }

  public static long sum(final long[] events, final int offset, final int length) {
    long total = 0;
    for (int i = offset, endOffset = offset + length; i < endOffset; ++i) {
      total += events[i];
    }
    return total;
  }

  public static double average(final long numEvents, final double sum) {
    return numEvents != 0 ? sum / numEvents : 0;
  }

  public static double variance(final long numEvents, final double sum, final double sumSquares) {
    if (numEvents == 0) return 0;
    return (sumSquares * numEvents - sum * sum) / (numEvents * numEvents);
  }

  public static double standardDeviation(final long numEvents, final double sum, final double sumSquares) {
    if (numEvents == 0) return 0;
    return Math.sqrt(Math.max(variance(numEvents, sum, sumSquares), 0.0));
  }

  public static double percentile(final double p, final long[] bounds, final long minValue, final long maxValue, final long numEvents, final long[] events, final int eventOff) {
    if (numEvents == 0) return 0;

    final double threshold = numEvents * (p / 100.0);
    long cumulativeSum = 0;
    for (int b = 0; b < bounds.length; b++) {
      final long bucketValue = events[eventOff + b];
      cumulativeSum += bucketValue;
      if (cumulativeSum >= threshold) {
        // Scale linearly within this bucket
        final long leftPoint = (b == 0) ? minValue : bounds[b - 1];
        final long rightPoint = bounds[b];
        final long leftSum = cumulativeSum - bucketValue;
        double pos = 0;
        final long rightLeftDiff = cumulativeSum - leftSum;
        if (rightLeftDiff != 0) {
          pos = (threshold - leftSum) / rightLeftDiff;
        }
        double r = leftPoint + (rightPoint - leftPoint) * pos;
        if (r < minValue) r = minValue;
        if (r > maxValue) r = maxValue;
        return r;
      }
    }
    return maxValue;
  }
}
