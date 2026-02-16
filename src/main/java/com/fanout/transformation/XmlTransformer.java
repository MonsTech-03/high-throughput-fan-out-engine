package com.fanout.transformation;

import com.fanout.model.Record;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms records to XML format for message queues
 */
public class XmlTransformer implements DataTransformer {
    private final XmlMapper xmlMapper;

    public XmlTransformer() {
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public byte[] transform(Record record) throws Exception {
        Map<String, Object> output = new HashMap<>();
        output.put("id", record.getId());
        output.put("timestamp", record.getTimestamp().toString());
        output.put("source", record.getSource());
        output.put("data", record.getData());
        
        return xmlMapper.writeValueAsBytes(output);
    }

    @Override
    public String getType() {
        return "XML";
    }
}
