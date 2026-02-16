package com.fanout.util;

import com.fanout.model.ProcessingResult;
import com.fanout.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Dead Letter Queue for failed records
 */
public class DeadLetterQueue {
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueue.class);
    private final Path dlqPath;
    private final ObjectMapper objectMapper;
    private boolean enabled;

    public DeadLetterQueue(String dlqPath, boolean enabled) {
        this.dlqPath = Path.of(dlqPath);
        this.objectMapper = new ObjectMapper();
        this.enabled = enabled;
        
        if (enabled) {
            try {
                Files.createDirectories(this.dlqPath);
                logger.info("Dead Letter Queue initialized at: {}", this.dlqPath);
            } catch (IOException e) {
                logger.error("Failed to create DLQ directory", e);
                this.enabled = false;
            }
        }
    }

    public void writeFailedRecord(ProcessingResult result) {
        if (!enabled) {
            return;
        }

        try {
            Map<String, Object> dlqEntry = new HashMap<>();
            dlqEntry.put("recordId", result.getRecord().getId());
            dlqEntry.put("sinkName", result.getSinkName());
            dlqEntry.put("errorMessage", result.getErrorMessage());
            dlqEntry.put("retryCount", result.getRecord().getRetryCount());
            dlqEntry.put("failedAt", Instant.now().toString());
            dlqEntry.put("originalData", result.getRecord().getData());

            String json = objectMapper.writeValueAsString(dlqEntry) + "\n";
            Path dlqFile = dlqPath.resolve("failed-records.jsonl");
            
            Files.writeString(dlqFile, json, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.APPEND);
            
            logger.debug("Wrote failed record to DLQ: {}", result.getRecord().getId());
            
        } catch (IOException e) {
            logger.error("Failed to write to DLQ", e);
        }
    }
}
