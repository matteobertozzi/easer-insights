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

public final class ArrayUtil {
  private ArrayUtil() {
    // no-op
  }

  public static void copy(final long[] dstArray, final int dstIndex, final long[] srcArray, final int srcFromIndex, final int srcToIndex) {
    for (int i = 0, len = srcToIndex - srcFromIndex; i < len; ++i) {
      dstArray[dstIndex + i] = srcArray[srcFromIndex + i];
    }
  }

  public static void copyStride(final long[] dstArray, final int dstIndex, final long[] srcArray, final int srcIndex, final int length, final int stride) {
    int index = 0;
    for (int i = 0; i < length; ++i) {
      dstArray[dstIndex + i] = srcArray[srcIndex + index];
      index += stride;
    }
  }

  // ====================================================================================================
  //  Swap Util
  // ====================================================================================================
  public static void swap(final long[] values, final int aIndex, final int bIndex) {
    final long tmp = values[aIndex];
    values[aIndex] = values[bIndex];
    values[bIndex] = tmp;
  }

  public static <T> void swap(final T[] values, final int aIndex, final int bIndex) {
    final T tmp = values[aIndex];
    values[aIndex] = values[bIndex];
    values[bIndex] = tmp;
  }

  // ====================================================================================================
  //  Sort Util
  // ====================================================================================================
  public interface ArrayIndexComparator {
    int compare(int aIndex, int bIndex);
  }

  public interface ArrayIndexSwapper {
    void swap(int aIndex, int bIndex);
  }

  public static void sort(final int off, final int len,
      final ArrayIndexComparator comparator, final ArrayIndexSwapper swapper) {
    int i = len / 2 - 1;

    // heapify
    for (; i >= 0; --i) {
      int c = i * 2 + 1;
      int r = i;
      while (c < len) {
        if (c < len - 1 && comparator.compare(off + c, off + c + 1) < 0) {
          c += 1;
        }
        if (comparator.compare(off + r, off + c) >= 0) {
          break;
        }
        swapper.swap(off + r, off + c);
        r = c;
        c = r * 2 + 1;
      }
    }

    // sort
    for (i = len - 1; i > 0; --i) {
      int c = 1;
      int r = 0;
      swapper.swap(off, off + i);
      while (c < i) {
        if (c < i - 1 && comparator.compare(off + c, off + c + 1) < 0) {
          c += 1;
        }
        if (comparator.compare(off + r, off + c) >= 0) {
          break;
        }
        swapper.swap(off + r, off + c);
        r = c;
        c = r * 2 + 1;
      }
    }
  }
}
