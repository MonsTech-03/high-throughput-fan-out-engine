package com.fanout.transformation;

import com.fanout.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTransformerTest {

    @Test
    void testTransformRecord() throws Exception {
        // Arrange
        Map<String, Object> data = Map.of(
                "name", "Test User",
                "email", "test@example.com",
                "age", 30
        );
        Record record = new Record(data, "TEST");
        JsonTransformer transformer = new JsonTransformer();

        // Act
        byte[] result = transformer.transform(record);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);

        // Verify it's valid JSON
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> parsed = mapper.readValue(result, Map.class);
        
        assertNotNull(parsed.get("id"));
        assertNotNull(parsed.get("timestamp"));
        assertEquals("TEST", parsed.get("source"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsedData = (Map<String, Object>) parsed.get("data");
        assertEquals("Test User", parsedData.get("name"));
        assertEquals("test@example.com", parsedData.get("email"));
    }

    @Test
    void testGetType() {
        JsonTransformer transformer = new JsonTransformer();
        assertEquals("JSON", transformer.getType());
    }
}
