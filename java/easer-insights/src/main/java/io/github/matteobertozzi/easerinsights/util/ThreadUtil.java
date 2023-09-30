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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ThreadUtil {
  private static final Logger LOGGER = Logger.getLogger("ThreadUtil");

  private ThreadUtil() {
    // no-op
  }

  @FunctionalInterface
  public interface RunnableWithException {
    void run() throws Exception;
  }

  public static <T> T poll(final BlockingQueue<T> queue, final int timeout, final TimeUnit unit) {
    try {
      return queue.poll(timeout, unit);
    } catch (final InterruptedException e) {
      return null;
    }
  }

  public static void ignoreException(final String name, final String action, final RunnableWithException r) {
    try {
      r.run();
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "failed while " + name + " was " + action + ": " + e.getMessage(), e);
    }
  }
}
