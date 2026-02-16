package com.fanout.sink;

import com.fanout.model.Record;
import com.fanout.model.SinkConfig;

/**
 * Mock Wide-Column Database sink - simulates asynchronous UPSERT to Cassandra/ScyllaDB
 */
public class WideColumnDbSink extends AbstractSink {

    public WideColumnDbSink(SinkConfig config) {
        super(config);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        // In production: initialize Cassandra session
        logger.info("Wide-Column DB Sink initialized for endpoint: {} keyspace: {} table: {}", 
                config.getEndpoint(), config.getKeyspace(), config.getTable());
    }

    @Override
    protected void sendToSink(byte[] data, Record record) throws Exception {
        // Simulate database UPSERT
        logger.debug("Simulating UPSERT to {}.{} with {} bytes", 
                config.getKeyspace(), config.getTable(), data.length);
        
        // Simulate database latency
        Thread.sleep((long) (Math.random() * 10));
        
        // Simulate occasional failures (1% failure rate)
        if (Math.random() < 0.01) {
            throw new RuntimeException("Simulated DB error");
        }
        
        // In production with Cassandra:
        /*
        String cql = "INSERT INTO " + config.getKeyspace() + "." + config.getTable() + 
                     " (id, timestamp, data) VALUES (?, ?, ?)";
        PreparedStatement prepared = session.prepare(cql);
        BoundStatement bound = prepared.bind(record.getId(), 
                record.getTimestamp(), ByteBuffer.wrap(data));
        session.executeAsync(bound);
        */
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        // In production: close Cassandra session
    }
}
