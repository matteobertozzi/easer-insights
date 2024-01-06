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
class TimeRangeCounterSnapshot:
    last_interval: int
    window: int
    data: list[int]


class TimeRangeCounter:
    def __init__(self, max_interval: int, window: int, unit: TimeUnit) -> None:
        self._time_range = TimeRange(unit.to_millis(window), 0)
        trc_count = ceil(unit.to_millis(max_interval) / self._time_range.window)
        self._counters = [0] * trc_count

    def add(self, timestamp: int, delta: int):
        self._time_range.update(timestamp, len(self._counters), self._reset_slots,
                                lambda idx: self._update_slot(idx, delta))

    def set(self, timestamp: int, value: int):
        self._time_range.update(timestamp, len(self._counters), self._reset_slots,
                                lambda idx: self._assign_slot(idx, value))

    def _update_slot(self, index: int, delta: int):
        print(index)
        self._counters[index] += delta

    def _assign_slot(self, index: int, value: int):
        self._counters[index] = value

    def _reset_slots(self, from_index: int, to_index: int):
        for i in range(to_index - from_index):
            self._counters[from_index + i] = 0

    def snapshot(self) -> TimeRangeCounterSnapshot:
        now = current_epoch_millis()
        counters_length = len(self._counters)
        self._time_range.update(now, counters_length, self._reset_slots)

        data_size = self._time_range.size(counters_length)
        data = []
        self._time_range.copy(counters_length, data_size,
                              lambda _, src_from_idx, src_to_idx: data.extend(self._counters[src_from_idx:src_to_idx]))
        return TimeRangeCounterSnapshot(self._time_range.last_interval, self._time_range.window, data)
