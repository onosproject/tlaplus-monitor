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

import tlc2.TLCGlobals;
import tlc2.monitor.source.Partition;
import tlc2.monitor.source.Record;
import tlc2.monitor.source.Source;
import tlc2.monitor.source.Sources;
import tlc2.monitor.util.Logger;
import tlc2.monitor.util.ModuleLogger;
import tlc2.tool.ModelChecker;
import tlc2.value.impl.BoolValue;
import tlc2.value.impl.IntValue;
import tlc2.value.impl.Value;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Trace utilities.
 */
public class Traces {
    private static final Logger LOGGER = new ModuleLogger();

    public static final String SOURCE_ENV = "TLC_TRACES_SOURCE";
    public static final String PARTITION_ENV = "TLC_TRACES_PARTITION";
    public static final String WINDOW_START_ENV = "TLC_TRACES_WINDOW_START";
    public static final String WINDOW_END_ENV = "TLC_TRACES_WINDOW_END";

    private static final Source SOURCE;
    private static final Partition PARTITION;

    private static final Long START_TIME;
    private static final Long END_TIME;
    private static volatile Long index;

    private static Integer currentLevel;
    private static Integer currentOffset;
    private static Long upperBound;

    static {
        String uri = System.getenv(SOURCE_ENV);
        if (uri != null) {
            Source source = null;
            try {
                source = Sources.getSource(uri);
            } catch (URISyntaxException | IOException e) {
                LOGGER.log("Failed to load source %s: %s", uri, e);
                e.printStackTrace();
            }
            SOURCE = source;
        } else {
            SOURCE = null;
        }

        if (SOURCE != null) {
            int partitionId = Integer.valueOf(System.getenv(PARTITION_ENV));
            PARTITION = SOURCE.getPartition(partitionId);
        } else {
            PARTITION = null;
        }
    }

    static {
        String startTime = System.getenv(WINDOW_START_ENV);
        if (startTime != null) {
            START_TIME = Long.parseLong(startTime);
        } else {
            START_TIME = null;
        }
        String endTime = System.getenv(WINDOW_END_ENV);
        if (endTime != null) {
            END_TIME = Long.parseLong(endTime);
        } else {
            END_TIME = null;
        }
    }

    private static void assertSource() {
        if (SOURCE == null) {
            throw new IllegalStateException("No source configured. Are you sure TLC is running in monitor mode?");
        }
    }

    /**
     * Returns the index for the given offset.
     *
     * @param offset the offset for which to return the index
     * @return the index for the given offset
     */
    private static long indexOf(int offset) throws IOException {
        return index() + offset;
    }

    /**
     * Computes the starting trace index.
     *
     * @return the starting trace index
     */
    private static long index() throws IOException {
        assertSource();
        if (index == null) {
            // If the start time is not set, return an infinite lower bound.
            if (START_TIME == null) {
                return 0;
            }

            // Get the first offset following the configured start time.
            long startIndex = PARTITION.indexOf(START_TIME);
            if (startIndex == 0) {
                index = 1L;
            } else {
                index = startIndex;
            }
        }
        return index;
    }

    @TLAPlusOperator(identifier = "Trace", module = "Traces")
    public static Value trace(IntValue offset) throws IOException {
        assertSource();
        final ModelChecker mc = (ModelChecker) TLCGlobals.mainChecker;
        currentLevel = mc.trace.getLevel();
        currentOffset = offset.val;
        return PARTITION.get(indexOf(offset.val)).value();
    }

    @TLAPlusOperator(identifier = "UpperBound", module = "Traces")
    public static Value upperBound() throws IOException {
        assertSource();

        // If the trace level has reset, unset the level and offset.
        final ModelChecker mc = (ModelChecker) TLCGlobals.mainChecker;
        if (mc.trace.getLevel() < currentLevel) {
            currentLevel = 0;
            currentOffset = 0;
            return BoolValue.ValFalse;
        }

        // If the end time is not set, return an infinite upper bound.
        if (END_TIME == null) {
            return BoolValue.ValFalse;
        }

        // If the current offset is not set, return false.
        if (currentOffset == null) {
            return BoolValue.ValFalse;
        }

        // If the upper bound is set, return whether the current offset is greater than the upper bound.
        if (upperBound != null) {
            return currentOffset >= upperBound ? BoolValue.ValTrue : BoolValue.ValFalse;
        }

        // Get the next offset and determine whether it exceeds the upper bound.
        Record record = PARTITION.get(indexOf(currentOffset + 1));
        if (record.timestamp() >= END_TIME) {
            upperBound = currentOffset + 1L;
            return BoolValue.ValTrue;
        }
        return BoolValue.ValFalse;
    }
}
