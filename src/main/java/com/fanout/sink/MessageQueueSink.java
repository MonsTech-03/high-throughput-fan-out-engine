package com.fanout.sink;

import com.fanout.model.Record;
import com.fanout.model.SinkConfig;

/**
 * Mock Message Queue sink - simulates publishing to Kafka/RabbitMQ
 */
public class MessageQueueSink extends AbstractSink {

    public MessageQueueSink(SinkConfig config) {
        super(config);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        // In production: initialize Kafka producer or RabbitMQ connection
        logger.info("Message Queue Sink initialized for endpoint: {} topic: {}", 
                config.getEndpoint(), config.getTopic());
    }

    @Override
    protected void sendToSink(byte[] data, Record record) throws Exception {
        // Simulate message publishing
        logger.debug("Simulating publish to topic {} with {} bytes", 
                config.getTopic(), data.length);
        
        // Simulate network latency
        Thread.sleep((long) (Math.random() * 20));
        
        // Simulate occasional failures (2% failure rate)
        if (Math.random() < 0.02) {
            throw new RuntimeException("Simulated MQ error");
        }
        
        // In production with Kafka:
        /*
        ProducerRecord<String, byte[]> producerRecord = 
                new ProducerRecord<>(config.getTopic(), record.getId(), data);
        producer.send(producerRecord).get();
        */
        
        // In production with RabbitMQ:
        /*
        channel.basicPublish("", config.getTopic(), null, data);
        */
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        // In production: close producer/connection
    }
}
