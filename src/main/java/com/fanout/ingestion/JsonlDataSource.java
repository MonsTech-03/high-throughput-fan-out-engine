package com.fanout.ingestion;

import com.fanout.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * JSONL (JSON Lines) file data source - streams records line by line
 */
public class JsonlDataSource implements DataSource {
    private static final Logger logger = LoggerFactory.getLogger(JsonlDataSource.class);
    private final Path filePath;
    private final ObjectMapper objectMapper;
    private BufferedReader reader;

    public JsonlDataSource(String filePath) {
        this.filePath = Path.of(filePath);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Stream<Record> stream() throws Exception {
        logger.info("Opening JSONL file: {}", filePath);
        
        reader = Files.newBufferedReader(filePath);
        
        return reader.lines()
                .filter(line -> !line.trim().isEmpty())
                .map(this::convertToRecord)
                .onClose(() -> {
                    try {
                        close();
                    } catch (Exception e) {
                        logger.error("Error closing JSONL reader", e);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private Record convertToRecord(String jsonLine) {
        try {
            Map<String, Object> data = objectMapper.readValue(jsonLine, Map.class);
            return new Record(data, "JSONL:" + filePath.getFileName());
        } catch (Exception e) {
            logger.error("Failed to parse JSON line: {}", jsonLine, e);
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    @Override
    public String getType() {
        return "JSONL";
    }

    @Override
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
        logger.info("Closed JSONL data source");
    }
}
