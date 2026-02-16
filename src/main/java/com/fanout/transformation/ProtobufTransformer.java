package com.fanout.transformation;

import com.fanout.model.Record;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Transforms records to Protocol Buffer format for gRPC
 * Note: This is a simplified implementation. In production, you would use generated protobuf classes
 */
public class ProtobufTransformer implements DataTransformer {

    @Override
    public byte[] transform(Record record) throws Exception {
        // Simplified protobuf encoding
        // In production, use actual .proto definitions and generated classes
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(record.getId()).append(";");
        sb.append("timestamp:").append(record.getTimestamp()).append(";");
        sb.append("source:").append(record.getSource()).append(";");
        sb.append("data:").append(record.getData().toString());
        
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getType() {
        return "PROTOBUF";
    }
}
