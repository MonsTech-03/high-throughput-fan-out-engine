package com.fanout.transformation;

import com.fanout.model.Record;

/**
 * Strategy pattern interface for data transformation
 */
public interface DataTransformer {
    /**
     * Transform a record to the target format
     * @param record the source record
     * @return transformed data as byte array
     */
    byte[] transform(Record record) throws Exception;
    
    /**
     * Get the transformation type
     */
    String getType();
}
