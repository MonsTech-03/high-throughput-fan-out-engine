package com.fanout.sink;

import com.fanout.model.SinkConfig;

/**
 * Factory for creating sink instances based on configuration
 */
public class SinkFactory {
    
    public static DataSink createSink(SinkConfig config) {
        return switch (config.getType().toUpperCase()) {
            case "REST" -> new RestApiSink(config);
            case "GRPC" -> new GrpcSink(config);
            case "MQ" -> new MessageQueueSink(config);
            case "DB" -> new WideColumnDbSink(config);
            default -> throw new IllegalArgumentException("Unknown sink type: " + config.getType());
        };
    }
}
