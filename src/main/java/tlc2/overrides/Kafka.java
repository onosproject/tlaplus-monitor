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
import java.util.Iterator;
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
public class Kafka {
  private static Consumer<String, String> consumer;
  private static Producer<String, String> producer;
  private static Iterator<ConsumerRecord<String, String>> records;
  private static ObjectMapper mapper = new ObjectMapper();

  @TLAPlusOperator(identifier = "KafkaConsume", module = "Kafka")
  public static synchronized IValue consume(String topic) throws IOException {
    if (records == null || !records.hasNext()) {
      records = getConsumer().poll(Duration.ofMillis(Long.MAX_VALUE)).iterator();
    }
    ConsumerRecord<String, String> record = records.next();
    JsonNode node = mapper.readTree(record.value());
    return Json.getValue(node);
  }

  @TLAPlusOperator(identifier = "KafkaProduce", module = "Kafka")
  public static synchronized void produce(String topic, IValue value) throws IOException {
    getProducer().send(new ProducerRecord<>(topic, Json.getNode(value).toString()));
  }

  private static Consumer<String, String> getConsumer() throws IOException {
    if (consumer == null) {
      Properties config = new Properties();
      // TODO
      config.put("group.id", "foo");
      config.put("client.id", InetAddress.getLocalHost().getHostName());
      config.put("bootstrap.servers", "host1:9092,host2:9092");
      consumer = new KafkaConsumer<>(config);
    }
    return consumer;
  }

  private static Producer<String, String> getProducer() throws IOException {
    if (producer == null) {
      Properties config = new Properties();
      // TODO
      config.put("client.id", InetAddress.getLocalHost().getHostName());
      config.put("bootstrap.servers", "host1:9092,host2:9092");
      config.put("acks", "all");
      producer = new KafkaProducer<>(config);
    }
    return producer;
  }
}
