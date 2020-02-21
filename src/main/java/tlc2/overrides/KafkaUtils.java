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

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import tlc2.value.IValue;

/**
 * Kafka utilities.
 */
public class KafkaUtils {
  private static final String KAFKA_GROUP = System.getenv("KAFKA_GROUP");
  private static final String KAFKA_SERVERS = System.getenv("KAFKA_SERVERS");

  private static final Map<String, Consumer<String, String>> consumers = new HashMap<>();
  private static Producer<String, String> producer;
  private static Iterator<ConsumerRecord<String, String>> records;
  private static ObjectMapper mapper = new ObjectMapper();

  @TLAPlusOperator(identifier = "KafkaConsume", module = "Kafka")
  public static synchronized IValue consume(String topic) throws IOException {
    if (records == null || !records.hasNext()) {
      records = getConsumer(topic).poll(Duration.ofMillis(Long.MAX_VALUE)).iterator();
    }
    ConsumerRecord<String, String> record = records.next();
    JsonNode node = mapper.readTree(record.value());
    return JsonUtils.getValue(node);
  }

  @TLAPlusOperator(identifier = "KafkaProduce", module = "Kafka")
  public static synchronized void produce(String topic, IValue value) throws IOException {
    getProducer().send(new ProducerRecord<>(topic, JsonUtils.getNode(value).toString()));
  }

  private static Consumer<String, String> getConsumer(String topic) throws IOException {
    Consumer<String, String> consumer = consumers.get(topic);
    if (consumer == null) {
      Properties config = new Properties();
      config.put("bootstrap.servers", KAFKA_SERVERS);
      config.put("client.id", InetAddress.getLocalHost().getHostName());
      config.put("group.id", KAFKA_GROUP);
      config.setProperty("enable.auto.commit", "true");
      config.setProperty("auto.commit.interval.ms", "1000");
      config.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      config.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      consumer = new KafkaConsumer<>(config);
      consumer.subscribe(Collections.singleton(topic));
      consumers.put(topic, consumer);
    }
    return consumer;
  }

  private static Producer<String, String> getProducer() throws IOException {
    if (producer == null) {
      Properties config = new Properties();
      config.put("bootstrap.servers", KAFKA_SERVERS);
      config.put("client.id", InetAddress.getLocalHost().getHostName());
      config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      config.put("acks", "all");
      producer = new KafkaProducer<>(config);
    }
    return producer;
  }
}
