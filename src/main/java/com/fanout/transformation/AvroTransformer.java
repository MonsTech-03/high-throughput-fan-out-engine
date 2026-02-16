package com.fanout.transformation;

import com.fanout.model.Record;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Transforms records to Avro format for wide-column databases
 */
public class AvroTransformer implements DataTransformer {
    private final Schema schema;
    private final GenericDatumWriter<GenericRecord> writer;

    public AvroTransformer() {
        // Define a simple Avro schema
        String schemaString = """
            {
                "type": "record",
                "name": "DataRecord",
                "fields": [
                    {"name": "id", "type": "string"},
                    {"name": "timestamp", "type": "string"},
                    {"name": "source", "type": "string"},
                    {"name": "payload", "type": "string"}
                ]
            }
            """;
        this.schema = new Schema.Parser().parse(schemaString);
        this.writer = new GenericDatumWriter<>(schema);
    }

    @Override
    public byte[] transform(Record record) throws Exception {
        GenericRecord avroRecord = new GenericData.Record(schema);
        avroRecord.put("id", record.getId());
        avroRecord.put("timestamp", record.getTimestamp().toString());
        avroRecord.put("source", record.getSource());
        avroRecord.put("payload", record.getData().toString());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        writer.write(avroRecord, encoder);
        encoder.flush();
        
        return outputStream.toByteArray();
    }

    @Override
    public String getType() {
        return "AVRO";
    }
}
