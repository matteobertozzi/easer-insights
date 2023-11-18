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

import io.github.matteobertozzi.easerinsights.tracing.RootSpan;
import io.github.matteobertozzi.easerinsights.tracing.Span;
import io.github.matteobertozzi.easerinsights.tracing.SpanId;
import io.github.matteobertozzi.easerinsights.tracing.TraceId;
import io.github.matteobertozzi.easerinsights.tracing.TraceProvider;

public final class BasicTracer implements TraceProvider {
  public static final BasicTracer INSTANCE = new BasicTracer();

  private BasicTracer() {
    // no-op
  }

  @Override
  public RootSpan openRootSpan(final TraceId traceId, final SpanId parentId, final SpanId spanId) {
    return new BasicRootSpan(traceId, parentId, spanId);
  }

  @Override
  public Span openSpan(final RootSpan rootSpan, final SpanId parentId, final SpanId spanId) {
    return new BasicSpan(rootSpan, parentId, spanId);
  }

  @Override
  public void closeSpan(final Span span) {
    // no-op
  }
}
