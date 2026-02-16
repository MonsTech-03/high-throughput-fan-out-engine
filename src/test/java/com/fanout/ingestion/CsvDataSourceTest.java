package com.fanout.ingestion;

import com.fanout.model.Record;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CsvDataSourceTest {

    @TempDir
    Path tempDir;

    @Test
    void testStreamCsvFile() throws Exception {
        // Arrange
        Path csvFile = tempDir.resolve("test.csv");
        String csvContent = """
                name,email,age
                Alice,alice@test.com,25
                Bob,bob@test.com,30
                """;
        Files.writeString(csvFile, csvContent);

        CsvDataSource dataSource = new CsvDataSource(csvFile.toString());

        // Act
        List<Record> records = dataSource.stream().collect(Collectors.toList());

        // Assert
        assertEquals(2, records.size());
        
        Record firstRecord = records.get(0);
        assertEquals("Alice", firstRecord.getData().get("name"));
        assertEquals("alice@test.com", firstRecord.getData().get("email"));
        assertEquals("25", firstRecord.getData().get("age"));
        
        dataSource.close();
    }

    @Test
    void testGetType() {
        CsvDataSource dataSource = new CsvDataSource("dummy.csv");
        assertEquals("CSV", dataSource.getType());
    }
}
