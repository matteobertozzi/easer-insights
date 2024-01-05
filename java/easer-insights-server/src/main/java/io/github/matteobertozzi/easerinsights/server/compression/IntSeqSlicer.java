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

package io.github.matteobertozzi.easerinsights.server.compression;

import io.github.matteobertozzi.easerinsights.server.compression.IntSeqCoding.IntSeqEncoder;
import io.github.matteobertozzi.easerinsights.server.compression.IntSeqCoding.IntSeqSliceType;
import io.github.matteobertozzi.rednaco.collections.arrays.ArrayUtil;

public final class IntSeqSlicer {
  private IntSeqSlicer() {
    // no-op
  }

  public static void slice(final IntSeqEncoder encoder, final long[] seq) {
    if (ArrayUtil.isEmpty(seq)) return;

    final int maxLength = encoder.maxSliceLength();
    IntSeqSliceType sliceType = IntSeqSliceType.MIN;
    final long[] minmax = new long[2];
    int sliceStart = 0;
    int sliceEnd = 1;
    int sliceLength = 1;
    long sliceValue = seq[0];
    long sliceDelta = 0;

    int rleCount = 1;
    int linCount = 2;
    int minSavepointIdx = sliceStart;
    long minSavepointValue = sliceValue;
    long minSavepointDelta = sliceDelta;
    long minLookaheadValue = sliceValue;
    long minLookaheadDelta = sliceDelta;

    // Lorenzo Moreschini (https://github.com/lmores)
    // The general idea is the following:
    // - start reading values one by one and add them to the current slice
    // - use 'rle_count' and 'lin_count' to keep track of how many constant
    // or linearly spaced consecutive values we find and as soon as it is
    // advantageous emit a slice of type RLE or MIN
    // - otherwise we are left with the problem to detect when it is convenient
    // to stop the current MIN slice to open another MIN slice: to detect such
    // cases we use the variable 'min_savepoint_idx' to mark an item when we
    // are sure than all values between 'slice_start' and 'min_savepoint_idx'
    // (inclusive) must belong a single slice of type MIN (i.e. when we are
    // sure that it is not conveninent to further divide that slice).
    //
    // |---- slice_start
    // | |---- min_savepoint_idx
    // | ||---- min_savepoint_idx + 1
    // | || |---- slice_end (= i at the end of each loop)
    // v VV V
    // [ *** past values ***, . ...... .. ..... ., *** future values *** ]
    //
    // 'min_savepoint_value' and 'min_savepoint_delta' keep track of the
    // base value and delta of the interval [slice_start, min_savepoint_idx]
    // 'min_lookahead_value' and 'min_lookahead_delta' keep track of the
    // base value and delta of the interval [min_savepoint_idx+1, slice_end]
    for (int i = 1; i < seq.length; ) {
      final long val = seq[i];
      if (val < 0) {
        throw new IllegalArgumentException("Value at index " + i + " is " + val + " < 0");
      }

      // If current slice length exceeds MAX_LENGTH (specified by the protocol),
      // then forcibly close the current slice and start a new one
      if (sliceLength == maxLength) {
        if (sliceType == IntSeqSliceType.MIN && minSavepointIdx > sliceStart) {
          encoder.add(IntSeqSliceType.MIN, seq, sliceStart, minSavepointIdx + 1, minSavepointValue, minSavepointDelta);

          // slice_type = IntSeqSliceType.MIN; // useless
          sliceStart = minSavepointIdx + 1;
          sliceLength = sliceEnd - sliceStart;
          sliceValue = minLookaheadValue;
          sliceDelta = minLookaheadDelta;
        } else {
          encoder.add(sliceType, seq, sliceStart, sliceEnd, sliceValue, sliceDelta);

          sliceType = IntSeqSliceType.MIN;
          sliceStart = sliceEnd;
          sliceEnd += 1;
          sliceLength = 1;
          sliceValue = val;
          sliceDelta = 0;
          rleCount = 1; // reset
          linCount = 2; // reset
          i = sliceEnd;
        }

        // Reset
        minSavepointIdx = sliceStart;
        minSavepointValue = sliceValue;
        minSavepointDelta = sliceDelta;
        minLookaheadValue = sliceValue;
        minLookaheadDelta = sliceDelta;
        continue;
      }

      // [For testing purpose only] no slice should ever be longer than MAX_LENGTH
      if (sliceLength > maxLength) {
        throw new IllegalArgumentException(
            "slice length should never be greater than MAX_LENGTH, got " + sliceLength + " > " + maxLength);
      }

      // Continue or close current slice according to its type and the incoming 'val'
      if (sliceType == IntSeqSliceType.MIN) {
        // While encoding a slice of type MIN we must detect incoming
        // subsequences that can be encoded using RLE or LIN and
        // switch to those encodings as soon as it is advantageous

        // Track of how many *consecutive* constant values
        // are there in the tail of current slice
        if (val == seq[i - 1]) {
          rleCount += 1;
          linCount = 2; // reset
        } else {
          rleCount = 1; // reset
          // Track of how many *consecutive* (non-constant) linearly
          // spaced values are there in the tail of current slice
          if (sliceLength > 1 && val - seq[i - 1] == seq[i - 1] - seq[i - 2]) {
            linCount += 1;
          } else {
            linCount = 2;
          }
        }

        sliceEnd += 1;
        sliceLength += 1;

        final long prev_slice_delta = sliceDelta;
        if (val > sliceValue + sliceDelta) {
          sliceDelta = val - sliceValue;
        } else if (val < sliceValue) {
          sliceDelta = sliceValue + sliceDelta - val;
          sliceValue = val;
        }

        if (i - minSavepointIdx > 1) {
          if (val > minLookaheadValue + minLookaheadDelta) {
            minLookaheadDelta = val - minLookaheadValue;
          } else if (val < minLookaheadValue) {
            minLookaheadDelta = minLookaheadValue + minLookaheadDelta - val;
            minLookaheadValue = val;
          }
        } else {
          minLookaheadValue = val;
          minLookaheadDelta = 0;
        }

        if (rleCount > 1) {
          final int _slice_length = sliceEnd - sliceStart - rleCount;
          final long diff = (encoder.minBits(sliceLength, sliceValue, sliceDelta) -
                  encoder.sliceBits(_slice_length, sliceValue, sliceDelta) -
                  encoder.rleBits(rleCount, val));
          if (diff > 0) {
            if (_slice_length > 0) {
              if (_slice_length == 1) {
                encoder.add(IntSeqSliceType.RLE, seq, sliceStart, sliceStart + 1, seq[sliceStart], 0);
              } else if (_slice_length == 2) {
                encoder.add(IntSeqSliceType.LIN, seq, sliceStart, sliceStart + 2, seq[sliceStart], seq[sliceStart+1] - seq[sliceStart]);
              } else {
                final int _slice_end = sliceStart + _slice_length;
                minMax(minmax, seq, sliceStart, _slice_end);
                final long _slice_value = minmax[0];
                final long _slice_delta = minmax[1] - _slice_value;
                encoder.add(sliceType, seq, sliceStart, _slice_end, _slice_value, _slice_delta);
              }
              sliceStart += _slice_length;
            }
            sliceType = IntSeqSliceType.RLE;
            sliceValue = val;
            sliceDelta = 0;
            i = sliceEnd;
            continue;
          }

        } else if (linCount > 2) {
          final long lin_delta = val - seq[i - 1];
          final long lin_value = val - lin_delta * (linCount - 1);
          final int _slice_length = sliceEnd - sliceStart - linCount;
          final long diff = (encoder.minBits(sliceLength, sliceValue, sliceDelta) -
                  encoder.sliceBits(_slice_length, sliceValue, sliceDelta) -
                  encoder.linBits(linCount, lin_value, lin_delta));
          if (diff > 0) {
            if (_slice_length > 0) {
              if (_slice_length == 1) {
                encoder.add(IntSeqSliceType.RLE, seq, sliceStart, sliceStart + 1, seq[sliceStart], 0);
              } else if (_slice_length == 2) {
                encoder.add(IntSeqSliceType.LIN, seq, sliceStart, sliceStart + 2, seq[sliceStart], seq[sliceStart+1] - seq[sliceStart]);
              } else {
                final int _slice_end = sliceStart + _slice_length;
                minMax(minmax, seq, sliceStart, _slice_end);
                final long _slice_value = minmax[0];
                final long _slice_delta = minmax[1] - _slice_value;
                encoder.add(sliceType, seq, sliceStart, _slice_end, _slice_value, _slice_delta);
              }
              sliceStart += _slice_length;
            }
            sliceType = IntSeqSliceType.LIN;
            sliceValue = lin_value;
            sliceDelta = lin_delta;
            i = sliceEnd;
            continue;
          }
        }

        if (i - minSavepointIdx > 2) {
          final int min_slice_length = minSavepointIdx + 1 - sliceStart;
          final long diff = (encoder.minBits(sliceLength, sliceValue, sliceDelta) -
                  encoder.sliceBits(min_slice_length, minSavepointValue, minSavepointDelta) -
                  encoder.minBits(i - minSavepointIdx, minLookaheadValue, minLookaheadDelta));

          if (diff > 0) {
            if (min_slice_length > 0) {
              encoder.add(IntSeqSliceType.MIN, seq, sliceStart, sliceStart + min_slice_length, minSavepointValue, minSavepointDelta);
              sliceStart += min_slice_length;
              // Reset
              minSavepointIdx = sliceStart;
              minSavepointValue = minLookaheadDelta;
              minSavepointDelta = minLookaheadDelta;
            } else {
              throw new IllegalStateException("should not be here");
            }

            sliceType = IntSeqSliceType.MIN; // already MIN
            sliceLength = sliceEnd - sliceStart;
            sliceValue = minLookaheadValue;
            sliceDelta = minLookaheadDelta;
            i = sliceEnd;
            continue;
          }
        }

        if (IntSeqCoding.valueBits(prev_slice_delta) <= IntSeqCoding.valueBits(val - sliceValue)) {
          minSavepointIdx = i;
          minSavepointValue = sliceValue;
          minSavepointDelta = sliceDelta;
          minLookaheadValue = val;
          minLookaheadDelta = 0;
        }

        i = sliceEnd;

      } else if (sliceType == IntSeqSliceType.RLE) {
        if (val == sliceValue) {
          // extend the current slice
          sliceEnd += 1;
          sliceLength += 1;
        } else {
          // Close the current slice and start a new one
          encoder.add(sliceType, seq, sliceStart, i, sliceValue, sliceDelta);
          sliceType = IntSeqSliceType.MIN;
          sliceStart = i;
          sliceEnd += 1; // = slice_start + 1
          sliceLength = 1;
          sliceValue = val;
          sliceDelta = 0;

          // Reset
          rleCount = 1;
          minSavepointIdx = sliceStart;
          minSavepointValue = sliceValue;
          minSavepointDelta = sliceDelta;
          minLookaheadValue = sliceValue;
          minLookaheadDelta = sliceDelta;

          if (sliceEnd != (sliceStart + 1)) throw new IllegalStateException("got " + sliceEnd + " != " + (sliceStart + 1));
          if ((i + 1) != sliceEnd) throw new IllegalStateException("got " + (i + 1) + " != " + sliceEnd);
        }
        i = sliceEnd;

      } else if (sliceType == IntSeqSliceType.LIN) {
        if (val - seq[i - 1] == sliceDelta) {
          // extend the current slice
          sliceEnd += 1;
          sliceLength += 1;
        } else {
          // Close the current slide and start a new one
          encoder.add(sliceType, seq, sliceStart, i, sliceValue, sliceDelta);
          sliceType = IntSeqSliceType.MIN;
          sliceStart = i;
          sliceEnd += 1; // = slice_start + 1
          sliceLength = 1;
          sliceValue = val;
          sliceDelta = 0;

          // Reset
          linCount = 2;
          minSavepointIdx = sliceStart;
          minSavepointValue = sliceValue;
          minSavepointDelta = sliceDelta;
          minLookaheadValue = sliceValue;
          minLookaheadDelta = sliceDelta;

          if (sliceEnd != (sliceStart + 1)) throw new IllegalStateException("got " + sliceEnd + " != " + (sliceStart + 1));
          if ((i + 1) != sliceEnd) throw new IllegalStateException("got " + (i + 1) + " != " + sliceEnd);
        }
        i = sliceEnd;
      } else {
        throw new IllegalStateException("unexpected slice type: " + sliceType);
      }
    }

    if (sliceEnd != seq.length) {
      throw new IllegalStateException("Got " + sliceLength + " != " + seq.length);
    }

    if (sliceLength == 1) {
      encoder.add(IntSeqSliceType.RLE, seq, sliceStart, sliceEnd, sliceValue, sliceDelta);
    } else if (sliceLength == 2) {
      encoder.add(IntSeqSliceType.LIN, seq, sliceStart, sliceEnd, seq[seq.length-2], seq[seq.length-1] - seq[seq.length-2]);
    } else {
      encoder.add(sliceType, seq, sliceStart, sliceEnd, sliceValue, sliceDelta);
    }
  }

  private static void minMax(final long[] minMax, final long[] items, final int startIndex, final int endIndex) {
    final int length = endIndex - startIndex;

    if (length == 1) {
      minMax[0] = items[startIndex];
      minMax[1] = items[startIndex];
      return;
    }

    if (items[startIndex] < items[startIndex + 1]) {
      minMax[0] = items[startIndex];
      minMax[1] = items[startIndex + 1];
    } else {
      minMax[1] = items[startIndex];
      minMax[0] = items[startIndex + 1];
    }

    for (int i = 2, n = length - 2; i <= n; i += 2) {
      final int index = startIndex + i;
      if (items[index] < items[index + 1]) {
        minMax[0] = Math.min(minMax[0], items[index]);
        minMax[1] = Math.max(minMax[1], items[index + 1]);
      } else {
        minMax[1] = Math.max(minMax[1], items[index]);
        minMax[0] = Math.min(minMax[0], items[index + 1]);
      }
    }

    if ((length & 1) == 1) {
      minMax[0] = Math.min(minMax[0], items[endIndex - 1]);
      minMax[1] = Math.max(minMax[1], items[endIndex - 1]);
    }
  }
}
