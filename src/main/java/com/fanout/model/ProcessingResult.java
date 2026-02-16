package com.fanout.model;

import java.time.Instant;

/**
 * Represents the result of processing a record through a sink
 */
public class ProcessingResult {
    private final Record record;
    private final String sinkName;
    private final Status status;
    private final String errorMessage;
    private final Instant processedAt;
    private final long processingTimeMs;

    public enum Status {
        SUCCESS,
        FAILURE,
        RETRY
    }

    private ProcessingResult(Record record, String sinkName, Status status, 
                            String errorMessage, long processingTimeMs) {
        this.record = record;
        this.sinkName = sinkName;
        this.status = status;
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
        this.processingTimeMs = processingTimeMs;
    }

    public static ProcessingResult success(Record record, String sinkName, long processingTimeMs) {
        return new ProcessingResult(record, sinkName, Status.SUCCESS, null, processingTimeMs);
    }

    public static ProcessingResult failure(Record record, String sinkName, 
                                          String errorMessage, long processingTimeMs) {
        return new ProcessingResult(record, sinkName, Status.FAILURE, errorMessage, processingTimeMs);
    }

    public static ProcessingResult retry(Record record, String sinkName, 
                                        String errorMessage, long processingTimeMs) {
        return new ProcessingResult(record, sinkName, Status.RETRY, errorMessage, processingTimeMs);
    }

    public Record getRecord() {
        return record;
    }

    public String getSinkName() {
        return sinkName;
    }

    public Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public boolean shouldRetry() {
        return status == Status.RETRY;
    }

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "recordId='" + record.getId() + '\'' +
                ", sinkName='" + sinkName + '\'' +
                ", status=" + status +
                ", processingTimeMs=" + processingTimeMs +
                (errorMessage != null ? ", error='" + errorMessage + '\'' : "") +
                '}';
    }
}
