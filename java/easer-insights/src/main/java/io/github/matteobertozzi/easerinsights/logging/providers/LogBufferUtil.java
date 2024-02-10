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

import io.github.matteobertozzi.easerinsights.logging.LogBuffer;
import io.github.matteobertozzi.rednaco.strings.StringFormat;

public final class LogBufferUtil {
  private LogBufferUtil() {
    // no-op
  }

  public static abstract class AbstractLogBuffer implements LogBuffer {
    @Override
    public LogBuffer append(final CharSequence csq) {
      return csq != null ? append(csq, 0, csq.length()) : this;
    }

    @Override
    public LogBuffer append(final Object value) {
      return append(String.valueOf(value));
    }

    @Override
    public LogBuffer appendAscii(final CharSequence csq) {
      return csq != null ? appendAscii(csq, 0, csq.length()) : this;
    }

    @Override
    public LogBuffer appendAscii(final Object value) {
      return appendAscii(String.valueOf(value));
    }

    @Override
    public LogBuffer appendStackTrace(final Throwable exception) {
      append(exception.getClass().getName()).appendAscii(": ").append(exception.getMessage()).appendAscii(System.lineSeparator());
      appendStackTrace(exception.getStackTrace(), 0);
      for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
        appendAscii("Caused by: ").append(cause.getClass().getName()).appendAscii(": ").append(cause.getMessage()).appendAscii(System.lineSeparator());
        appendStackTrace(cause.getStackTrace(), 0);
      }
      return this;
    }

    @Override
    public LogBuffer appendStackTrace(final StackTraceElement[] stackTrace, final int fromLevel) {
      if (stackTrace == null) return this;

      for (int i = fromLevel; i < stackTrace.length; ++i) {
        final StackTraceElement st = stackTrace[i];
        appendAscii("\tat ").append(st.getClassName()).appendAscii('.').append(st.getMethodName());
        appendAscii('(').append(st.getFileName()).appendAscii(':').appendAscii(String.valueOf(st.getLineNumber())).appendAscii(')');
        appendAscii(System.lineSeparator());
      }
      return this;
    }
  }

  public static class StringBuilderLogBuffer extends AbstractLogBuffer {
    private final StringBuilder builder;

    public StringBuilderLogBuffer(final int initialCapacity) {
      this.builder = new StringBuilder(initialCapacity);
    }

    public StringBuilder getBuilder() {
      return builder;
    }

    public void reset() {
      this.builder.setLength(0);
    }

    @Override
    public void commitEntry() {
      builder.append(System.lineSeparator());
    }

    @Override
    public LogBuffer append(final char ch) {
      builder.append(ch);
      return this;
    }

    @Override
    public LogBuffer append(final CharSequence csq, final int start, final int end) {
      builder.append(csq, start, end);
      return this;
    }

    @Override
    public LogBuffer appendAscii(final char ch) {
      builder.append(ch);
      return this;
    }

    @Override
    public LogBuffer appendAscii(final CharSequence csq, final int start, final int end) {
      builder.append(csq, start, end);
      return this;
    }

    public LogBuffer appendNamedFormat(final String format, final Object[] args) {
      StringFormat.applyNamedFormat(builder, format, args);
      return this;
    }
  }
}
