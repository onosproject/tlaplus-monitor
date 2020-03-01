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
package tlc2.overrides.source;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Kafka consumer pool
 */
final class KafkaConsumerPool {
    private final String host;
    private final int port;
    private final String topic;
    private final int partition;
    private final Queue<KafkaConsumer> consumers = new ArrayDeque<>();

    KafkaConsumerPool(String host, int port, String topic, int partition) {
        this.host = host;
        this.port = port;
        this.topic = topic;
        this.partition = partition;
    }

    /**
     * Acquires a consumer from the pool.
     *
     * @return the consumer
     * @throws IOException if the consumer connection failed
     */
    synchronized KafkaConsumer acquire() throws IOException {
        KafkaConsumer consumer = consumers.poll();
        if (consumer == null) {
            consumer = new KafkaConsumer(host, port, topic, partition, this);
        }
        return consumer;
    }

    /**
     * Releases a consumer to the pool.
     *
     * @param consumer the consumer
     */
    synchronized void release(KafkaConsumer consumer) {
        consumers.add(consumer);
    }
}
