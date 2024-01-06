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
class MaxAvgTimeRangeGaugeSnapshot:
    last_interval: int
    window: int
    count: list[int]
    sum: list[int]
    max: list[int]


class MaxAvgTimeRangeGauge:
    def __init__(self, max_interval: int, window: int, unit: TimeUnit) -> None:
        self._time_range = TimeRange(unit.to_millis(window), 0)
        trc_count = ceil(unit.to_millis(max_interval) / self._time_range.window)
        self._ring = [0] * (trc_count * 3)  # count|sum|max

    def sample(self, timestamp: int, value: int):
        ring_length = len(self._ring) // 3
        self._time_range.update(timestamp, ring_length, self._reset_slots, lambda idx: self._update_slot(idx, value))

    def _update_slot(self, index: int, value: int):
        ring_index = index * 3
        self._ring[ring_index] += 1
        self._ring[ring_index + 1] += value
        self._ring[ring_index + 2] = max(self._ring[ring_index + 2], value)

    def _reset_slots(self, from_index: int, to_index: int):
        ring_from_index = from_index * 3
        ring_to_index = to_index * 3
        for i in range(ring_to_index - ring_from_index):
            self._ring[ring_from_index + i] = 0

    def snapshot(self) -> MaxAvgTimeRangeGaugeSnapshot:
        now = current_epoch_millis()
        ring_length = len(self._ring) // 3
        self._time_range.update(now, ring_length, self._reset_slots)

        length = self._time_range.size(ring_length)
        count = []
        sum = []
        max = []

        def _copy_slots(_, src_from_idx, src_to_idx):
            copy_length = src_to_idx - src_from_idx
            ring_index = src_from_idx * 3
            index = 0
            for i in range(copy_length):
                count.append(self._ring[ring_index + index])
                sum.append(self._ring[ring_index + index + 1])
                max.append(self._ring[ring_index + index + 2])
                index += 3

        self._time_range.copy(ring_length, length, _copy_slots)
        return MaxAvgTimeRangeGaugeSnapshot(self._time_range.last_interval, self._time_range.window, count, sum, max)
