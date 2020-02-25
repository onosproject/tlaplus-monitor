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
package tlc2.overrides.monitoring;

import org.apache.kafka.clients.producer.ProducerRecord;
import tlc2.overrides.JsonUtils;
import tlc2.value.IValue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

/**
 * Produces values to a Kafka topic.
 */
public class KafkaProducer implements Producer {
    private final org.apache.kafka.clients.producer.Producer<String, String> producer;
    private final String topic;

    public KafkaProducer(String host, int port, String topic) throws IOException {
        this.producer = getProducer(host, port);
        this.topic = topic;
    }

    @Override
    public void produce(IValue value) throws IOException {
        producer.send(new ProducerRecord<>(topic, JsonUtils.getNode(value).toString()));
    }

    private static org.apache.kafka.clients.producer.Producer<String, String> getProducer(String host, int port) throws IOException {
        Properties config = new Properties();
        config.setProperty("bootstrap.servers", String.format("%s:%d", host, port));
        config.setProperty("client.id", InetAddress.getLocalHost().getHostName());
        config.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.setProperty("client.dns.lookup", "use_all_dns_ips");
        config.setProperty("acks", "all");
        return new org.apache.kafka.clients.producer.KafkaProducer<>(config);
    }
}
