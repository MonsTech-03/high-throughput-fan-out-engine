# AI Prompts Used for Development

This document contains all the prompts used with AI coding assistants during the development of the High-Throughput Fan-Out Engine project, as required by the assignment submission guidelines.

## Project Setup Prompts

### Initial Project Structure
```
Create a Java 21 Maven project for a high-throughput fan-out engine. Include:
- Project structure with src/main/java and src/test/java
- POM.xml with dependencies for Jackson, Protobuf, gRPC, Avro, Guava, SLF4J, JUnit
- Use Java 21 features including Virtual Threads
- Configure Maven compiler plugin for Java 21 with preview features
```

### Configuration System
```
Create a YAML-based configuration system for the fan-out engine that includes:
- Source configuration (file type, path, batch size)
- Thread pool settings (type, core/max pool size)
- Sink configurations (name, type, endpoint, rate limit, retry attempts, timeout)
- Backpressure settings (queue capacity, strategy)
- Monitoring settings (status update interval)
- Resilience settings (DLQ enabled, DLQ path)
- Use Jackson for YAML parsing
```

## Core Model Prompts

### Record Model
```
Create a Record model class that:
- Has an immutable design with final fields
- Includes: id (UUID), data (Map<String, Object>), timestamp, source
- Supports retry counting
- Has a method to create a new Record with incremented retry count
- Implements equals/hashCode based on id
- Has a clean toString() for debugging
```

### Processing Result Model
```
Create a ProcessingResult model that:
- Represents the outcome of processing a record through a sink
- Has status enum (SUCCESS, FAILURE, RETRY)
- Includes record reference, sink name, error message, processing time
- Has static factory methods for success(), failure(), retry()
- Has convenience methods isSuccess(), isFailure(), shouldRetry()
```

## Transformation Layer Prompts

### Transformer Interface
```
Create a DataTransformer interface using the Strategy pattern that:
- Has a transform(Record) method returning byte[]
- Has a getType() method returning the transformer type
- Allows easy addition of new transformation formats
```

### JSON Transformer
```
Implement a JsonTransformer that:
- Uses Jackson ObjectMapper
- Converts Record to JSON format suitable for REST APIs
- Includes id, timestamp, source, and data fields in output
- Handles exceptions appropriately
```

### XML Transformer
```
Implement an XmlTransformer that:
- Uses Jackson XmlMapper
- Converts Record to XML format for message queues
- Follows same structure as JSON transformer
- Handles exceptions appropriately
```

### Protobuf Transformer
```
Implement a ProtobufTransformer that:
- Simulates Protocol Buffer encoding
- Note: In production this would use generated .proto classes
- For now, create a simplified string-based encoding
- Returns byte array representation
```

### Avro Transformer
```
Implement an AvroTransformer that:
- Uses Apache Avro for serialization
- Defines a schema for DataRecord with id, timestamp, source, payload
- Uses GenericRecord for schema-based serialization
- Returns binary-encoded Avro data
```

### Transformer Factory
```
Create a TransformerFactory using Factory pattern that:
- Maintains a registry of all available transformers
- Has a static getTransformer(type) method
- Supports registering custom transformers
- Throws IllegalArgumentException for unknown types
```

## Sink Layer Prompts

### Sink Interface
```
Create a DataSink interface that:
- Has process(Record) method returning CompletableFuture<ProcessingResult>
- Has getName(), getType(), initialize(), shutdown() methods
- Has isHealthy() method for health checks
- Supports asynchronous processing
```

### Abstract Sink Base Class
```
Create an AbstractSink base class using Template Method pattern that:
- Implements common functionality: rate limiting, retry logic, error handling
- Uses Guava RateLimiter for throttling
- Has abstract sendToSink(byte[], Record) method for subclasses
- Handles transformation via DataTransformer
- Implements the process() method with complete error handling
```

### REST API Sink
```
Create a RestApiSink that:
- Extends AbstractSink
- Simulates HTTP/2 POST requests
- Uses Java 11+ HttpClient
- Simulates network latency (random 0-50ms)
- Simulates occasional failures (5% rate) for testing
- Has commented-out code showing real HTTP implementation
```

### gRPC Sink
```
Create a GrpcSink that:
- Extends AbstractSink
- Simulates bi-directional streaming gRPC
- Simulates network latency (random 0-30ms)
- Simulates occasional failures (3% rate)
- Has commented-out code showing real gRPC channel creation
```

### Message Queue Sink
```
Create a MessageQueueSink that:
- Extends AbstractSink
- Simulates publishing to Kafka/RabbitMQ
- Simulates network latency (random 0-20ms)
- Simulates occasional failures (2% rate)
- Has commented-out code for Kafka Producer and RabbitMQ Channel
```

### Wide Column DB Sink
```
Create a WideColumnDbSink that:
- Extends AbstractSink
- Simulates asynchronous UPSERT to Cassandra/ScyllaDB
- Simulates DB latency (random 0-10ms)
- Simulates occasional failures (1% rate)
- Has commented-out code for Cassandra PreparedStatement
```

### Sink Factory
```
Create a SinkFactory that:
- Takes SinkConfig and returns appropriate DataSink instance
- Uses switch expression (Java 21 pattern matching)
- Supports REST, GRPC, MQ, DB sink types
- Throws IllegalArgumentException for unknown types
```

## Ingestion Layer Prompts

### Data Source Interface
```
Create a DataSource interface that:
- Has stream() method returning Stream<Record>
- Uses Java Streams for memory-efficient processing
- Has getType() and close() methods
- Supports try-with-resources pattern
```

### CSV Data Source
```
Create a CsvDataSource that:
- Uses Apache Commons CSV for parsing
- Opens file with BufferedReader (not loading entire file)
- Returns Stream<Record> for lazy evaluation
- Converts CSVRecord to Record model
- Properly closes resources
- Handles headers automatically
```

### JSONL Data Source
```
Create a JsonlDataSource that:
- Reads JSON Lines format (one JSON object per line)
- Uses BufferedReader.lines() for streaming
- Uses Jackson ObjectMapper to parse each line
- Filters out empty lines
- Handles errors in individual lines gracefully
```

### Data Source Factory
```
Create a DataSourceFactory that:
- Takes type and filePath parameters
- Returns appropriate DataSource implementation
- Supports CSV, JSONL formats
- Has placeholder for FIXED_WIDTH (not yet implemented)
```

## Utility Layer Prompts

### Metrics Collector
```
Create a MetricsCollector that:
- Uses thread-safe counters (LongAdder) for metrics
- Tracks: total processed, success, failure, retry counts
- Maintains per-sink metrics (success, failure, avg processing time)
- Calculates throughput (records/sec) - both current and overall
- Has printStatus() method for formatted output every 5 seconds
- Uses emoji for visual appeal in status updates
```

### Dead Letter Queue
```
Create a DeadLetterQueue utility that:
- Writes failed records to JSONL format
- Includes: record ID, sink name, error message, retry count, timestamp, original data
- Creates DLQ directory if it doesn't exist
- Appends to failed-records.jsonl file
- Can be enabled/disabled via configuration
- Handles I/O errors gracefully
```

## Orchestration Layer Prompts

### Fan-Out Orchestrator
```
Create a FanOutOrchestrator that:
- Coordinates data ingestion, transformation, and distribution
- Manages thread pool (Virtual Threads, ForkJoinPool, or Fixed)
- Implements backpressure using BlockingQueue
- Fans out records to all enabled sinks in parallel
- Handles retry logic with max 3 attempts
- Integrates MetricsCollector and DeadLetterQueue
- Has start() and shutdown() methods with graceful termination
- Prints status updates every 5 seconds
- Waits for queue to drain before shutdown
```

### Main Application
```
Create the FanOutEngine main class that:
- Is the application entry point
- Loads configuration from YAML (default or from args)
- Creates and starts the FanOutOrchestrator
- Adds shutdown hook for graceful termination
- Has nice ASCII art banner
- Logs configuration details at startup
- Exits with appropriate status code
```

## Testing Prompts

### Transformer Tests
```
Create unit tests for JsonTransformer that:
- Test basic transformation of Record to JSON
- Verify output is valid JSON
- Check that all fields are present
- Verify getType() returns correct value
- Use JUnit 5 assertions
```

```
Create unit tests for XmlTransformer that:
- Test basic transformation to XML
- Verify output contains XML tags
- Test getType() method
```

### Data Source Tests
```
Create unit tests for CsvDataSource that:
- Use @TempDir for test files
- Create test CSV file programmatically
- Verify correct number of records parsed
- Check record data integrity
- Test resource cleanup
```

### Integration Test Concepts
```
Outline integration tests that would:
- Test end-to-end flow with all sinks
- Verify retry logic works correctly
- Test DLQ for failed records
- Measure throughput under load
- Verify backpressure prevents OOM
- Test graceful shutdown
```

## Documentation Prompts

### README Creation
```
Create a comprehensive README.md that includes:
- Architecture diagram in ASCII art
- Feature list with checkmarks
- Quick start guide (prerequisites, build, run)
- Configuration explanation
- Sample output showing metrics
- Design patterns used with explanations
- Key design decisions with rationale
- Project structure overview
- Testing instructions
- Scalability information
- How to add new sinks (extensibility example)
- Monitoring and observability details
- Troubleshooting section
- Assumptions made
- Future enhancements
- Professional formatting with sections
```

### Architecture Documentation
```
Create detailed ARCHITECTURE.md that includes:
- System overview
- Component architecture for each layer
- Data flow diagrams
- Backpressure handling explanation
- Error handling and retry logic flow
- Configuration management approach
- Extensibility examples (adding new sink/transformer)
- Performance characteristics
- Production considerations
- Testing strategy
- Metrics and observability details
- Comparison with alternatives (Kafka, NiFi, etc.)
- Conclusion summarizing design principles
```

## Build and Configuration Prompts

### Maven POM
```
Create pom.xml for Java 21 project that includes:
- Maven Compiler Plugin configured for Java 21
- Maven Shade Plugin for fat JAR creation
- Dependencies: Jackson (core, XML, YAML), Commons CSV, Protobuf, gRPC, Avro, Guava, SLF4J, Logback, JUnit 5, Mockito
- Properties for dependency versions
- Surefire plugin for test execution
```

### Logback Configuration
```
Create logback.xml that:
- Configures console and file appenders
- Uses rolling file appender (daily rotation, 7 day retention)
- Sets log pattern with timestamp, thread, level, logger, message
- Sets com.fanout package to INFO level
- Creates logs in logs/ directory
```

### Sample Data
```
Create sample-data.csv with:
- Headers: id, name, email, age, country, department, salary
- 10 sample employee records
- Realistic data for testing
```

### Git Ignore
```
Create .gitignore for Java/Maven project that excludes:
- Maven target directory
- IDE files (.idea, *.iml, .vscode)
- Log files
- DLQ output
- OS-specific files (.DS_Store)
- Build artifacts
```

## Refinement and Optimization Prompts

### Memory Optimization
```
Review the code and ensure:
- No loading of entire file into memory
- Streaming is used throughout
- Objects are short-lived for efficient GC
- Queue has bounded capacity
- Can run with -Xmx512m for 100GB files
```

### Concurrency Review
```
Review concurrency implementation for:
- Thread safety of shared state
- Proper use of Virtual Threads
- No race conditions in metrics collection
- Correct CompletableFuture usage
- Proper resource cleanup in shutdown
```

### Code Quality Review
```
Review code for:
- Proper exception handling
- Informative logging
- Clear variable names
- Good separation of concerns
- SOLID principles adherence
- Design pattern correctness
```

## Notes on Prompt Evolution

Throughout development, prompts were refined based on:
1. **Clarity**: Making requirements more specific
2. **Completeness**: Adding missing details
3. **Correctness**: Fixing technical inaccuracies
4. **Context**: Providing better examples

The iterative process helped achieve a production-quality codebase that meets all assignment requirements while following Java best practices and design patterns.

---

**Note**: These prompts represent the key development steps. In practice, many small refinement prompts were used to polish specific aspects, fix bugs, and improve code quality. The prompts above capture the essential instructions that shaped the architecture and implementation.
