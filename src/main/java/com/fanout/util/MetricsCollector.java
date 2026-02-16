package com.fanout.util;

import com.fanout.model.ProcessingResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects and tracks processing metrics
 */
public class MetricsCollector {
    private final LongAdder totalProcessed = new LongAdder();
    private final LongAdder totalSuccess = new LongAdder();
    private final LongAdder totalFailure = new LongAdder();
    private final LongAdder totalRetry = new LongAdder();
    
    private final Map<String, SinkMetrics> sinkMetrics = new ConcurrentHashMap<>();
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastProcessedCount = new AtomicLong(0);

    public void recordResult(ProcessingResult result) {
        totalProcessed.increment();
        
        switch (result.getStatus()) {
            case SUCCESS -> totalSuccess.increment();
            case FAILURE -> totalFailure.increment();
            case RETRY -> totalRetry.increment();
        }
        
        sinkMetrics.computeIfAbsent(result.getSinkName(), k -> new SinkMetrics())
                .record(result);
    }

    public void printStatus() {
        long now = System.currentTimeMillis();
        long currentProcessed = totalProcessed.sum();
        long timeSinceLastUpdate = now - lastUpdateTime.get();
        long processedSinceLastUpdate = currentProcessed - lastProcessedCount.get();
        
        double currentThroughput = timeSinceLastUpdate > 0 
                ? (processedSinceLastUpdate * 1000.0) / timeSinceLastUpdate 
                : 0;
        
        long totalTime = now - startTime.get();
        double overallThroughput = totalTime > 0 
                ? (currentProcessed * 1000.0) / totalTime 
                : 0;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 STATUS UPDATE");
        System.out.println("=".repeat(80));
        System.out.printf("Total Processed:    %,d records%n", currentProcessed);
        System.out.printf("Success:            %,d (%.1f%%)%n", 
                totalSuccess.sum(), 
                getPercentage(totalSuccess.sum(), currentProcessed));
        System.out.printf("Failures:           %,d (%.1f%%)%n", 
                totalFailure.sum(), 
                getPercentage(totalFailure.sum(), currentProcessed));
        System.out.printf("Retries:            %,d%n", totalRetry.sum());
        System.out.printf("Current Throughput: %.2f records/sec%n", currentThroughput);
        System.out.printf("Overall Throughput: %.2f records/sec%n", overallThroughput);
        System.out.println("-".repeat(80));
        
        System.out.println("Per-Sink Metrics:");
        sinkMetrics.forEach((sinkName, metrics) -> {
            System.out.printf("  %s:%n", sinkName);
            System.out.printf("    Success: %,d | Failures: %,d | Avg Time: %.2fms%n",
                    metrics.successCount.sum(),
                    metrics.failureCount.sum(),
                    metrics.getAverageProcessingTime());
        });
        System.out.println("=".repeat(80) + "\n");
        
        lastUpdateTime.set(now);
        lastProcessedCount.set(currentProcessed);
    }

    private double getPercentage(long part, long total) {
        return total > 0 ? (part * 100.0) / total : 0;
    }

    public long getTotalProcessed() {
        return totalProcessed.sum();
    }

    public long getTotalSuccess() {
        return totalSuccess.sum();
    }

    public long getTotalFailure() {
        return totalFailure.sum();
    }

    private static class SinkMetrics {
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final LongAdder totalProcessingTime = new LongAdder();
        private final LongAdder recordCount = new LongAdder();

        void record(ProcessingResult result) {
            if (result.isSuccess()) {
                successCount.increment();
            } else if (result.isFailure()) {
                failureCount.increment();
            }
            totalProcessingTime.add(result.getProcessingTimeMs());
            recordCount.increment();
        }

        double getAverageProcessingTime() {
            long count = recordCount.sum();
            return count > 0 ? (double) totalProcessingTime.sum() / count : 0;
        }
    }
}
