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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

import io.github.matteobertozzi.easerinsights.tracing.Span.SpanState;

public final class TraceRecorder {
  public static final TraceRecorder INSTANCE = new TraceRecorder();

  private static final ConcurrentHashMap<RootSpan, ConcurrentLinkedQueue<Span>> liveTraces = new ConcurrentHashMap<>();

  private static final PriorityBlockingQueue<Traces> topTraces = new PriorityBlockingQueue<>(16, (a, b) -> Long.compare(a.elapsedNanos(), b.elapsedNanos()));
  public record Traces(RootSpan rootSpan, Span[] spans) {
    public long elapsedNanos() {
      return rootSpan().elapsedNanos();
    }
  }

  private TraceRecorder() {
    Tracer.subscribeToNewSpanEvents(this::addSpan);
    Tracer.subscribeToRootSpanEvents(this::closeRootSpan);
  }

  private void addSpan(final Span span) {
    if (span instanceof final RootSpan rootSpan) {
      liveTraces.put(rootSpan, new ConcurrentLinkedQueue<>());
    } else {
      liveTraces.get(span.rootSpan()).add(span);
    }
  }

  private void closeRootSpan(final RootSpan span) {
    if (span.state() == SpanState.IN_PROGRESS) return;

    final ConcurrentLinkedQueue<Span> traces = liveTraces.remove(span);
    topTraces.add(new Traces(span, traces.toArray(new Span[0])));
    if (topTraces.size() > 16) {
      topTraces.poll();
    }
  }

  public Traces[] snapshot() {
    return topTraces.toArray(new Traces[0]);
  }
}
