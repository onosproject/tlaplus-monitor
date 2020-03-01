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

import tlc2.overrides.source.Consumer;
import tlc2.overrides.source.KafkaSource;
import tlc2.overrides.source.Partition;
import tlc2.overrides.source.Source;
import tlc2.value.impl.BoolValue;
import tlc2.value.impl.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trace utilities.
 */
public class Traces {
    private static final String TRACES_SOURCE = "TRACES_SOURCE";
    private static final Source SOURCE;
    private static final List<Partition> PARTITIONS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger PARTITION_INDEX = new AtomicInteger();
    private static final ThreadLocal<Consumer> CONSUMER = new ThreadLocal<>();

    private static Source getSource() {
        String consumerInfo = System.getenv(TRACES_SOURCE);
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
            PARTITIONS.addAll(SOURCE.getPartitions());
        }
    }

    @TLAPlusOperator(identifier = "BeginWindow", module = "Traces")
    public static Value beginWindow() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        // If a consumer is already defined for this thread, the window is already open.
        Consumer consumer = CONSUMER.get();
        if (consumer != null) {
            return BoolValue.ValFalse;
        }

        // Select a partition and get the next window for this thread.
        int partitionIndex = PARTITION_INDEX.incrementAndGet();
        Partition partition = PARTITIONS.get(partitionIndex % PARTITIONS.size());
        consumer = partition.getConsumer();
        CONSUMER.set(consumer);
        return BoolValue.ValTrue;
    }

    @TLAPlusOperator(identifier = "NextWindow", module = "Traces")
    public static Value nextWindow() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        // Get the consumer for this thread.
        Consumer consumer = CONSUMER.get();
        if (consumer == null) {
            throw new IllegalStateException("Not in a window");
        }
        return (Value) consumer.next();
    }

    @TLAPlusOperator(identifier = "EndWindow", module = "Traces")
    public static Value endWindow() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }

        // Get the consumer for this thread.
        Consumer consumer = CONSUMER.get();
        if (consumer == null) {
            return BoolValue.ValFalse;
        }

        // Close the consumer and unset the thread local.
        consumer.close();
        CONSUMER.set(null);
        return BoolValue.ValTrue;
    }
}
