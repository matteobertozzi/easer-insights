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

import java.util.Map;

public final class TraceAttributes {
  private TraceAttributes() {
    // no-op
  }

  public static final TraceStringAttribute TENANT_ID = new TraceStringAttribute("tenant.id");
  public static final TraceStringAttribute TENANT_NAME = new TraceStringAttribute("tenant.name");

  public static final TraceStringAttribute OWNER_ID = new TraceStringAttribute("owner.id");
  public static final TraceStringAttribute OWNER_NAME = new TraceStringAttribute("owner.name");
  public static final TraceStringAttribute SESSION_ID = new TraceStringAttribute("session.id");

  public static final TraceStringAttribute THREAD_NAME = new TraceStringAttribute("thread.name");
  public static final TraceStringAttribute MODULE_NAME = new TraceStringAttribute("module.name");
  public static final TraceStringAttribute CODE_METHOD_CALLER = new TraceStringAttribute("code.method.caller");

  public static final TraceLongAttribute TASK_QUEUE_TIME_NS = new TraceLongAttribute("task.queue.time.ns");

  public static final TraceStringAttribute HTTP_REQUEST_METHOD = new TraceStringAttribute("http.request.method");
  public static final TraceStringAttribute HTTP_RESPONSE_METHOD = new TraceStringAttribute("http.response.status_code");
  public static final TraceStringAttribute HTTP_ROUTE = new TraceStringAttribute("http.route");
  public static final TraceStringAttribute URL_PATH = new TraceStringAttribute("url.path");

  public record TraceStringAttribute(String name) {
    public String get(final Span span, final String defaultValue) {
      return get(span.attributes(), defaultValue);
    }

    public String get(final Map<String, Object> attrs, final String defaultValue) {
      final String value = (String) attrs.get(name);
      return value != null ? value : defaultValue;
    }

    public void set(final Span span, final String value) {
      span.attributes().put(name, value);
    }

    public void setIfAbsent(final Span span, final String value) {
      span.attributes().putIfAbsent(name, value);
    }
  }

  public record TraceLongAttribute(String name) {
    public long get(final Span span, final long defaultValue) {
      return get(span.attributes(), defaultValue);
    }

    public long get(final Map<String, Object> attrs, final long defaultValue) {
      final Long value = (Long) attrs.get(name);
      return value != null ? value : defaultValue;
    }

    public void set(final Span span, final long value) {
      span.attributes().put(name, value);
    }

    public void setIfAbsent(final Span span, final long value) {
      span.attributes().putIfAbsent(name, value);
    }
  }
}
