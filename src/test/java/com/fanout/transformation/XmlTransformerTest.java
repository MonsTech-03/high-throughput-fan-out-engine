package com.fanout.transformation;

import com.fanout.model.Record;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlTransformerTest {

    @Test
    void testTransformRecord() throws Exception {
        // Arrange
        Map<String, Object> data = Map.of(
                "name", "Test User",
                "email", "test@example.com"
        );
        Record record = new Record(data, "TEST");
        XmlTransformer transformer = new XmlTransformer();

        // Act
        byte[] result = transformer.transform(record);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
        
        // Verify it contains XML tags
        String xml = new String(result);
        assertTrue(xml.contains("<"));
        assertTrue(xml.contains(">"));
    }

    @Test
    void testGetType() {
        XmlTransformer transformer = new XmlTransformer();
        assertEquals("XML", transformer.getType());
    }
}
