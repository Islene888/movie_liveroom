package com.ella.backend.consumer;

import com.ella.backend.service.StatsWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Component
public class KafkaAggConsumer {

    @Autowired
    private StatsWebSocketHandler statsWebSocketHandler;

    @PostConstruct
    public void consumeLoop() {
        new Thread(() -> {
            // 1. Kafka 消费者配置
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092"); // Kafka地址改为容器内可访问的地址
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "agg-stats-consumer-group");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList("live-events-agg")); // 聚合topic

            ObjectMapper om = new ObjectMapper();

            while (true) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                    for (ConsumerRecord<String, String> record : records) {
                        String aggJson = record.value();
                        System.out.println("推送聚合数据到前端: " + aggJson);

                        // 解析批次计数（注意字段名和你的Kafka聚合消息一致！）
                        JsonNode node = om.readTree(aggJson);
                        int userJoinBatch = node.path("user_join").asInt(0);
                        int likeBatch = node.path("like").asInt(0);
                        int commentBatch = node.path("comment").asInt(0);
                        int giftBatch = node.path("send_gift").asInt(0);

                        // 累加并推送
                        statsWebSocketHandler.addAndBroadcast(userJoinBatch, likeBatch, commentBatch, giftBatch);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }
}
