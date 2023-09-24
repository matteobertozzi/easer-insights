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

package io.github.matteobertozzi.easerinsights.metrics.collectors;

class HistogramImplSt extends Histogram {
  private final long[] events;
  private long minValue;
  private long maxValue;
  private long sumSquares;
  private long sum;

  protected HistogramImplSt(final long[] bounds) {
    super(bounds);
    this.events = new long[bounds.length + 1];
    this.minValue = Long.MAX_VALUE;
    this.maxValue = Long.MIN_VALUE;
    this.sumSquares = 0;
    this.sum = 0;
  }

  @Override
  protected void add(final int boundIndex, final long value) {
    events[boundIndex]++;
    minValue = Math.min(minValue, value);
    maxValue = Math.max(maxValue, value);
    sum += value;
    sumSquares += (value * value);
  }

  @Override
  public MetricDataSnapshot snapshot() {
    long numEvents = 0;
    for (int i = 0; i < events.length; i++) {
      numEvents += events[i];
    }

    if (numEvents == 0) return Histogram.EMPTY_SNAPSHOT;

    int firstBound = 0;
    int lastBound = events.length - 1;

    while (events[firstBound] == 0) firstBound++;
    while (events[lastBound] == 0) lastBound--;

    final int numBounds = 1 + (lastBound - firstBound);
    final long[] snapshotBounds = new long[numBounds];
    final long[] snapshotEvents = new long[numBounds];
    for (int i = firstBound; i < lastBound; ++i) {
      snapshotBounds[i] = bound(i);
      snapshotEvents[i] = events[i];
    }
    snapshotBounds[numBounds - 1] = maxValue;
    snapshotEvents[numBounds - 1] = events[lastBound];

    return new HistogramSnapshot(snapshotBounds, snapshotEvents, numEvents, minValue, sum, sumSquares);
  }
}
