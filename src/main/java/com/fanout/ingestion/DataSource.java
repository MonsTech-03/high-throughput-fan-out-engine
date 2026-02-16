package com.fanout.ingestion;

import com.fanout.model.Record;

import java.util.stream.Stream;

/**
 * Interface for data source readers
 */
public interface DataSource {
    /**
     * Get a stream of records from the data source
     * This uses streaming to avoid loading entire file into memory
     */
    Stream<Record> stream() throws Exception;
    
    /**
     * Get the source type
     */
    String getType();
    
    /**
     * Close the data source
     */
    void close() throws Exception;
}
