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

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import io.github.matteobertozzi.easerinsights.logging.LogBuffer;
import io.github.matteobertozzi.easerinsights.logging.LogProvider;
import io.github.matteobertozzi.easerinsights.logging.LogUtil;
import io.github.matteobertozzi.easerinsights.logging.Logger.LogLevel;
import io.github.matteobertozzi.easerinsights.logging.providers.AsyncTextLogWriter.AsyncTextLogBuffer;
import io.github.matteobertozzi.easerinsights.logging.providers.LogBufferUtil.StringBuilderLogBuffer;
import io.github.matteobertozzi.easerinsights.tracing.RootSpan;
import io.github.matteobertozzi.easerinsights.tracing.Span;
import io.github.matteobertozzi.easerinsights.tracing.Tracer;
import io.github.matteobertozzi.rednaco.strings.StringFormat;
import io.github.matteobertozzi.rednaco.threading.BenchUtil;
import io.github.matteobertozzi.rednaco.threading.StripedLock;

public abstract class TextLogProvider implements LogProvider {
  private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  public static TextLogProvider newAsyncProvider(final AsyncTextLogWriter asyncWriter) {
    return new AsyncTextLogProvider(asyncWriter);
  }

  public static TextLogProvider newStreamProvider(final PrintStream stream) {
    return new PrintStreamTextLogProvider(stream);
  }

  private static class AsyncTextLogProvider extends TextLogProvider {
    private final AsyncTextLogWriter asyncLogWriter;

    private AsyncTextLogProvider(final AsyncTextLogWriter asyncLogWriter) {
      this.asyncLogWriter = asyncLogWriter;
    }

    @Override
    public void logMessage(final Span span, final LogLevel level, final String format, final Object[] args) {
      logMessage(span, level, null, format, args);
    }

    @Override
    public void logMessage(final Span span, final LogLevel level, final Throwable exception, final String format, final Object[] args) {
      final String dateTime = LOG_DATE_FORMAT.format(ZonedDateTime.now());
      final String lineClassAndMethod = LogUtil.lookupLineClassAndMethod(5);
      final String traceId = String.valueOf(span.traceId());
      final String spanId = String.valueOf(span.spanId());
      final Thread thread = Thread.currentThread();
      final String threadName = thread.getName();
      final String threadId = String.valueOf(thread.threadId());
      final RootSpan rootSpan = span.rootSpan();
      final String module = rootSpan.module();
      final String tenantId = rootSpan.tenantId();
      final String ownerId = rootSpan.ownerId();
      final String message = args.length == 0 ? format : StringFormat.namedFormat(format, args);

      final StripedLock.Cell<AsyncTextLogBuffer> cell = asyncLogWriter.get();
      cell.lock();
      try {
        final AsyncTextLogBuffer logBuffer = cell.data();
        addLogMessage(logBuffer,
          dateTime, traceId, spanId, threadName, threadId, module, tenantId, ownerId,
          level, lineClassAndMethod, message
        );
        if (exception != null) {
          logBuffer.appendAscii(" - ").append(exception.getMessage()).append(System.lineSeparator());
          logBuffer.appendStackTrace(exception);
        }
        logBuffer.commitEntry();
      } finally {
        cell.unlock();
      }
    }

    @Override
    public void logEntry(final LogEntry entry) {
      final StripedLock.Cell<AsyncTextLogBuffer> cell = asyncLogWriter.get();
      cell.lock();
      try {
        final AsyncTextLogBuffer logBuffer = cell.data();
        entry.writeTextTo(logBuffer);
        logBuffer.commitEntry();
      } finally {
        cell.unlock();
      }
    }
  }

  private static class PrintStreamTextLogProvider extends TextLogProvider {
    private final PrintStream stream;

    private PrintStreamTextLogProvider(final PrintStream stream) {
      this.stream = stream;
    }

    @Override
    public void logMessage(final Span span, final LogLevel level, final String format, final Object[] args) {
      final StringBuilderLogBuffer logBuffer = new StringBuilderLogBuffer(32 + format.length());
      addLogMessage(logBuffer, span, level, format, args);
      logBuffer.commitEntry();
      stream.print(logBuffer.getBuilder());
    }

    @Override
    public void logMessage(final Span span, final LogLevel level, final Throwable exception, final String format, final Object[] args) {
      final StringBuilderLogBuffer logBuffer = new StringBuilderLogBuffer(32 + format.length());
      addLogMessage(logBuffer, span, level, format, args);
      if (exception != null) {
        logBuffer.append(" - ").append(exception.getMessage()).append(System.lineSeparator());
        logBuffer.appendStackTrace(exception);
      }
      logBuffer.commitEntry();
      stream.print(logBuffer.getBuilder());
    }

    private static void addLogMessage(final StringBuilderLogBuffer buffer, final Span span, final LogLevel level, final String format, final Object[] args) {
      final Thread thread = Thread.currentThread();
      final String threadName = thread.getName();
      final String threadId = String.valueOf(thread.threadId());
      final RootSpan rootSpan = span.rootSpan();
      buffer.append(LOG_DATE_FORMAT.format(ZonedDateTime.now()));
      buffer.append(" [").append(span.traceId()).append(':').append(span.spanId());
      buffer.appendAscii(':').append(threadName).appendAscii(':').appendAscii(threadId);
      buffer.append(':').append(rootSpan.module()).append(':').append(rootSpan.tenantId()).append(':').append(rootSpan.ownerId());
      buffer.append("] ");
      buffer.append(level.name()).append(' ').append(LogUtil.lookupLineClassAndMethod(5)).append(" - ");
      buffer.appendNamedFormat(format, args);
    }

    @Override
    public void logEntry(final LogEntry entry) {
      final StringBuilderLogBuffer logBuffer = new StringBuilderLogBuffer(80);
      entry.writeTextTo(logBuffer);
      logBuffer.commitEntry();
      stream.print(logBuffer.getBuilder());
    }
  }

  private static void addLogMessage(final LogBuffer buffer, final String dateTime, final String traceId, final String spanId,
      final String threadName, final String threadId, final String module, final String tenantId, final String ownerId,
      final LogLevel level, final String lineClassAndMethod, final String message) {
    buffer.appendAscii(dateTime);
    buffer.appendAscii(" [").appendAscii(traceId).appendAscii(':').appendAscii(spanId);
    buffer.appendAscii(':').append(threadName).appendAscii(':').appendAscii(threadId);
    buffer.appendAscii(':').append(module).appendAscii(':').append(tenantId).appendAscii(':').append(ownerId).appendAscii("] ");
    buffer.append(level.name()).appendAscii(' ');
    buffer.append(lineClassAndMethod).appendAscii(" - ").append(message);
  }

  public static void main(final String[] args) throws Throwable {
    try (PrintStream stream = new PrintStream(OutputStream.nullOutputStream())) {
      try (AsyncTextLogWriter asyncLogWriter = new AsyncTextLogWriter(stream, 8192)) {
        final TextLogProvider textLog = TextLogProvider.newAsyncProvider(asyncLogWriter);
        //final TextLogProvider textLog = TextLogProvider.newStreamProvider(stream);
        BenchUtil.runInThreads("print", 4, 1_000_000, () -> {
          final Span span = Tracer.getThreadLocalSpan();
          for (int i = 0; i < 1; ++i) {
            textLog.logMessage(span, LogLevel.INFO, "hello world this is a tesht", new Object[0]);
          }
        });
      }
    }
  }
}
