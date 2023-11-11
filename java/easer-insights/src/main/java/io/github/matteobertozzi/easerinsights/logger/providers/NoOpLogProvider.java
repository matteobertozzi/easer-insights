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

package io.github.matteobertozzi.easerinsights.logger.providers;

import io.github.matteobertozzi.easerinsights.logger.LogProvider;
import io.github.matteobertozzi.easerinsights.logger.Logger.LogLevel;

public final class NoOpLogProvider implements LogProvider {
  public static final NoOpLogProvider INSTANCE = new NoOpLogProvider();

  private NoOpLogProvider() {
    // no-op
  }

  @Override
  public void logMessage(final LogLevel level, final Throwable exception, final String format, final Object[] args) {
    // no-op
  }

  @Override
  public void logEntry(final LogEntry entry) {
    // no-op
  }
}
