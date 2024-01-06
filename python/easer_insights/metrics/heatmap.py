# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from dataclasses import dataclass
from math import ceil

from .time_range import TimeRange
from ..util.time_unit import TimeUnit
from ..util.time_util import current_epoch_millis


@dataclass
class HeatmapSnapshot:
    last_interval: int
    window: int
    bounds: list[int]
    events: list[int]
    min_value: list[int]
    sum: list[int]
    sum_squares: list[int]


class Heatmap:
    def __init__(self, max_interval: int, window: int, unit: TimeUnit, bounds: list[int]) -> None:
        self.bounds = bounds
        stride = len(bounds) + 5
        self._time_range = TimeRange(unit.to_millis(window), 0)
        self._total_slots = ceil(unit.to_millis(max_interval) / self._time_range.window)
        self._ring = [0] * (self._total_slots * stride)  # |min|max|sum|sumSquares|events...

    def sample(self, timestamp: int, value: int):
        self._time_range.update(timestamp, self._total_slots, self._reset_slots,
                                lambda idx: self._update_slot(idx, value))

    def _update_slot(self, index: int, value: int):
        bound_index = self._find_bound_index(value)
        ring_index = index * (len(self.bounds) + 5)
        ring = self._ring
        ring[ring_index] = min(ring[ring_index], value)
        ring[ring_index + 1] = max(ring[ring_index + 1], value)
        ring[ring_index + 2] += value
        ring[ring_index + 3] += value * value
        ring[ring_index + 4 + bound_index] += 1

    def _reset_slots(self, from_index: int, to_index: int):
        reset_length = to_index - from_index
        stride = len(self.bounds) + 5
        ring_from_index = from_index * stride
        ring_to_index = to_index * stride
        ring = self._ring
        for i in range(ring_from_index, ring_to_index):
            ring[i] = 0
        for i in range(reset_length):
            ring[(from_index + i) * stride] = 0xffffffffffffffff

    def _find_bound_index(self, value: int) -> int:
        index = 0
        bounds = self.bounds
        num_bounds = len(bounds)
        while index < num_bounds:
            if value < bounds[index]:
                return index
            index += 1
        return index

    def snapshot(self) -> HeatmapSnapshot:
        stride = len(self.bounds) + 5
        now = current_epoch_millis()
        self._time_range.update(now, self._total_slots, self._reset_slots)

        ring = self._ring
        last_bound = 0
        for i in range(0, len(ring), stride):
            last_bound = max(last_bound, ring[i + 1])
        last_bound_index = self._find_bound_index(last_bound) + 1
        snapshot_bounds = self.bounds[:last_bound_index]
        if len(snapshot_bounds) < last_bound_index:
            snapshot_bounds.append(last_bound)
        else:
            snapshot_bounds[last_bound_index - 1] = last_bound

        events = []
        min_value = []
        max_value = []
        sum_squares = []
        sum = []
        length = self._time_range.size(self._total_slots)

        def _copy_slots(_, src_from_idx, src_to_idx):
            copy_length = src_to_idx - src_from_idx
            for i in range(copy_length):
                ring_index = (src_from_idx + i) * stride
                min_value.append(0 if ring[ring_index] == 0xffffffffffffffff else ring[ring_index])
                max_value.append(ring[ring_index + 1])
                sum.append(ring[ring_index + 2])
                sum_squares.append(ring[ring_index + 3])
                events.extend(ring[ring_index + 4:ring_index + 4 + last_bound_index])

        self._time_range.copy(self._total_slots, length, _copy_slots)
        return HeatmapSnapshot(self._time_range.last_interval, self._time_range.window, snapshot_bounds, events,
                               min_value, sum, sum_squares)
