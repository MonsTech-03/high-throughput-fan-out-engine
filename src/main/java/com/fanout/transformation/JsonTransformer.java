package com.fanout.transformation;

import com.fanout.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms records to JSON format for REST APIs
 */
public class JsonTransformer implements DataTransformer {
    private final ObjectMapper objectMapper;

    public JsonTransformer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public byte[] transform(Record record) throws Exception {
        Map<String, Object> output = new HashMap<>();
        output.put("id", record.getId());
        output.put("timestamp", record.getTimestamp().toString());
        output.put("source", record.getSource());
        output.put("data", record.getData());
        
        return objectMapper.writeValueAsBytes(output);
    }

    @Override
    public String getType() {
        return "JSON";
    }
}
