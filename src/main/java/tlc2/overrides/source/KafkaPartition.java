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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumes values from a Kafka partition.
 */
public class KafkaPartition implements Partition {
    private final KafkaConsumerPool pool;
    private final AtomicLong offset = new AtomicLong();

    KafkaPartition(String host, int port, String topic, int partition) {
        this.pool = new KafkaConsumerPool(host, port, topic, partition);
    }

    @Override
    public synchronized Consumer getConsumer() throws IOException {
        KafkaConsumer consumer = pool.acquire();
        consumer.reset(offset.incrementAndGet());
        return consumer;
    }
}
