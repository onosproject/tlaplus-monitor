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
package tlc2.monitor.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import tlc2.overrides.JsonUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
        long endOffset = consumer.endOffsets(Collections.singleton(partition)).get(partition) - 1;
        if (endOffset == -1) {
            return 0;
        }

        Record record = get(endOffset);
        if (record.timestamp() < timestamp) {
            return record.offset();
        }

        Map<TopicPartition, Long> times = new HashMap<>();
        times.put(partition, timestamp);
        endOffset = consumer.offsetsForTimes(times).get(partition).offset();
        if (endOffset == 0) {
            return 1;
        }
        return endOffset;
    }

    @Override
    public Record get(long offset) throws IOException {
        Record record = records.get(offset);
        if (record == null) {
            records.clear();
            consumer.seek(partition, offset);
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
            for (ConsumerRecord<String, String> r : records) {
                this.records.put(r.offset(), new Record(r.offset(), JsonUtils.getValue(mapper.readTree(r.value())), r.timestamp()));
            }
            record = this.records.get(offset);
        }
        return record;
    }

    @Override
    public String toString() {
        return String.format("%s/%d", partition.topic(), partition.partition());
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
