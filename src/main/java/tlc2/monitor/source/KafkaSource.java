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

import org.apache.kafka.common.PartitionInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes values from a Kafka topic.
 */
public class KafkaSource implements Source {
    private static final String SCHEME = "kafka";

    private final String uri;
    private final Map<Integer, Partition> partitions = new ConcurrentHashMap<>();

    public KafkaSource(String uri) throws URISyntaxException, IOException {
        this.uri = uri;

        URI source = new URI(uri);
        if (!source.getScheme().equals(SCHEME)) {
            throw new IllegalStateException("Unknown source scheme " + source.getScheme());
        }
        String topic = source.getPath().substring(1);
        if (topic.equals("")) {
            throw new IllegalStateException("No topic specified");
        }
        getPartitions(source.getHost(), source.getPort(), topic)
            .forEach(partition -> partitions.put(partition.id(), partition));
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public Partition getPartition(int partition) {
        return partitions.get(partition);
    }

    @Override
    public Collection<Partition> getPartitions() {
        return partitions.values();
    }

    @Override
    public String toString() {
        return uri;
    }

    private static Collection<Partition> getPartitions(String host, int port, String topic) throws IOException {
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
        List<PartitionInfo> partitionsInfo = consumer.partitionsFor(topic);
        List<Partition> partitions = new ArrayList<>(partitionsInfo.size());
        for (PartitionInfo partitionInfo : partitionsInfo) {
            partitions.add(new KafkaPartition(host, port, topic, partitionInfo.partition()));
        }
        return partitions;
    }
}
