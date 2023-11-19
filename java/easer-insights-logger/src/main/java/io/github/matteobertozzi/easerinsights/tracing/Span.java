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

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface Span extends Closeable {
  enum SpanState { IN_PROGRESS, OK, SYSTEM_FAILURE, USER_FAILURE }

  @Override void close();

  RootSpan rootSpan();
  TraceId traceId();
  SpanId parentId();
  SpanId spanId();

  long startUnixNanos();
  long endUnixNanos();
  long elapsedNanos();

  String name();
  Span setName(String name);

  SpanState state();
  Throwable exception();
  void setState(SpanState state);
  void setFailureState(Throwable e, SpanState state);

  boolean hasAttributes();
  Map<String, Object> attributes();

  boolean hasEvents();
  List<SpanEvent> events();
  SpanEvent addEvent(final String eventName);
}
