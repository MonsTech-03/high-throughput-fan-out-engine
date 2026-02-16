package com.fanout.orchestrator;

import com.fanout.config.Configuration;
import com.fanout.ingestion.DataSource;
import com.fanout.ingestion.DataSourceFactory;
import com.fanout.model.ProcessingResult;
import com.fanout.model.Record;
import com.fanout.model.SinkConfig;
import com.fanout.sink.DataSink;
import com.fanout.sink.SinkFactory;
import com.fanout.util.DeadLetterQueue;
import com.fanout.util.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Main orchestrator that coordinates data ingestion, transformation, and distribution
 */
public class FanOutOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(FanOutOrchestrator.class);
    
    private final Configuration config;
    private final List<DataSink> sinks;
    private final ExecutorService executorService;
    private final BlockingQueue<Record> recordQueue;
    private final MetricsCollector metricsCollector;
    private final DeadLetterQueue deadLetterQueue;
    private final ScheduledExecutorService monitoringExecutor;
    
    private volatile boolean running = false;

    public FanOutOrchestrator(Configuration config) {
        this.config = config;
        this.sinks = initializeSinks();
        this.executorService = createExecutorService();
        this.recordQueue = new LinkedBlockingQueue<>(config.getQueueCapacity());
        this.metricsCollector = new MetricsCollector();
        this.deadLetterQueue = new DeadLetterQueue(
                config.getDeadLetterPath(), 
                config.isDeadLetterQueueEnabled()
        );
        this.monitoringExecutor = Executors.newSingleThreadScheduledExecutor();
        
        logger.info("FanOutOrchestrator initialized with {} sinks", sinks.size());
    }

    private List<DataSink> initializeSinks() {
        return config.getSinks().stream()
                .filter(SinkConfig::isEnabled)
                .map(SinkFactory::createSink)
                .collect(Collectors.toList());
    }

    private ExecutorService createExecutorService() {
        String poolType = config.getThreadPoolType();
        logger.info("Creating executor service of type: {}", poolType);
        
        return switch (poolType.toUpperCase()) {
            case "VIRTUAL" -> Executors.newVirtualThreadPerTaskExecutor();
            case "FORK_JOIN" -> new ForkJoinPool(
                    config.getMaxPoolSize(),
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    null,
                    true
            );
            case "FIXED" -> Executors.newFixedThreadPool(config.getMaxPoolSize());
            default -> {
                logger.warn("Unknown pool type {}, defaulting to VIRTUAL", poolType);
                yield Executors.newVirtualThreadPerTaskExecutor();
            }
        };
    }

    public void start() throws Exception {
        logger.info("Starting Fan-Out Engine...");
        running = true;

        // Initialize all sinks
        for (DataSink sink : sinks) {
            sink.initialize();
        }

        // Start monitoring
        monitoringExecutor.scheduleAtFixedRate(
                metricsCollector::printStatus,
                config.getStatusUpdateInterval(),
                config.getStatusUpdateInterval(),
                TimeUnit.SECONDS
        );

        // Create data source
        DataSource dataSource = DataSourceFactory.createDataSource(
                config.getSourceType(),
                config.getSourceFilePath()
        );

        logger.info("Starting data processing from: {}", config.getSourceFilePath());

        try {
            // Process records in streaming fashion
            dataSource.stream()
                    .parallel()
                    .forEach(this::processRecord);
                    
            logger.info("Finished reading all records from source");
            
            // Wait for queue to drain
            waitForQueueToDrain();
            
        } finally {
            dataSource.close();
            shutdown();
        }
    }

    private void processRecord(Record record) {
        try {
            // Add to queue with backpressure handling
            if (!recordQueue.offer(record, 10, TimeUnit.SECONDS)) {
                logger.warn("Queue full, dropping record: {}", record.getId());
                return;
            }

            // Fan out to all sinks concurrently
            List<CompletableFuture<ProcessingResult>> futures = sinks.stream()
                    .map(sink -> sink.process(record))
                    .collect(Collectors.toList());

            // Wait for all sinks to complete processing
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenAccept(v -> {
                        // Process results
                        futures.forEach(future -> {
                            try {
                                ProcessingResult result = future.get();
                                handleResult(result);
                            } catch (Exception e) {
                                logger.error("Error getting result", e);
                            }
                        });
                        
                        // Remove from queue
                        recordQueue.remove(record);
                    })
                    .exceptionally(ex -> {
                        logger.error("Error processing record {}", record.getId(), ex);
                        recordQueue.remove(record);
                        return null;
                    });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while processing record", e);
        }
    }

    private void handleResult(ProcessingResult result) {
        metricsCollector.recordResult(result);
        
        if (result.shouldRetry()) {
            // Retry the record
            Record retriedRecord = result.getRecord().withIncrementedRetry();
            logger.info("Retrying record {} (attempt {})", 
                    retriedRecord.getId(), retriedRecord.getRetryCount());
            
            // Re-process with incremented retry count
            DataSink sink = sinks.stream()
                    .filter(s -> s.getName().equals(result.getSinkName()))
                    .findFirst()
                    .orElse(null);
                    
            if (sink != null) {
                sink.process(retriedRecord)
                        .thenAccept(this::handleResult);
            }
            
        } else if (result.isFailure()) {
            // Write to dead letter queue
            deadLetterQueue.writeFailedRecord(result);
            logger.error("Record {} failed permanently for sink {}", 
                    result.getRecord().getId(), result.getSinkName());
        }
    }

    private void waitForQueueToDrain() throws InterruptedException {
        logger.info("Waiting for queue to drain...");
        while (!recordQueue.isEmpty()) {
            Thread.sleep(100);
        }
        logger.info("Queue drained successfully");
    }

    public void shutdown() throws Exception {
        logger.info("Shutting down Fan-Out Engine...");
        running = false;

        monitoringExecutor.shutdown();
        monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS);

        executorService.shutdown();
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        for (DataSink sink : sinks) {
            sink.shutdown();
        }

        // Final metrics report
        metricsCollector.printStatus();
        
        logger.info("Shutdown complete. Total processed: {}, Success: {}, Failures: {}",
                metricsCollector.getTotalProcessed(),
                metricsCollector.getTotalSuccess(),
                metricsCollector.getTotalFailure());
    }

    public MetricsCollector getMetrics() {
        return metricsCollector;
    }
}
