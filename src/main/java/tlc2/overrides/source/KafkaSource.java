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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import tlc2.overrides.JsonUtils;
import tlc2.value.IValue;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

/**
 * Consumes values from a Kafka topic.
 */
public class KafkaSource implements Source {
    private final org.apache.kafka.clients.consumer.Consumer<String, String> consumer;
    private Iterator<ConsumerRecord<String, String>> records;
    private ObjectMapper mapper = new ObjectMapper();

    public KafkaSource(String host, int port, String topic) throws IOException {
        this.consumer = getConsumer(host, port, topic);
    }

    @Override
    public IValue consume() throws IOException {
        if (records == null || !records.hasNext()) {
            records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE)).iterator();
        }
        ConsumerRecord<String, String> record = records.next();
        JsonNode node = mapper.readTree(record.value());
        return JsonUtils.getValue(node);
    }

    private static org.apache.kafka.clients.consumer.Consumer<String, String> getConsumer(String host, int port, String topic) throws IOException {
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
        consumer.subscribe(Collections.singleton(topic));
        return consumer;
    }
}
