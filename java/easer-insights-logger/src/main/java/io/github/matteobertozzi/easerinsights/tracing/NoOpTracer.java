package io.github.matteobertozzi.easerinsights.tracing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NoOpTracer implements TraceProvider {
  public static final RootSpan NULL_SPAN_INSTANCE = new NoOpNullSpan(NullTraceId.INSTANCE, NullSpanId.INSTANCE);
  public static final TraceIdProvider TRACE_ID_PROVIDER = new NullTraceIdProvider();
  public static final SpanIdProvider SPAN_ID_PROVIDER = new NullSpanIdProvider();
  public static final NoOpTracer INSTANCE = new NoOpTracer();

  private NoOpTracer() {
    // no-op
  }

  @Override
  public RootSpan openRootSpan(final TraceId traceId, final SpanId parentId, final SpanId spanId) {
    return NULL_SPAN_INSTANCE;
  }

  @Override
  public Span openSpan(final RootSpan rootSpan, final SpanId parentId, final SpanId spanId) {
    return NULL_SPAN_INSTANCE;
  }

  @Override
  public void closeSpan(final Span span) {
    // no-op
  }

  private static final class NullTraceIdProvider implements TraceIdProvider {
    @Override public TraceId nullTraceId() { return NullTraceId.INSTANCE; }
    @Override public TraceId newTraceId() { return NullTraceId.INSTANCE; }
    @Override public TraceId parseTraceId(final String traceId) { return NullTraceId.INSTANCE; }
    @Override public TraceId parseTraceId(final byte[] traceId) { return NullTraceId.INSTANCE; }
  }

  private static final class NullTraceId implements TraceId {
    private static final NullTraceId INSTANCE = new NullTraceId();
    private NullTraceId() {}
    @Override public String toString() { return ""; }
  }

  private static final class NullSpanIdProvider implements SpanIdProvider {
    @Override public SpanId nullSpanId() { return NullSpanId.INSTANCE; }
    @Override public SpanId newSpanId() { return NullSpanId.INSTANCE; }
    @Override public SpanId parseSpanId(final String spanId) { return NullSpanId.INSTANCE; }
    @Override public SpanId parseSpanId(final byte[] spanId) { return NullSpanId.INSTANCE; }
  }

  private static final class NullSpanId implements SpanId {
    private static final NullSpanId INSTANCE = new NullSpanId();
    private NullSpanId() {}
    @Override public String toString() { return ""; }
  }

  public record NoOpNullSpan(TraceId traceId, SpanId spanId) implements RootSpan {
    @Override public RootSpan rootSpan() { return this; }
    @Override public SpanId parentId() { return null; }
    @Override public long startUnixNanos() { return 0; }
    @Override public long endUnixNanos() { return 0; }
    @Override public long elapsedNanos() { return 0; }
    @Override public void close() {}

    // ====================================================================================================
    //  Name
    // ====================================================================================================
    @Override public String name() { return ""; }
    @Override public Span setName(final String name) { return this; }

    // ====================================================================================================
    //  State
    // ====================================================================================================
    @Override public SpanState state() { return SpanState.OK; }
    @Override public Throwable exception() { return null; }
    @Override public void setState(final SpanState state) { }
    @Override public void setFailureState(final Throwable e, final SpanState state) { }

    // ====================================================================================================
    //  Attributes
    // ====================================================================================================
    @Override public boolean hasAttributes() { return false; }
    @Override public Map<String, Object> attributes() { return DevNullMap.STRING_OBJECT_INSTANCE; }

    // ====================================================================================================
    //  Events
    // ====================================================================================================
    @Override public boolean hasEvents() { return false; }
    @Override public List<SpanEvent> events() { return List.of(); }
    @Override public SpanEvent addEvent(final String eventName) { return NullSpanEvent.INSTANCE; }

    // ====================================================================================================
    //  RootSpan context
    // ====================================================================================================
    @Override public String tenantId() { return ""; }
    @Override public String tenant() { return ""; }
    @Override public String module() { return ""; }
    @Override public String ownerId() { return ""; }
    @Override public String owner() { return ""; }
  }

  private static class NullSpanEvent implements SpanEvent {
    private static final NullSpanEvent INSTANCE = new NullSpanEvent();
    private NullSpanEvent() {}
    @Override public String eventName() { return ""; }
    @Override public long timeUnixNanos() { return 0; }
    @Override public boolean hasAttributes() { return false; }
    @Override public Map<String, Object> attributes() { return DevNullMap.STRING_OBJECT_INSTANCE; }

  }

  private static class DevNullMap<K, V> implements Map<K, V> {
    public static final DevNullMap<String, Object> STRING_OBJECT_INSTANCE = new DevNullMap<>();

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean containsKey(final Object key) {
      return false;
    }

    @Override
    public boolean containsValue(final Object value) {
      return false;
    }

    @Override
    public V get(final Object key) {
      return null;
    }

    @Override
    public V put(final K key, final V value) {
      return null;
    }

    @Override
    public V remove(final Object key) {
      return null;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
      // no-op
    }

    @Override
    public void clear() {
      // no-op
    }

    @Override
    public Set<K> keySet() {
      return Set.of();
    }

    @Override
    public Collection<V> values() {
      return List.of();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return Set.of();
    }
  }
}
