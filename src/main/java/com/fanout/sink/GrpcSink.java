package com.fanout.sink;

import com.fanout.model.Record;
import com.fanout.model.SinkConfig;

/**
 * Mock gRPC sink - simulates bi-directional streaming gRPC client
 */
public class GrpcSink extends AbstractSink {

    public GrpcSink(SinkConfig config) {
        super(config);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        // In production: initialize gRPC channel and stub
        logger.info("gRPC Sink initialized for endpoint: {}", config.getEndpoint());
    }

    @Override
    protected void sendToSink(byte[] data, Record record) throws Exception {
        // Simulate gRPC call
        logger.debug("Simulating gRPC call to {} with {} bytes", config.getEndpoint(), data.length);
        
        // Simulate network latency
        Thread.sleep((long) (Math.random() * 30));
        
        // Simulate occasional failures (3% failure rate)
        if (Math.random() < 0.03) {
            throw new RuntimeException("Simulated gRPC error");
        }
        
        // In production, actual gRPC call would be:
        /*
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(config.getEndpoint())
                .usePlaintext()
                .build();
                
        // Use generated stub to make call
        // stub.sendData(request);
        */
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        // In production: shutdown gRPC channel
    }
}
