package com.fanout.sink;

import com.fanout.model.ProcessingResult;
import com.fanout.model.Record;
import com.fanout.model.SinkConfig;
import com.fanout.transformation.DataTransformer;
import com.fanout.transformation.TransformerFactory;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for all sinks with common functionality
 */
public abstract class AbstractSink implements DataSink {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final SinkConfig config;
    protected final DataTransformer transformer;
    protected final RateLimiter rateLimiter;
    protected volatile boolean healthy = true;

    protected AbstractSink(SinkConfig config) {
        this.config = config;
        this.transformer = TransformerFactory.getTransformer(config.getTransformation());
        this.rateLimiter = RateLimiter.create(config.getRateLimit());
    }

    @Override
    public CompletableFuture<ProcessingResult> process(Record record) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Apply rate limiting
                rateLimiter.acquire();
                
                // Transform the record
                byte[] transformedData = transformer.transform(record);
                
                // Send to sink
                sendToSink(transformedData, record);
                
                long processingTime = System.currentTimeMillis() - startTime;
                logger.debug("Successfully processed record {} in {}ms", record.getId(), processingTime);
                
                return ProcessingResult.success(record, getName(), processingTime);
                
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                logger.error("Failed to process record {}: {}", record.getId(), e.getMessage());
                
                if (record.getRetryCount() < config.getRetryAttempts()) {
                    return ProcessingResult.retry(record, getName(), e.getMessage(), processingTime);
                } else {
                    return ProcessingResult.failure(record, getName(), e.getMessage(), processingTime);
                }
            }
        });
    }

    /**
     * Template method for sending data to the specific sink implementation
     */
    protected abstract void sendToSink(byte[] data, Record record) throws Exception;

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getType() {
        return config.getType();
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing sink: {}", getName());
    }

    @Override
    public void shutdown() throws Exception {
        logger.info("Shutting down sink: {}", getName());
    }
}
