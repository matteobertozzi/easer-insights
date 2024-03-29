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

package io.github.matteobertozzi.easerinsights.metrics.collectors.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class TopKImplMt extends TopKImplSt {
  private final ReentrantLock lock = new ReentrantLock(true);

  public TopKImplMt(final int k, final long maxInterval, final long window, final TimeUnit unit) {
    super(k, maxInterval, window, unit);
  }

  @Override
  public void sample(final String key, final long timestamp, final long value) {
    lock.lock();
    try {
      super.sample(key, timestamp, value);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public TopKSnapshot dataSnapshot() {
    final TopList topEntries;
    lock.lock();
    try {
      topEntries = super.dataSnapshotTopList();
    } finally {
      lock.unlock();
    }
    return snapshotFrom(topEntries);
  }
}
