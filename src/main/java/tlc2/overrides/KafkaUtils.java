// SPDX-FileCopyrightText: 2020-present Open Networking Foundation <info@opennetworking.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import tlc2.value.impl.BoolValue;
import tlc2.value.impl.StringValue;

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

  @TLAPlusOperator(identifier = "KafkaConsume", module = "KafkaUtils")
  public static synchronized IValue consume(StringValue topic) throws IOException {
    if (records == null || !records.hasNext()) {
      records = getConsumer(topic.val.toString())
          .poll(Duration.ofMillis(Long.MAX_VALUE))
          .iterator();
    }
    ConsumerRecord<String, String> record = records.next();
    JsonNode node = mapper.readTree(record.value());
    return JsonUtils.getValue(node);
  }

  @TLAPlusOperator(identifier = "KafkaProduce", module = "KafkaUtils")
  public static synchronized IValue produce(StringValue topic, IValue value) throws IOException {
    getProducer().send(new ProducerRecord<>(topic.val.toString(), JsonUtils.getNode(value).toString()));
    return BoolValue.ValTrue;
  }

  private static Consumer<String, String> getConsumer(String topic) throws IOException {
    Consumer<String, String> consumer = consumers.get(topic);
    if (consumer == null) {
      Properties config = new Properties();
      config.setProperty("bootstrap.servers", KAFKA_SERVERS);
      config.setProperty("client.id", InetAddress.getLocalHost().getHostName());
      config.setProperty("group.id", KAFKA_GROUP);
      config.setProperty("enable.auto.commit", "true");
      config.setProperty("auto.commit.interval.ms", "1000");
      config.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      config.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      config.setProperty("client.dns.lookup", "use_all_dns_ips");
      consumer = new KafkaConsumer<>(config);
      consumer.subscribe(Collections.singleton(topic));
      consumers.put(topic, consumer);
    }
    return consumer;
  }

  private static Producer<String, String> getProducer() throws IOException {
    if (producer == null) {
      Properties config = new Properties();
      config.setProperty("bootstrap.servers", KAFKA_SERVERS);
      config.setProperty("client.id", InetAddress.getLocalHost().getHostName());
      config.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      config.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      config.setProperty("client.dns.lookup", "use_all_dns_ips");
      config.setProperty("acks", "all");
      producer = new KafkaProducer<>(config);
    }
    return producer;
  }
}
