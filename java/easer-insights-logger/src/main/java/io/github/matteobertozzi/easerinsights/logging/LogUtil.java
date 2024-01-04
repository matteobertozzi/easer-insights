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

package io.github.matteobertozzi.easerinsights.logging;

import java.lang.StackWalker.StackFrame;

import io.github.matteobertozzi.easerinsights.logging.Logger.LogLevel;

public final class LogUtil {
  private LogUtil() {
    // no-op
  }

  // ===============================================================================================
  //  Log Levels related
  // ===============================================================================================
  private static final LogLevel[] LOG_LEVELS = LogLevel.values();
  public static final int LOG_LEVELS_COUNT = LOG_LEVELS.length;

  public static LogLevel levelFromOrdinal(final int ordinal) {
    return LOG_LEVELS[ordinal];
  }

  // ===============================================================================================
  //  Stack Trace to String related
  // ===============================================================================================
  private static boolean shouldKeep(final StackFrame frame) {
    return !Logger.EXCLUDE_CLASSES.contains(frame.getClassName());
  }

  public static String lookupLineClassAndMethod(final int skipFrames) {
    // Get the stack trace: this is expensive... but really useful
    // NOTE: i should be set to the first public method
    // skipFrames = 2 -> [0: lookupLogLineClassAndMethod(), 1: myLogger(), 2:userFunc()]
    final StackFrame frame = StackWalker.getInstance().walk(s ->
      s.skip(skipFrames)
      .filter(LogUtil::shouldKeep)
      .findFirst()
    ).orElseThrow();

    // com.foo.Bar.m1():11
    return getClassName(frame.getClassName()) + '.' + frame.getMethodName() + "():" + frame.getLineNumber();
  }

  public static String lookupClassAndMethod(final int skipFrames) {
    // Get the stack trace: this is expensive... but really useful
    // NOTE: i should be set to the first public method
    // skipFrames = 2 -> [0: lookupLogLineClassAndMethod(), 1: myLogger(), 2:userFunc()]
    final StackFrame frame = StackWalker.getInstance().walk(s ->
      s.skip(skipFrames)
      .filter(LogUtil::shouldKeep)
      .findFirst()
    ).orElseThrow();

    // com.foo.Bar.m1()
    return getClassName(frame.getClassName()) + '.' + frame.getMethodName() + "()";
  }

  private static String getClassName(final String cname) {
    final int index = cname.length();

    final int aIndex = cname.lastIndexOf('.', index - 1);
    if (aIndex < 0) return cname.substring(index + 1);

    final int bIndex = cname.lastIndexOf('.', aIndex - 1);
    return cname.substring(((bIndex < 0) ? aIndex : bIndex) + 1);
  }

  public static String stackTraceToString(final Throwable exception) {
    return appendStackTrace(new StringBuilder(512), exception).toString();
  }

  public static String stackTraceToString(final StackTraceElement[] stackTrace, final int fromLevel) {
    return appendStackTrace(new StringBuilder(), stackTrace, fromLevel).toString();
  }

  public static StringBuilder appendStackTrace(final StringBuilder builder, final Throwable exception) {
    builder.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append(System.lineSeparator());
    return appendStackTrace(builder, exception.getStackTrace(), 0);
  }

  public static StringBuilder appendStackTrace(final StringBuilder builder, final StackTraceElement[] stackTrace, final int fromLevel) {
    if (stackTrace == null) return builder;

    for (int i = fromLevel; i < stackTrace.length; ++i) {
      final StackTraceElement st = stackTrace[i];
      builder.append("\tat ").append(st.getClassName()).append('.').append(st.getMethodName());
      builder.append('(').append(st.getFileName()).append(':').append(st.getLineNumber()).append(')');
      builder.append(System.lineSeparator());
    }
    return builder;
  }
}
