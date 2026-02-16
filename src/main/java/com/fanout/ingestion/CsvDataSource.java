package com.fanout.ingestion;

import com.fanout.model.Record;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * CSV file data source - streams records without loading entire file
 */
public class CsvDataSource implements DataSource {
    private static final Logger logger = LoggerFactory.getLogger(CsvDataSource.class);
    private final Path filePath;
    private BufferedReader reader;
    private CSVParser csvParser;

    public CsvDataSource(String filePath) {
        this.filePath = Path.of(filePath);
    }

    @Override
    public Stream<Record> stream() throws Exception {
        logger.info("Opening CSV file: {}", filePath);
        
        reader = Files.newBufferedReader(filePath);
        csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build());

        return StreamSupport.stream(csvParser.spliterator(), false)
                .map(this::convertToRecord)
                .onClose(() -> {
                    try {
                        close();
                    } catch (Exception e) {
                        logger.error("Error closing CSV parser", e);
                    }
                });
    }

    private Record convertToRecord(CSVRecord csvRecord) {
        Map<String, Object> data = new HashMap<>();
        csvRecord.toMap().forEach(data::put);
        return new Record(data, "CSV:" + filePath.getFileName());
    }

    @Override
    public String getType() {
        return "CSV";
    }

    @Override
    public void close() throws Exception {
        if (csvParser != null) {
            csvParser.close();
        }
        if (reader != null) {
            reader.close();
        }
        logger.info("Closed CSV data source");
    }
}
