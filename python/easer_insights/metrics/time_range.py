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

from ..util.time_util import align_to_window


class TimeRange:
    def __init__(self, window: int, timestamp: int) -> None:
        self.window = window
        self.last_interval = timestamp
        self._next = 0

    def size(self, slots_count: int) -> int:
        return min(self._next + 1, slots_count)

    def copy(self, slots_count: int, length: int, copy_func):
        eof_index = 1 + (self._next % slots_count)
        if self._next >= length:
            # 5, 6, 7, 8, 1, 2, 3, 4
            copy_func(0, eof_index, slots_count)
            copy_func(slots_count - eof_index, 0, eof_index)
        else:
            # 1, 2, 3, 4
            copy_func(0, 0, eof_index)

    def iter_reverse(self, slots_count: int, consumer_func):
        eof_index = self._next % slots_count
        if self._next >= slots_count:
            index = eof_index
            while index >= 0:
                consumer_func(index)
                index -= 1
            index = slots_count - 1
            while index > eof_index:
                consumer_func(index)
                index -= 1
        else:
            index = eof_index
            while index >= 0:
                consumer_func(index)
                index -= 1

    def update(self, timestamp: int, total_slots: int, reset_func, update_func=lambda idx: None):
        align_ts = align_to_window(timestamp, self.window)
        delta_time = align_ts - self.last_interval

        if delta_time == 0:
            update_func(self._next % total_slots)
            return

        if delta_time == self.window:
            self.last_interval = align_ts
            self._next += 1
            index = self._next % total_slots
            reset_func(index, index + 1)
            update_func(index)
            return

        # Inject Slots
        if delta_time > 0:
            self._inject_slots(delta_time, total_slots, reset_func)
            self.last_interval = align_ts
            update_func(self._next % total_slots)
            return

        # Update past slot
        avail_slots = min(self._next + 1, total_slots)
        past_index = -delta_time // self.window
        if past_index >= avail_slots:
            # ignore, too far in the past
            return

        update_func((self._next - past_index) % total_slots)

    def _inject_slots(self, delta_time: int, total_slots: int, reset_func):
        slots = delta_time // self.window
        if slots >= total_slots:
            reset_func(0, total_slots)
            self._next += slots
            return

        from_index = (self._next + 1) % total_slots
        to_index = (self._next + 1 + slots) % total_slots
        if from_index < to_index:
            reset_func(from_index, to_index)
        else:
            reset_func(from_index, total_slots)
            reset_func(0, to_index)
        self._next += slots
