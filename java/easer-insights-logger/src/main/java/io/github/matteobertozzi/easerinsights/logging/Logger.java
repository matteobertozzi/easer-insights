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

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;

import io.github.matteobertozzi.easerinsights.logging.LogProvider.LogEntry;
import io.github.matteobertozzi.easerinsights.tracing.Tracer;

public final class Logger {
  public static final CopyOnWriteArraySet<String> EXCLUDE_CLASSES = new CopyOnWriteArraySet<>();

  // ===============================================================================================
  //  Log Provider related
  // ===============================================================================================
  private static LogProvider logProvider = NoOpLogProvider.INSTANCE;

  public static void setLogProvider(final LogProvider logProvider) {
    Logger.logProvider = logProvider;
  }

  // ===============================================================================================
  //  Log Levels related
  // ===============================================================================================
  public enum LogLevel {
    ALWAYS,
    FATAL,      // system is unusable
    ALERT,      // action must be taken immediately
    CRITICAL,   // critical conditions
    ERROR,      // error conditions
    WARNING,    // warning conditions
    INFO,       // informational
    DEBUG,      // debug-level messages
    TRACE,      // very verbose debug level messages
    NEVER,
  }

  // ===============================================================================================
  //  Logging methods
  // ===============================================================================================
  public static void always(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.ALWAYS, exception, format, args);
  }

  public static void always(final String format, final Object... args) {
    log(LogLevel.ALWAYS, format, args);
  }

  public static void fatal(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.FATAL, exception, format, args);
  }

  public static void fatal(final String format, final Object... args) {
    log(LogLevel.FATAL, format, args);
  }

  public static void alert(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.ALERT, exception, format, args);
  }

  public static void alert(final String format, final Object... args) {
    log(LogLevel.ALERT, format, args);
  }

  public static void critical(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.CRITICAL, exception, format, args);
  }

  public static void critical(final String format, final Object... args) {
    log(LogLevel.CRITICAL, format, args);
  }

  public static void error(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.ERROR, exception, format, args);
  }

  public static void error(final String format, final Object... args) {
    log(LogLevel.ERROR, format, args);
  }

  public static void warn(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.WARNING, exception, format, args);
  }

  public static void warn(final String format, final Object... args) {
    log(LogLevel.WARNING, format, args);
  }

  public static void warn(final String format, final Supplier<?>... args) {
    log(LogLevel.WARNING, format, convertArgs(args));
  }

  public static void info(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.INFO, exception, format, args);
  }

  public static void info(final String format, final Object... args) {
    log(LogLevel.INFO, format, args);
  }

  public static void info(final String format, final Supplier<?>... args) {
    log(LogLevel.INFO, format, convertArgs(args));
  }

  public static void debug(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.DEBUG, exception, format, args);
  }

  public static void debug(final String format, final Object... args) {
    log(LogLevel.DEBUG, format, args);
  }

  public static void debug(final String format, final Supplier<?>... args) {
    log(LogLevel.DEBUG, format, convertArgs(args));
  }

  public static void trace(final Throwable exception, final String format, final Object... args) {
    log(LogLevel.TRACE, exception, format, args);
  }

  public static void trace(final String format, final Object... args) {
    log(LogLevel.TRACE, format, args);
  }

  public static void trace(final String format, final Supplier<?>... args) {
    log(LogLevel.TRACE, format, convertArgs(args));
  }

  public static void raw(final LogEntry entry) {
    logProvider.logEntry(entry);
  }

  // ===============================================================================================
  private static void log(final LogLevel level, final String format, final Object[] args) {
    logProvider.logMessage(Tracer.getThreadLocalSpan(), level, format, args);
  }

  private static void log(final LogLevel level, final Throwable exception, final String format, final Object[] args) {
    logProvider.logMessage(Tracer.getThreadLocalSpan(), level, exception, format, args);
  }

  private static Object[] convertArgs(final Supplier<?>[] argSuppliers) {
    final Object[] args = new Object[argSuppliers.length];
    for (int i = 0; i < args.length; ++i) {
      args[i] = argSuppliers[i].get();
    }
    return args;
  }

  // ===============================================================================================
  @FunctionalInterface
  public interface ExecutableFunction {
    void run() throws Throwable;
  }

  public static void ignoreException(final String name, final String action, final ExecutableFunction r) {
    try {
      r.run();
    } catch (final Throwable e) {
      warn(e, "failed while {} was {}", name, action);
    }
  }
}
