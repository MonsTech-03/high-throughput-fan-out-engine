package com.fanout.config;

import com.fanout.model.SinkConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Application configuration loader
 */
public class Configuration {
    private Map<String, Object> application;
    private Map<String, Object> source;
    private Map<String, Object> threadPool;
    private List<SinkConfig> sinks;
    private Map<String, Object> backpressure;
    private Map<String, Object> monitoring;
    private Map<String, Object> resilience;

    private static Configuration instance;

    public static Configuration load(String configPath) throws Exception {
        if (instance == null) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream inputStream = Configuration.class.getClassLoader()
                    .getResourceAsStream(configPath);
            
            if (inputStream == null) {
                throw new IllegalArgumentException("Config file not found: " + configPath);
            }
            
            instance = mapper.readValue(inputStream, Configuration.class);
        }
        return instance;
    }

    public static Configuration getInstance() {
        if (instance == null) {
            try {
                return load("application.yaml");
            } catch (Exception e) {
                throw new RuntimeException("Failed to load configuration", e);
            }
        }
        return instance;
    }

    // Getters
    public Map<String, Object> getApplication() {
        return application;
    }

    public void setApplication(Map<String, Object> application) {
        this.application = application;
    }

    public Map<String, Object> getSource() {
        return source;
    }

    public void setSource(Map<String, Object> source) {
        this.source = source;
    }

    public Map<String, Object> getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(Map<String, Object> threadPool) {
        this.threadPool = threadPool;
    }

    public List<SinkConfig> getSinks() {
        return sinks;
    }

    public void setSinks(List<SinkConfig> sinks) {
        this.sinks = sinks;
    }

    public Map<String, Object> getBackpressure() {
        return backpressure;
    }

    public void setBackpressure(Map<String, Object> backpressure) {
        this.backpressure = backpressure;
    }

    public Map<String, Object> getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Map<String, Object> monitoring) {
        this.monitoring = monitoring;
    }

    public Map<String, Object> getResilience() {
        return resilience;
    }

    public void setResilience(Map<String, Object> resilience) {
        this.resilience = resilience;
    }

    // Convenience methods
    public String getSourceType() {
        return (String) source.get("type");
    }

    public String getSourceFilePath() {
        return (String) source.get("filePath");
    }

    public int getBatchSize() {
        return (int) source.get("batchSize");
    }

    public String getThreadPoolType() {
        return (String) threadPool.get("type");
    }

    public int getCorePoolSize() {
        return (int) threadPool.get("corePoolSize");
    }

    public int getMaxPoolSize() {
        return (int) threadPool.get("maxPoolSize");
    }

    public int getQueueCapacity() {
        return (int) backpressure.get("queueCapacity");
    }

    public int getStatusUpdateInterval() {
        return (int) monitoring.get("statusUpdateIntervalSeconds");
    }

    public boolean isDeadLetterQueueEnabled() {
        return (boolean) resilience.get("deadLetterQueueEnabled");
    }

    public String getDeadLetterPath() {
        return (String) resilience.get("deadLetterPath");
    }
}
