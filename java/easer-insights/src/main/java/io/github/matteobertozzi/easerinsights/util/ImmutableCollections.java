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

package io.github.matteobertozzi.easerinsights.util;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class ImmutableCollections {
  private ImmutableCollections() {
    // no-op
  }

  public static <T> List<T> listOf(final T[] items) {
    return new ImmutableList<>(items);
  }

  public static <T> Set<T> setOf(final T[] items) {
    return new ImmutableSet<>(items);
  }

  public static <K, V> Map<K, V> mapOf(final K[] keys, final V[] values) {
    return new ImmutableMap<>(new MapEntryWithKeyValArrays<>(keys, values));
  }

  private static final class ImmutableList<T> extends AbstractList<T> {
    private final T[] items;

    public ImmutableList(final T[] items) {
      this.items = items;
    }

    @Override
    public int size() {
      return items.length;
    }

    @Override
    public T get(final int index) {
      return items[index];
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class ImmutableSet<T> extends AbstractSet<T> {
    private final T[] items;

    public ImmutableSet(final T[] items) {
      this.items = items;
    }

    @Override
    public int size() {
      return items.length;
    }

    @Override
    public Iterator<T> iterator() {
      return new ArrayIterator<>(items);
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class ArrayIterator<T> implements Iterator<T> {
    private final T[] items;
    private int index;

    public ArrayIterator(final T[] items) {
      this.items = items;
      this.index = 0;
    }

    @Override
    public boolean hasNext() {
      return index < items.length;
    }

    @Override
    public T next() {
      return items[index++];
    }
  }

  private static final class ImmutableMap<K, V> extends AbstractMap<K, V> {
    private final Set<Entry<K, V>> entries;

    public ImmutableMap(final Set<Entry<K, V>> entries) {
      this.entries = entries;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return entries;
    }
  }

  private static final class MapEntryWithKeyValArrays<K, V> extends AbstractSet<Map.Entry<K, V>> {
    private final K[] keys;
    private final V[] vals;

    public MapEntryWithKeyValArrays(final K[] keys, final V[] vals) {
      this.keys = keys;
      this.vals = vals;
    }

    @Override
    public int size() {
      return keys.length;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new IteratorEntryWithKeyValArrays<>(keys, vals);
    }

    @Override
    public boolean addAll(final Collection<? extends Entry<K, V>> c) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class IteratorEntryWithKeyValArrays<K, V> implements Iterator<Entry<K, V>> {
    private final K[] keys;
    private final V[] vals;
    private int index;

    public IteratorEntryWithKeyValArrays(final K[] keys, final V[] vals) {
      this.keys = keys;
      this.vals = vals;
      this.index = 0;
    }

    @Override
    public boolean hasNext() {
      return index < keys.length;
    }

    @Override
    public Entry<K, V> next() {
      final Entry<K, V> entry = Map.entry(keys[index], vals[index]);
      index++;
      return entry;
    }
  }
}
