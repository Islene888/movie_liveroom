package com.ella.backend.service;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);
    private final String KAFKA_TOPIC = "live-events"; // 定义要发送到的Topic


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String eventMessage) {
        kafkaTemplate.send(KAFKA_TOPIC, eventMessage);
        log.info("🚀 成功发送事件到Kafka Topic [{}]: {}", KAFKA_TOPIC, eventMessage);
    }



}