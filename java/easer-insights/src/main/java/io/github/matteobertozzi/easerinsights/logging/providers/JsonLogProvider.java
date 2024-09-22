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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import io.github.matteobertozzi.easerinsights.logging.LogProvider;
import io.github.matteobertozzi.easerinsights.logging.LogUtil;
import io.github.matteobertozzi.easerinsights.logging.Logger.LogLevel;
import io.github.matteobertozzi.easerinsights.tracing.RootSpan;
import io.github.matteobertozzi.easerinsights.tracing.Span;
import io.github.matteobertozzi.easerinsights.tracing.TraceAttributes;
import io.github.matteobertozzi.rednaco.strings.StringFormat;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

public class JsonLogProvider implements LogProvider {
  private final JsonLogWriter jsonWriter;

  public JsonLogProvider(final JsonLogWriter jsonWriter) {
    this.jsonWriter = jsonWriter;
  }

  @Override
  public void logMessage(final Span span, final LogLevel level, final String format, final Object[] args) {
    addMessageEntry(span, level, StringFormat.namedFormat(format, args), null);
  }

  @Override
  public void logMessage(final Span span, final LogLevel level, final Throwable exception, final String format, final Object[] args) {
    final StringBuilder msgBuilder = new StringBuilder(32 + format.length());
    StringFormat.applyNamedFormat(msgBuilder, format, args);
    if (exception != null) {
      msgBuilder.append(" - ").append(exception.getMessage()).append(System.lineSeparator());
    }

    addMessageEntry(span, level, msgBuilder.toString(), exception != null ? LogUtil.stackTraceToString(exception) : null);
  }

  private void addMessageEntry(final Span span, final LogLevel level, final String message, final String exception) {
    final long nanos = TimeUtil.currentEpochNanos();
    final long hstamp = millisToHumanDate(nanos / 1_000_000L);
    final String traceId = span.traceId().toString();
    final String spanId = span.spanId().toString();
    final String thread = Thread.currentThread().getName();
    final String caller = LogUtil.lookupLineClassAndMethod(5);
    final RootSpan rootSpan = span.rootSpan();
    final String sessionId = TraceAttributes.SESSION_ID.get(rootSpan, null);
    jsonWriter.write(new LogMsgEntry(
        nanos, hstamp,
        traceId, spanId, thread,
        caller, rootSpan.module(),
        rootSpan.tenantId(), rootSpan.tenant(),
        rootSpan.ownerId(), rootSpan.owner(), sessionId,
        level, message, exception));
  }

  @Override
  public void logEntry(final LogEntry entry) {
    jsonWriter.write(entry);
  }

  record LogMsgEntry(
      long nanos, long hstamp,
      String traceId, String spanId, String thread,
      String caller, String module,
      String tenantId, String tenant, String ownerId, String owner, String sessionId,
      LogLevel level, Object message, String exception
  ) {
  }

  private static long millisToHumanDate(final long millis) {
    final ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    return (zdt.getYear() * 100_00_00_00_00L) + (zdt.getMonthValue() * 100_00_00_00L) + (zdt.getDayOfMonth() * 100_00_00)
        + (zdt.getHour() * 100_00) + (zdt.getMinute() * 100) + zdt.getSecond();
  }

  @FunctionalInterface
  public interface JsonLogWriter {
    void write(Object entry);
  }
}
