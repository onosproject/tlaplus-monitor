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
package tlc2.monitor.sink;

import org.apache.kafka.clients.producer.ProducerRecord;
import tlc2.overrides.JsonUtils;
import tlc2.value.IValue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Produces values to a Kafka topic.
 */
public class KafkaSink implements Sink {
    static final String SCHEME = "kafka";

    private final String uri;
    private final org.apache.kafka.clients.producer.Producer<String, String> producer;
    private final String topic;

    public KafkaSink(String uri) throws URISyntaxException, IOException {
        this.uri = uri;

        URI source = new URI(uri);
        if (!source.getScheme().equals(SCHEME)) {
            throw new IllegalStateException("Unknown sink scheme " + source.getScheme());
        }
        String topic = source.getPath().substring(1);
        if (topic.equals("")) {
            throw new IllegalStateException("No topic specified");
        }
        this.producer = getProducer(source.getHost(), source.getPort());
        this.topic = topic;
    }

    @Override
    public String uri() {
        return uri;
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

    @Override
    public String toString() {
        return uri;
    }
}
