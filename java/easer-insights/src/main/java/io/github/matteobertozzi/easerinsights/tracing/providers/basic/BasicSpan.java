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

package io.github.matteobertozzi.easerinsights.tracing.providers.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.matteobertozzi.easerinsights.tracing.RootSpan;
import io.github.matteobertozzi.easerinsights.tracing.Span;
import io.github.matteobertozzi.easerinsights.tracing.SpanEvent;
import io.github.matteobertozzi.easerinsights.tracing.SpanId;
import io.github.matteobertozzi.easerinsights.tracing.TraceId;
import io.github.matteobertozzi.easerinsights.tracing.Tracer;
import io.github.matteobertozzi.rednaco.time.TimeUtil;

public class BasicSpan implements Span {
  private final Map<String, Object> attributes = new HashMap<>(0);
  private final ArrayList<SpanEvent> events = new ArrayList<>(0);

  private transient final RootSpan rootSpan;
  private final SpanId parentId;
  private final SpanId spanId;
  private final long startUnixNanos;

  private SpanState state = SpanState.IN_PROGRESS;
  private Throwable exception = null;
  private String name = null;
  private long endUnixNanos;

  public BasicSpan(final RootSpan rootSpan, final SpanId parentId, final SpanId spanId) {
    this.rootSpan = rootSpan;
    this.parentId = parentId;
    this.spanId = spanId;
    this.startUnixNanos = TimeUtil.currentEpochNanos();
  }

  @Override
  public RootSpan rootSpan() {
    return rootSpan;
  }

  @Override
  public TraceId traceId() {
    return rootSpan.traceId();
  }

  @Override
  public SpanId parentId() {
    return parentId;
  }

  @Override
  public SpanId spanId() {
    return spanId;
  }

  @Override
  public long startUnixNanos() {
    return startUnixNanos;
  }

  @Override
  public long endUnixNanos() {
    return endUnixNanos;
  }

  @Override
  public long elapsedNanos() {
    if (endUnixNanos < 0) {
      return TimeUtil.currentEpochNanos() - startUnixNanos;
    }
    return endUnixNanos - startUnixNanos;
  }

  @Override
  public void close() {
    if (state == SpanState.IN_PROGRESS) {
      state = SpanState.OK;
    }
    endUnixNanos = TimeUtil.currentEpochNanos();
    Tracer.closeSpan(this);
  }

  // ====================================================================================================
  //  Name
  // ====================================================================================================
  @Override
  public String name() {
    return name;
  }

  @Override
  public Span setName(final String name) {
    this.name = name;
    return this;
  }

  // ====================================================================================================
  //  State
  // ====================================================================================================
  @Override
  public SpanState state() {
    return state;
  }

  @Override
  public Throwable exception() {
    return exception;
  }

  @Override
  public void setState(final SpanState state) {
    this.state = state;
  }

  @Override
  public void setFailureState(final Throwable e, final SpanState state) {
    this.state = state;
    this.exception = e;
  }

  // ====================================================================================================
  //  Attributes
  // ====================================================================================================
  @Override
  public boolean hasAttributes() {
    return !attributes.isEmpty();
  }

  @Override
  public Map<String, Object> attributes() {
    return attributes;
  }

  // ====================================================================================================
  //  Events
  // ====================================================================================================
  @Override
  public boolean hasEvents() {
    return !events.isEmpty();
  }

  @Override
  public List<SpanEvent> events() {
    return events;
  }

  @Override
  public SpanEvent addEvent(final String eventName) {
    final SpanEvent event = new BasicSpanEvent(eventName);
    events.add(event);
    return event;
  }

  @Override
  public String toString() {
    return "BasicSpan [traceId:" + traceId() + ", spanId:" + spanId + ", name:" + name + ", state:" + state + "]";
  }
}
