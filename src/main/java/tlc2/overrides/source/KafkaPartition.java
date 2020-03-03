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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import tlc2.overrides.JsonUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes values from a Kafka partition.
 */
public class KafkaPartition implements Partition {
    private final TopicPartition partition;
    private final Consumer<String, String> consumer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Long, Record> records = new ConcurrentHashMap<>();

    KafkaPartition(String host, int port, String topic, int partition) throws IOException {
        this.partition = new TopicPartition(topic, partition);
        this.consumer = getConsumer(host, port, topic, partition);
    }

    @Override
    public int id() {
        return partition.partition();
    }

    @Override
    public long offset(long timestamp) throws IOException {
        long endOffset = consumer.endOffsets(Collections.singleton(partition)).get(partition);
        Record record = get(endOffset);
        if (record.timestamp() < timestamp) {
            return record.offset();
        }

        Map<TopicPartition, Long> times = new HashMap<>();
        times.put(partition, timestamp);
        return consumer.offsetsForTimes(times).get(partition).offset();
    }

    @Override
    public Record get(long offset) throws IOException {
        Record record = records.get(offset);
        if (record == null) {
            consumer.seek(partition, offset);
            Iterator<ConsumerRecord<String, String>> iterator = consumer.poll(Duration.ofMillis(Long.MAX_VALUE)).iterator();
            while (iterator.hasNext()) {
                ConsumerRecord<String, String> consumerRecord = iterator.next();
                records.put(consumerRecord.offset(), new Record(consumerRecord.offset(), JsonUtils.getValue(mapper.readTree(consumerRecord.value())), consumerRecord.timestamp()));
            }
            record = records.get(offset);
        }
        return record;
    }

    private static Consumer<String, String> getConsumer(String host, int port, String topic, int partition) throws IOException {
        Properties config = new Properties();
        config.setProperty("bootstrap.servers", String.format("%s:%d", host, port));
        config.setProperty("client.id", InetAddress.getLocalHost().getHostName());
        config.setProperty("group.id", InetAddress.getLocalHost().getHostName());
        config.setProperty("enable.auto.commit", "false");
        config.setProperty("auto.offset.reset", "earliest");
        config.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.setProperty("client.dns.lookup", "use_all_dns_ips");
        org.apache.kafka.clients.consumer.Consumer<String, String> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(config);
        consumer.assign(Collections.singleton(new TopicPartition(topic, partition)));
        return consumer;
    }
}
