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


@dataclass
class HistogramSnapshot:
    bounds: list[int]
    events: list[int]
    num_events: int
    min_value: int
    sum: int
    sum_squares: int


class Histogram:
    def __init__(self, bounds: list[int]):
        self.bounds = bounds
        self._events = [0] * (1 + len(bounds))
        self._min_value = 2 ** 64
        self._max_value = -(2 ** 64)
        self._sum_squares = 0
        self._sum = 0

    def sample(self, value: int):
        bound_index = self._find_bound_index(value)
        self._events[bound_index] += 1
        self._min_value = min(self._min_value, value)
        self._max_value = max(self._max_value, value)
        self._sum_squares += (value * value)
        self._sum += value

    def _find_bound_index(self, value: int) -> int:
        index = 0
        bounds = self.bounds
        num_bounds = len(bounds)
        while index < num_bounds:
            if value < bounds[index]:
                return index
            index += 1
        return index

    def snapshot(self) -> HistogramSnapshot:
        events = self._events
        num_events = sum(events)
        if num_events == 0:
            return HistogramSnapshot([], [], 0, 0, 0, 0)

        first_bound = 0
        last_bound = len(events) - 1

        while events[first_bound] == 0: first_bound += 1
        while events[last_bound] == 0: last_bound -= 1

        snapshot_bounds = self.bounds[first_bound:last_bound]
        snapshot_events = self._events[first_bound:last_bound]
        snapshot_bounds.append(self._max_value)
        snapshot_events.append(events[last_bound])

        return HistogramSnapshot(snapshot_bounds, snapshot_events, num_events, self._min_value, self._sum,
                                 self._sum_squares)
