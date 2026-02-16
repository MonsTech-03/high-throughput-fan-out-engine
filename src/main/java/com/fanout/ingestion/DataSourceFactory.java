package com.fanout.ingestion;

/**
 * Factory for creating data source instances
 */
public class DataSourceFactory {
    
    public static DataSource createDataSource(String type, String filePath) {
        return switch (type.toUpperCase()) {
            case "CSV" -> new CsvDataSource(filePath);
            case "JSONL" -> new JsonlDataSource(filePath);
            case "FIXED_WIDTH" -> throw new UnsupportedOperationException(
                    "Fixed-width format not yet implemented");
            default -> throw new IllegalArgumentException("Unknown data source type: " + type);
        };
    }
}
