package com.fanout.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single data record flowing through the system
 */
public class Record {
    private final String id;
    private final Map<String, Object> data;
    private final Instant timestamp;
    private final String source;
    private int retryCount;

    public Record(Map<String, Object> data, String source) {
        this.id = UUID.randomUUID().toString();
        this.data = Map.copyOf(data);
        this.timestamp = Instant.now();
        this.source = source;
        this.retryCount = 0;
    }

    public Record(String id, Map<String, Object> data, Instant timestamp, String source, int retryCount) {
        this.id = id;
        this.data = Map.copyOf(data);
        this.timestamp = timestamp;
        this.source = source;
        this.retryCount = retryCount;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public Record withIncrementedRetry() {
        return new Record(id, data, timestamp, source, retryCount + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return Objects.equals(id, record.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Record{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", retryCount=" + retryCount +
                ", timestamp=" + timestamp +
                '}';
    }
}
