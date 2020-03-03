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

import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.overrides.source.KafkaSource;
import tlc2.overrides.source.Partition;
import tlc2.overrides.source.Record;
import tlc2.overrides.source.Source;
import tlc2.value.impl.IntValue;
import tlc2.value.impl.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Trace utilities.
 */
public class Traces {
    private static final String TRACE_SOURCE = "TRACE_SOURCE";
    private static final String TRACE_START_TIMESTAMP = "TRACE_START_TIMESTAMP";
    private static final String TRACE_END_TIMESTAMP = "TRACE_END_TIMESTAMP";
    private static final Source SOURCE;
    private static final Queue<Partition> PARTITIONS = new LinkedBlockingQueue<>();

    private static final Long START_TIME;
    private static final Long END_TIME;

    private static final ThreadLocal<Partition> PARTITION = new ThreadLocal<>();
    private static final ThreadLocal<Long> UPPER_BOUND = new ThreadLocal<>();

    private static Source getSource() {
        String consumerInfo = System.getenv(TRACE_SOURCE);
        if (consumerInfo != null) {
            try {
                URI consumerUri = new URI(consumerInfo);
                switch (consumerUri.getScheme()) {
                    case "kafka":
                        String path = consumerUri.getPath().substring(1);
                        if (path.equals("")) {
                            throw new IllegalStateException("No topic specified");
                        }
                        return new KafkaSource(consumerUri.getHost(), consumerUri.getPort(), path);
                    default:
                        throw new IllegalStateException("Unknown consumer scheme");
                }
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    static {
        SOURCE = getSource();
        if (SOURCE != null) {
            synchronized (PARTITIONS) {
                PARTITIONS.addAll(SOURCE.getPartitions());
            }
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

    private static Partition getPartition() {
        Partition partition;
        synchronized (PARTITION) {
            partition = PARTITION.get();
            if (partition == null) {
                partition = PARTITIONS.remove();
                PARTITION.set(partition);
            }
        }
        return partition;
    }

    @TLAPlusOperator(identifier = "LowerBound", module = "Traces")
    public static Value lowerBound() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "start lowerBound");
        // If the start time is not set, return an infinite lower bound.
        if (START_TIME == null) {
            MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "end lowerBound");
            return IntValue.gen(0);
        }

        // Get the thread partition or set it if not already set
        Partition partition = getPartition();

        // Get the first offset following the configured start time.
        long startOffset = partition.offset(START_TIME);
        MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "end lowerBound");
        return IntValue.gen((int) startOffset);
    }

    @TLAPlusOperator(identifier = "UpperBound", module = "Traces")
    public static Value upperBound() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "start upperBound");
        // If the end time is not set, return an infinite upper bound.
        if (END_TIME == null) {
            MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "end upperBound");
            return IntValue.gen(Integer.MAX_VALUE);
        }

        // Get the thread partition or set it if not already set
        Partition partition = getPartition();

        // Get the upper bound.
        Long offset = UPPER_BOUND.get();

        // If the upper bound is not set, get the upper bound from the partition.
        if (offset == null) {
            long endOffset = partition.offset(END_TIME);
            UPPER_BOUND.set(endOffset);
            MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "end upperBound");
            return IntValue.gen((int) endOffset);
        }

        // Get the next offset and return the offset if it exceeds the upper bound timestamp, otherwise
        // return the offset + 1.
        Record record = partition.get(offset);
        if (record.timestamp() >= END_TIME) {
            MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "end upperBound");
            return IntValue.gen((int) (long) offset);
        }
        MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "end upperBound");
        return IntValue.gen((int) (long) offset + 1);
    }

    @TLAPlusOperator(identifier = "Trace", module = "Traces")
    public static Value trace(IntValue offset) throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "start trace " + offset.val);
        // Get the thread partition or set it if not already set
        Partition partition = getPartition();

        // Get the next record and set the upper bound
        UPPER_BOUND.set((long) offset.val + 1);
        Value value = partition.get(offset.val).value();
        MP.printMessage(EC.TLC_MODULE_OVERRIDE_STDOUT, "end trace");
        return value;
    }
}
