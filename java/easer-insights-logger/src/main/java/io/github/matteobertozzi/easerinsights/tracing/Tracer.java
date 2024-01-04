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

package io.github.matteobertozzi.easerinsights.tracing;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.github.matteobertozzi.easerinsights.logging.LogUtil;
import io.github.matteobertozzi.easerinsights.logging.Logger;

public final class Tracer {
  static {
    Logger.EXCLUDE_CLASSES.add(Tracer.class.getName());
  }

  private static TraceIdProvider traceIdProvider = NoOpTracer.TRACE_ID_PROVIDER;
  private static SpanIdProvider spanIdProvider = NoOpTracer.SPAN_ID_PROVIDER;
  private static TraceProvider traceProvider = NoOpTracer.INSTANCE;
  private static RootSpan nullSpan = NoOpTracer.NULL_SPAN_INSTANCE;

  private Tracer() {
    // no-op
  }

  // ========================================================================================================================
  //  Set Trace Providers
  // ========================================================================================================================
  public static void setTraceProvider(final TraceProvider traceProvider) {
    Tracer.traceProvider = traceProvider;
  }

  public static void setIdProviders(final TraceIdProvider traceIdProvider, final SpanIdProvider spanIdProvider) {
    Tracer.traceIdProvider = traceIdProvider;
    Tracer.spanIdProvider = spanIdProvider;
    Tracer.nullSpan = new NullSpan(traceIdProvider.nullTraceId(), spanIdProvider.nullSpanId());
  }

  public static void setProviders(final TraceProvider traceProvider, final TraceIdProvider traceIdProvider, final SpanIdProvider spanIdProvider) {
    setTraceProvider(traceProvider);
    setIdProviders(traceIdProvider, spanIdProvider);
  }

  // ========================================================================================================================
  //  Root Span related
  // ========================================================================================================================
  public static RootSpan newRootSpan() {
    return newRootSpan(traceIdProvider.newTraceId(), spanIdProvider.nullSpanId());
  }

  public static RootSpan newRootSpan(final String traceId, final String parentId) {
    return newRootSpan(traceIdProvider.parseTraceId(traceId), spanIdProvider.parseSpanId(parentId));
  }

  public static RootSpan newRootSpan(final byte[] traceId, final byte[] parentId) {
    return newRootSpan(traceIdProvider.parseTraceId(traceId), spanIdProvider.parseSpanId(parentId));
  }

  public static RootSpan newRootSpan(final TraceId traceId, final SpanId parentId) {
    final RootSpan span = traceProvider.openRootSpan(traceId, parentId, spanIdProvider.newSpanId());
    addToLocalSpanStack(localSpanStack.get(), span);
    notifyRootSpanListeners(span);
    return span;
  }

  // ========================================================================================================================
  //  Span related
  // ========================================================================================================================
  public static Span newSpan(final Span parent) {
    final Span span = traceProvider.openSpan(parent.rootSpan(), parent.spanId(), spanIdProvider.newSpanId());
    addToLocalSpanStack(localSpanStack.get(), span);
    return span;
  }

  // ========================================================================================================================
  //  Thread-Local Span related
  // ========================================================================================================================
  private static final ThreadLocal<ArrayList<Span>> localSpanStack = ThreadLocal.withInitial(ArrayList::new);

  private static void addToLocalSpanStack(final ArrayList<Span> spanStack, final Span span) {
    TraceAttributes.CODE_METHOD_CALLER.set(span, LogUtil.lookupClassAndMethod(3));
    spanStack.add(span);
    notifyNewSpanListeners(span);
  }

  public static Span newThreadLocalSpan() {
    final ArrayList<Span> spanStack = localSpanStack.get();
    if (spanStack.isEmpty()) {
      throw new IllegalArgumentException("expected a thread-local Span");
    }

    final Span parent = spanStack.getLast();
    final Span span = traceProvider.openSpan(parent.rootSpan(), parent.spanId(), spanIdProvider.newSpanId());
    addToLocalSpanStack(spanStack, span);
    return span;
  }

  public static Span getThreadLocalSpan() {
    final ArrayList<Span> spanStack = localSpanStack.get();
    if (spanStack.isEmpty()) {
      return Tracer.nullSpan;
    }
    return spanStack.getLast();
  }

  public static void closeSpan(final Span span) {
    final ArrayList<Span> spanStack = localSpanStack.get();
    if (spanStack.isEmpty()) {
      Logger.alert(new IllegalStateException(), "invalid thread-local span stack state. closing {} stack: {}", span, spanStack);
      return;
    }

    if (span != spanStack.getLast()) {
      Logger.alert(new IllegalStateException(), "invalid thread-local span stack state. closing {} stack: {}", span, spanStack);
      return;
    }

    spanStack.removeLast();
    Tracer.traceProvider.closeSpan(span);

    if (span instanceof final RootSpan rootSpan) {
      notifyRootSpanListeners(rootSpan);
    }
  }

  // ========================================================================================================================
  //  Listeners
  // ========================================================================================================================
  private static final CopyOnWriteArrayList<Consumer<RootSpan>> rootSpanListeners = new CopyOnWriteArrayList<>();
  private static final CopyOnWriteArrayList<Consumer<Span>> newSpanListeners = new CopyOnWriteArrayList<>();

  public static void subscribeToRootSpanEvents(final Consumer<RootSpan> listener) {
    rootSpanListeners.add(listener);
  }

  public static void subscribeToNewSpanEvents(final Consumer<Span> listener) {
    newSpanListeners.add(listener);
  }

  private static void notifyRootSpanListeners(final RootSpan span) {
    if (rootSpanListeners.isEmpty()) return;
    for (final Consumer<RootSpan> consumer: rootSpanListeners) {
      consumer.accept(span);
    }
  }

  private static void notifyNewSpanListeners(final Span span) {
    if (newSpanListeners.isEmpty()) return;
    for (final Consumer<Span> consumer: newSpanListeners) {
      consumer.accept(span);
    }
  }
}
