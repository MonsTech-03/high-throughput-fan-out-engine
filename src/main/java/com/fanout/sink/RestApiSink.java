package com.fanout.sink;

import com.fanout.model.Record;
import com.fanout.model.SinkConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Mock REST API sink - simulates HTTP/2 POST requests
 */
public class RestApiSink extends AbstractSink {
    private HttpClient httpClient;

    public RestApiSink(SinkConfig config) {
        super(config);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
        logger.info("REST API Sink initialized for endpoint: {}", config.getEndpoint());
    }

    @Override
    protected void sendToSink(byte[] data, Record record) throws Exception {
        // Simulate HTTP POST request
        // In production, this would actually send to the endpoint
        
        logger.debug("Simulating POST to {} with {} bytes", config.getEndpoint(), data.length);
        
        // Simulate network latency
        Thread.sleep((long) (Math.random() * 50));
        
        // Simulate occasional failures (5% failure rate for testing)
        if (Math.random() < 0.05) {
            throw new RuntimeException("Simulated network error");
        }
        
        // Uncomment below for actual HTTP requests
        /*
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getEndpoint()))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
                
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
                
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP error: " + response.statusCode());
        }
        */
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        // HttpClient doesn't need explicit shutdown
    }
}
