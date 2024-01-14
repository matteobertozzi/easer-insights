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

package io.github.matteobertozzi.easerinsights.profiler;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import io.github.matteobertozzi.easerinsights.logging.Logger;
import io.github.matteobertozzi.rednaco.strings.HumansUtil;
import io.github.matteobertozzi.rednaco.time.ByteUnit;
import one.profiler.AsyncProfiler;
import one.profiler.Events;

public class ProfilerService {
  public static final ProfilerService INSTANCE = new ProfilerService();

  private final ReentrantLock lock = new ReentrantLock();
  private final HashSet<String> includes = new HashSet<>();
  private final HashSet<String> excludes = new HashSet<>();
  private String flamegraphCommand = null;
  private String currentRecording = null;

  private ProfilerService() {
    // no-op
  }

  public void includeClassPath(final String classPath) {
    lock.lock();
    try {
      flamegraphCommand = null;
      final String path = classPath.replace('.', '/');
      excludes.remove(path);
      includes.add(path);
    } finally {
      lock.unlock();
    }
  }

  public void excludeClassPath(final String classPath) {
    lock.lock();
    try {
      flamegraphCommand = null;
      final String path = classPath.replace('.', '/');
      includes.remove(path);
      excludes.add(path);
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Start CPU Recording related
  // ====================================================================================================
  public void startCpuRecording() {
    startCpuRecording(1_000_000L);
  }

  public void startCpuRecording(final Duration interval) {
    startCpuRecording(interval.toNanos());
  }

  public void startCpuRecording(final long intervalNs) {
    lock.lock();
    try {
      if (currentRecording != null) {
        throw new IllegalStateException(currentRecording + " recoding is already active");
      }
      AsyncProfiler.getInstance().start(Events.CPU, intervalNs);
      currentRecording = Events.CPU;
      Logger.debug("start async-profiler CPU recording interval {}", HumansUtil.humanTimeNanos(intervalNs));
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Start WALL Recording related
  // ====================================================================================================
  public void startWallRecording() {
    startWallRecording(1_000_000L);
  }

  public void startWallRecording(final Duration interval) {
    startCpuRecording(interval.toNanos());
  }

  public void startWallRecording(final long intervalNs) {
    lock.lock();
    try {
      if (currentRecording != null) {
        throw new IllegalStateException(currentRecording + " recoding is already active");
      }
      AsyncProfiler.getInstance().start(Events.WALL, intervalNs);
      currentRecording = Events.WALL;
      Logger.debug("start async-profiler WALL recording interval {}", HumansUtil.humanTimeNanos(intervalNs));
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Start Lock Recording related
  // ====================================================================================================
  public void startLockRecording() {
    startLockRecording(1_000_000L);
  }

  public void startLockRecording(final Duration interval) {
    startLockRecording(interval.toNanos());
  }

  public void startLockRecording(final long intervalNs) {
    lock.lock();
    try {
      if (currentRecording != null) {
        throw new IllegalStateException(currentRecording + " recoding is already active");
      }
      AsyncProfiler.getInstance().start(Events.LOCK, intervalNs);
      currentRecording = Events.LOCK;
      Logger.debug("start async-profiler LOCK recording interval {}", HumansUtil.humanTimeNanos(intervalNs));
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Start ALLOC Recording related
  // ====================================================================================================
  public void startAllocRecording() {
    startAllocRecording(1L << 20);
  }

  public void startAllocRecording(final ByteUnit interval) {
    startAllocRecording(interval.toBytes());
  }

  public void startAllocRecording(final long intervalBytes) {
    lock.lock();
    try {
      if (currentRecording != null) {
        throw new IllegalStateException(currentRecording + " recoding is already active");
      }
      AsyncProfiler.getInstance().start(Events.ALLOC, intervalBytes);
      currentRecording = Events.ALLOC;
      Logger.debug("start async-profiler Memory recording interval {}", HumansUtil.humanBytes(intervalBytes));
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Stop Recording related
  // ====================================================================================================
  public void stopRecording() {
    lock.lock();
    try {
      if (currentRecording != null) {
        AsyncProfiler.getInstance().stop();
        currentRecording = null;
      }
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Flamegraph related
  // ====================================================================================================
  public String flamegraphHtml() throws IllegalStateException {
    lock.lock();
    try {
      if (currentRecording == null) {
        throw new IllegalStateException("profiler recoding is not active");
      }
      return AsyncProfiler.getInstance().execute(flamegraphCommand());
    } catch (final IllegalArgumentException | IOException e) {
      throw new IllegalStateException(e);
    } finally {
      lock.unlock();
    }
  }

  private String flamegraphCommand() {
    if (flamegraphCommand != null) return flamegraphCommand;

    final StringBuilder cmd = new StringBuilder();
    cmd.append("flamegraph");
    if (!includes.isEmpty()) {
      for (final String path: includes) {
        cmd.append(",include=").append(path);
      }
    }
    if (!excludes.isEmpty()) {
      for (final String path: includes) {
        cmd.append(",exclude=").append(path);
      }
    }

    return flamegraphCommand = cmd.toString();
  }

  public String flamegraphTextData() {
    lock.lock();
    try {
      if (currentRecording == null) {
        throw new IllegalStateException("profiler recoding is not active");
      }
      return AsyncProfiler.getInstance().execute("collapsed,total");
    } catch (final IllegalArgumentException | IOException e) {
      throw new IllegalStateException(e);
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Hot Methods histogram related
  // ====================================================================================================
  public String hotMethodsTextData(final int maxMethods) {
    lock.lock();
    try {
      if (currentRecording == null) {
        throw new IllegalStateException("profiler recoding is not active");
      }
      return AsyncProfiler.getInstance().execute("flat=" + maxMethods);
    } catch (final IllegalArgumentException | IOException e) {
      throw new IllegalStateException(e);
    } finally {
      lock.unlock();
    }
  }

  // ====================================================================================================
  //  Call Traces related
  // ====================================================================================================
  public String callTracesTextData(final int nSamples) {
    lock.lock();
    try {
      if (currentRecording == null) {
        throw new IllegalStateException("profiler recoding is not active");
      }
      return AsyncProfiler.getInstance().execute("traces=" + nSamples);
    } catch (final IllegalArgumentException | IOException e) {
      throw new IllegalStateException(e);
    } finally {
      lock.unlock();
    }
  }


  // ====================================================================================================
  //  Call Tree related
  // ====================================================================================================
  public String treeHtml() {
    lock.lock();
    try {
      if (currentRecording == null) {
        throw new IllegalStateException("profiler recoding is not active");
      }
      return AsyncProfiler.getInstance().execute("tree");
    } catch (final IllegalArgumentException | IOException e) {
      throw new IllegalStateException(e);
    } finally {
      lock.unlock();
    }
  }
}
