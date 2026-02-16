package com.fanout;

import com.fanout.config.Configuration;
import com.fanout.orchestrator.FanOutOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point
 */
public class FanOutEngine {
    private static final Logger logger = LoggerFactory.getLogger(FanOutEngine.class);

    public static void main(String[] args) {
        logger.info("╔══════════════════════════════════════════════════════════╗");
        logger.info("║      High-Throughput Fan-Out Engine v1.0.0              ║");
        logger.info("║      Distributed Data Fan-Out & Transformation          ║");
        logger.info("╚══════════════════════════════════════════════════════════╝");

        try {
            // Load configuration
            String configPath = args.length > 0 ? args[0] : "application.yaml";
            Configuration config = Configuration.load(configPath);
            
            logger.info("Configuration loaded successfully");
            logger.info("Source: {} - {}", config.getSourceType(), config.getSourceFilePath());
            logger.info("Enabled sinks: {}", config.getSinks().stream()
                    .filter(com.fanout.model.SinkConfig::isEnabled)
                    .map(com.fanout.model.SinkConfig::getName)
                    .toList());

            // Create and start orchestrator
            FanOutOrchestrator orchestrator = new FanOutOrchestrator(config);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown hook triggered");
                try {
                    orchestrator.shutdown();
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));

            // Start processing
            orchestrator.start();

            logger.info("Fan-Out Engine completed successfully");
            System.exit(0);

        } catch (Exception e) {
            logger.error("Fatal error in Fan-Out Engine", e);
            System.exit(1);
        }
    }
}
