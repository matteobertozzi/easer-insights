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

import java.util.List;
import java.util.Map;

public record NullSpan(TraceId traceId, SpanId spanId) implements RootSpan {
  @Override
  public RootSpan rootSpan() {
    return this;
  }

  @Override
  public SpanId parentId() {
    return null;
  }

  @Override
  public long startUnixNanos() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long endUnixNanos() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long elapsedNanos() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException();
  }

  // ====================================================================================================
  //  Name
  // ====================================================================================================
  @Override
  public String name() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Span setName(final String name) {
    throw new UnsupportedOperationException();
  }

  // ====================================================================================================
  //  State
  // ====================================================================================================
  @Override
  public SpanState state() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Throwable exception() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setState(final SpanState state) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFailureState(final Throwable e, final SpanState state) {
    throw new UnsupportedOperationException();
  }

  // ====================================================================================================
  //  Attributes
  // ====================================================================================================
  @Override
  public boolean hasAttributes() {
    return false;
  }

  @Override
  public Map<String, Object> attributes() {
    return Map.of();
  }

  // ====================================================================================================
  //  Events
  // ====================================================================================================
  @Override
  public boolean hasEvents() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<SpanEvent> events() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SpanEvent addEvent(final String eventName) {
    throw new UnsupportedOperationException();
  }

  // ====================================================================================================
  //  RootSpan context
  // ====================================================================================================
  @Override
  public String tenantId() {
    return "";
  }

  @Override
  public String tenant() {
    return "";
  }

  @Override
  public String module() {
    return "";
  }

  @Override
  public String ownerId() {
    return "";
  }

  @Override
  public String owner() {
    return "";
  }
}
