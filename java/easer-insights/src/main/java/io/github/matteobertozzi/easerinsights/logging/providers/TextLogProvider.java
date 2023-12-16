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

package io.github.matteobertozzi.easerinsights.logging.providers;

import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import io.github.matteobertozzi.easerinsights.logging.LogProvider;
import io.github.matteobertozzi.easerinsights.logging.LogUtil;
import io.github.matteobertozzi.easerinsights.logging.Logger.LogLevel;
import io.github.matteobertozzi.easerinsights.tracing.RootSpan;
import io.github.matteobertozzi.easerinsights.tracing.Span;
import io.github.matteobertozzi.rednaco.strings.StringFormat;

public class TextLogProvider implements LogProvider {
  private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private final PrintStream stream;

  public TextLogProvider() {
    this(System.out);
  }

  public TextLogProvider(final PrintStream stream) {
    this.stream = stream;
  }

  @Override
  public void logMessage(final Span span, final LogLevel level, final String format, final Object[] args) {
    final StringBuilder msgBuilder = new StringBuilder(32 + format.length());
    addLogMessage(msgBuilder, span, level, format, args);
    stream.println(msgBuilder);
  }

  @Override
  public void logMessage(final Span span, final LogLevel level, final Throwable exception, final String format, final Object[] args) {
    final StringBuilder msgBuilder = new StringBuilder(32 + format.length());
    addLogMessage(msgBuilder, span, level, format, args);
    if (exception != null) {
      msgBuilder.append(" - ").append(exception.getMessage()).append(System.lineSeparator());
      LogUtil.appendStackTrace(msgBuilder, exception);
    }
    stream.println(msgBuilder);
  }

  private static void addLogMessage(final StringBuilder msgBuilder, final Span span, final LogLevel level, final String format, final Object[] args) {
    final RootSpan rootSpan = span.rootSpan();
    msgBuilder.append(LOG_DATE_FORMAT.format(ZonedDateTime.now()));
    msgBuilder.append(" [");
    msgBuilder.append(span.traceId());
    msgBuilder.append(':');
    msgBuilder.append(span.spanId());
    msgBuilder.append(':');
    msgBuilder.append(rootSpan.module());
    msgBuilder.append(':');
    msgBuilder.append(rootSpan.tenantId());
    msgBuilder.append(':');
    msgBuilder.append(rootSpan.ownerId());
    msgBuilder.append("] ");
    msgBuilder.append(level.name());
    msgBuilder.append(' ');
    msgBuilder.append(LogUtil.lookupLineClassAndMethod(5));
    msgBuilder.append(" - ");
    StringFormat.applyNamedFormat(msgBuilder, format, args);
  }

  @Override
  public void logEntry(final LogEntry entry) {
    entry.writeTextTo(stream);
  }
}
