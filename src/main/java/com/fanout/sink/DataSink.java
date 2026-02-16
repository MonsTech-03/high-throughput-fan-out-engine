package com.fanout.sink;

import com.fanout.model.ProcessingResult;
import com.fanout.model.Record;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for all data sinks
 */
public interface DataSink {
    /**
     * Process a single record asynchronously
     */
    CompletableFuture<ProcessingResult> process(Record record);
    
    /**
     * Get sink name
     */
    String getName();
    
    /**
     * Get sink type
     */
    String getType();
    
    /**
     * Initialize the sink
     */
    void initialize() throws Exception;
    
    /**
     * Shutdown the sink gracefully
     */
    void shutdown() throws Exception;
    
    /**
     * Check if sink is healthy
     */
    boolean isHealthy();
}
