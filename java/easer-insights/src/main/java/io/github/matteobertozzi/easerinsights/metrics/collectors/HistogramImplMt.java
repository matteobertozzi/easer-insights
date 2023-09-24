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

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

class HistogramImplMt extends HistogramImplSt {
  // TODO: bring back the striped-lock implementation
  private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock(true);

  protected HistogramImplMt(final long[] bounds) {
    super(bounds);
  }

  @Override
  protected void add(final int boundIndex, final long value) {
    final WriteLock lock = rwlock.writeLock();
    lock.lock();
    try {
      super.add(boundIndex, value);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public MetricDataSnapshot snapshot() {
    final ReadLock lock = rwlock.readLock();
    lock.lock();
    try {
      return super.snapshot();
    } finally {
      lock.unlock();
    }
  }
}
