/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tlc2.overrides;

import tlc2.overrides.source.Partition;
import tlc2.overrides.source.Record;
import tlc2.overrides.source.Source;
import tlc2.overrides.source.Sources;
import tlc2.value.impl.IntValue;
import tlc2.value.impl.Value;

import java.io.IOException;

/**
 * Trace utilities.
 */
public class Traces {
    private static final String TRACE_SOURCE = "TRACE_SOURCE";
    private static final String TRACE_PARTITION = "TRACE_PARTITION";
    private static final String TRACE_START_TIMESTAMP = "TRACE_START_TIMESTAMP";
    private static final String TRACE_END_TIMESTAMP = "TRACE_END_TIMESTAMP";
    private static final Source SOURCE;
    private static final Partition PARTITION;

    private static final Long START_TIME;
    private static final Long END_TIME;

    private static Long upperBound;

    private static Source getSource() {
        String source = System.getenv(TRACE_SOURCE);
        if (source != null) {
            return Sources.getSource(source);
        }
        return null;
    }

    static {
        SOURCE = getSource();
        if (SOURCE != null) {
            int partitionId = Integer.valueOf(System.getenv(TRACE_PARTITION));
            PARTITION = SOURCE.getPartition(partitionId);
        } else {
            PARTITION = null;
        }
    }

    static {
        String startTime = System.getenv(TRACE_START_TIMESTAMP);
        if (startTime != null) {
            START_TIME = Long.parseLong(startTime);
        } else {
            START_TIME = null;
        }
        String endTime = System.getenv(TRACE_END_TIMESTAMP);
        if (endTime != null) {
            END_TIME = Long.parseLong(endTime);
        } else {
            END_TIME = null;
        }
    }

    @TLAPlusOperator(identifier = "LowerBound", module = "Traces")
    public static Value lowerBound() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        // If the start time is not set, return an infinite lower bound.
        if (START_TIME == null) {
            return IntValue.gen(0);
        }

        // Get the first offset following the configured start time.
        long startOffset = PARTITION.offset(START_TIME);
        if (startOffset == 0) {
            return IntValue.gen(0);
        }
        return IntValue.gen((int) startOffset - 1);
    }

    @TLAPlusOperator(identifier = "UpperBound", module = "Traces")
    public static Value upperBound() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        // If the end time is not set, return an infinite upper bound.
        if (END_TIME == null) {
            return IntValue.gen(Integer.MAX_VALUE);
        }

        // Get the upper bound.
        Long offset = upperBound;

        // If the upper bound is not set, get the upper bound from the partition.
        if (upperBound() == null) {
            long endOffset = PARTITION.offset(END_TIME);
            if (endOffset == 0) {
                upperBound = 1L;
            } else {
                upperBound = endOffset;
            }
            return IntValue.gen((int) endOffset);
        }

        // Get the next offset and return the offset if it exceeds the upper bound timestamp, otherwise
        // return the offset + 1.
        Record record = PARTITION.get(offset);
        if (record.timestamp() >= END_TIME) {
            return IntValue.gen((int) (long) offset);
        }
        return IntValue.gen((int) (long) offset + 1);
    }

    @TLAPlusOperator(identifier = "Trace", module = "Traces")
    public static Value trace(IntValue offset) throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        // Get the next record and set the upper bound
        upperBound = (long) offset.val + 1;
        Value value = PARTITION.get(offset.val).value();
        return value;
    }
}
