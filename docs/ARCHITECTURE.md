# Architecture Documentation

## System Overview

The High-Throughput Fan-Out Engine is designed as a modular, scalable system that processes large data files and distributes them to multiple downstream systems with transformation, throttling, and resilience.

## Component Architecture

### 1. Ingestion Layer

**Purpose**: Read data from various file formats without loading entire file into memory

**Components**:
- `DataSource` interface: Contract for all data readers
- `CsvDataSource`: Streams CSV files line-by-line
- `JsonlDataSource`: Streams JSON Lines files
- `DataSourceFactory`: Creates appropriate reader based on file type

**Key Features**:
- Lazy evaluation using Java Streams
- Memory-efficient (processes one record at a time)
- Automatic resource cleanup

**Flow**:
```
File → BufferedReader → Stream<String> → Stream<Record>
```

### 2. Transformation Layer

**Purpose**: Convert records into format required by each sink

**Components**:
- `DataTransformer` interface: Strategy pattern for transformations
- `JsonTransformer`: REST API format (JSON)
- `XmlTransformer`: Message Queue format (XML)
- `ProtobufTransformer`: gRPC format (Protocol Buffers)
- `AvroTransformer`: Database format (Apache Avro)
- `TransformerFactory`: Creates transformers based on config

**Design Pattern**: Strategy Pattern
- Allows runtime selection of transformation algorithm
- Easy to add new formats (e.g., MessagePack, YAML)
- Single Responsibility Principle (each transformer handles one format)

### 3. Distribution Layer (Sinks)

**Purpose**: Send transformed data to downstream systems

**Components**:
- `DataSink` interface: Contract for all sinks
- `AbstractSink`: Template method pattern with common functionality
  - Rate limiting (Guava RateLimiter)
  - Retry logic
  - Metrics collection
  - Error handling
- Concrete implementations:
  - `RestApiSink`: HTTP/2 POST requests
  - `GrpcSink`: gRPC streaming
  - `MessageQueueSink`: Kafka/RabbitMQ publishing
  - `WideColumnDbSink`: Cassandra/ScyllaDB upserts

**Design Patterns**:
- Template Method: Common behavior in AbstractSink, specific logic in subclasses
- Factory: SinkFactory creates appropriate sink instances

**Rate Limiting**:
- Uses Guava's `RateLimiter` (Token Bucket algorithm)
- Per-sink configuration (e.g., REST: 50 rps, DB: 1000 rps)
- Thread-safe and fair distribution

### 4. Orchestration Layer

**Purpose**: Coordinate data flow, handle concurrency, and manage system lifecycle

**Components**:
- `FanOutOrchestrator`: Main coordinator
  - Thread pool management (Virtual Threads / ForkJoinPool)
  - Backpressure handling (BlockingQueue)
  - Retry logic with exponential backoff
  - Graceful shutdown

**Concurrency Model**:

#### Option 1: Virtual Threads (Default - Java 21)
```
┌─────────────────────────────────────┐
│  VirtualThreadPerTaskExecutor       │
│  • Lightweight (1M+ threads)        │
│  • Natural blocking code            │
│  • Perfect for I/O operations       │
│  • No thread pool size limits       │
└─────────────────────────────────────┘
```

**Why Virtual Threads**:
- Each sink operation gets its own thread
- Blocking on I/O doesn't consume OS threads
- Simple, readable code (no reactive complexity)
- Automatic work-stealing under the hood

#### Option 2: ForkJoinPool (CPU-intensive)
```
┌─────────────────────────────────────┐
│  ForkJoinPool                       │
│  • Work-stealing algorithm          │
│  • Good for CPU-bound tasks         │
│  • Recursive task decomposition     │
│  • Parallelism = CPU cores          │
└─────────────────────────────────────┘
```

### 5. Resilience & Monitoring

**Components**:
- `MetricsCollector`: Real-time metrics aggregation
  - Thread-safe counters (LongAdder)
  - Per-sink statistics
  - Throughput calculation
- `DeadLetterQueue`: Failed record persistence
  - JSONL format for easy analysis
  - Includes error context and retry history

## Data Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│ 1. INGESTION                                                         │
│    File → DataSource.stream() → Stream<Record>                      │
└───────────────────────┬──────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 2. ORCHESTRATION                                                     │
│    Record → BlockingQueue → FanOutOrchestrator                      │
│    • Apply backpressure if queue full                               │
│    • Fan out to all enabled sinks in parallel                       │
└───────────────────────┬──────────────────────────────────────────────┘
                        │
                        ├─────────┬─────────┬─────────┬──────────┐
                        ▼         ▼         ▼         ▼          ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 3. TRANSFORMATION (Parallel)                                         │
│    Record → Transformer.transform() → byte[]                        │
│    [JSON]     [XML]      [Protobuf]    [Avro]                       │
└───────────────────────┬──────────────────────────────────────────────┘
                        │
                        ├─────────┬─────────┬─────────┬──────────┐
                        ▼         ▼         ▼         ▼          ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 4. DISTRIBUTION (Parallel with Rate Limiting)                        │
│    RateLimiter.acquire() → Sink.sendToSink(byte[])                  │
│    [REST API]  [gRPC]     [MQ]        [DB]                          │
└───────────────────────┬──────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────────────┐
│ 5. RESULT HANDLING                                                   │
│    • Success → MetricsCollector                                     │
│    • Retry → Re-queue with incremented retry count                  │
│    • Failure → MetricsCollector + DeadLetterQueue                   │
└──────────────────────────────────────────────────────────────────────┘
```

## Backpressure Handling

The system uses a BlockingQueue to implement backpressure:

```
Producer (File Reader)
    │
    ▼
┌──────────────────────┐
│  BlockingQueue       │  ← Fixed capacity (e.g., 10,000)
│  [R1][R2][R3]...[Rn] │
└──────────────────────┘
    │
    ▼
Consumers (Sinks)
```

**Behavior**:
1. If queue is full, producer blocks (waits)
2. Prevents OutOfMemoryError from unbounded buffering
3. Natural flow control (slow consumers slow down producers)

**Alternative Approaches Considered**:
- Drop strategy: Loses data (unacceptable)
- Unbounded queue: OOM risk
- Reactive Streams (Flow API): More complex, not needed for this use case

## Error Handling & Retry Logic

```
Record Processing Attempt
    │
    ▼
┌──────────────────┐
│  Try to process  │
└────────┬─────────┘
         │
    ┌────┴────┐
    │         │
Success?    Failure
    │         │
    │         ▼
    │    Retry Count < 3?
    │         │
    │     ┌───┴───┐
    │    Yes      No
    │     │        │
    │     ▼        ▼
    │  Retry     DLQ
    │            │
    ▼            ▼
Metrics      Metrics
 Update       Update
```

**Retry Conditions**:
- Network timeouts
- Temporary service unavailability
- Rate limit exceeded (429)

**No Retry Conditions**:
- Invalid data format (400)
- Authentication failure (401, 403)
- Not found (404)

## Configuration Management

Configuration is externalized in `application.yaml`:

```yaml
# Loaded at startup
Configuration.load("application.yaml")
    │
    ├─> Source config → DataSourceFactory
    ├─> Sink configs → SinkFactory (multiple instances)
    ├─> Thread pool → ExecutorService creation
    └─> Resilience → DLQ, retry settings
```

**Benefits**:
- No code changes for different environments
- Easy to tune performance parameters
- Version controlled configuration
- Can support multiple config files (dev, staging, prod)

## Extensibility

### Adding a New Sink Type

Example: Elasticsearch Sink

1. **Implement the interface**:
```java
public class ElasticsearchSink extends AbstractSink {
    @Override
    protected void sendToSink(byte[] data, Record record) {
        // Elasticsearch bulk API call
    }
}
```

2. **Register in factory**:
```java
case "ELASTICSEARCH" -> new ElasticsearchSink(config);
```

3. **Add to config**:
```yaml
sinks:
  - name: "ELASTICSEARCH"
    type: "ELASTICSEARCH"
    # ...
```

**No changes needed to**:
- Orchestrator
- Other sinks
- Transformation layer
- Monitoring

### Adding a New Transformation Format

Example: MessagePack Transformer

1. **Implement interface**:
```java
public class MessagePackTransformer implements DataTransformer {
    @Override
    public byte[] transform(Record record) {
        // MessagePack serialization
    }
}
```

2. **Register in factory**:
```java
registerTransformer(new MessagePackTransformer());
```

3. **Use in sink config**:
```yaml
transformation: "MESSAGEPACK"
```

## Performance Characteristics

### Memory Usage
- **Heap**: ~512MB for 100GB file processing
- **Why so low**: Streaming + no buffering of entire file
- **GC pressure**: Low (objects are short-lived)

### CPU Usage
- Scales with number of cores
- Virtual threads: efficient CPU utilization even with many threads
- Transformation is CPU-bound; sinks are I/O-bound

### Throughput
```
Throughput = min(
    File Read Speed,
    Transformation Speed,
    Slowest Sink Rate Limit,
    Network Bandwidth
)
```

Typically bottlenecked by sink rate limits.

### Latency
- Record latency: Time from ingestion to all sinks complete
- Affected by:
  - Transformation time: ~1-5ms
  - Network RTT: ~10-100ms
  - Queue wait time: Variable
  - Retry delays: ~100-1000ms

## Production Considerations

### Monitoring
- Export metrics to Prometheus
- Grafana dashboards for visualization
- Alerting on failure rate thresholds

### High Availability
- Run multiple instances (share source file via NFS/S3)
- Use distributed queue (Kafka) instead of file source
- Implement leader election for coordination

### Fault Tolerance
- DLQ for analysis and replay
- Checkpointing for resume capability
- Idempotent sinks (handle duplicates)

### Security
- TLS for all network communication
- API key authentication for sinks
- Encrypt sensitive data in DLQ
- Audit logging for compliance

## Testing Strategy

### Unit Tests
- Individual transformers
- Data source readers
- Sink implementations (mocked)

### Integration Tests
- End-to-end flow with test data
- Multiple sinks simultaneously
- Failure scenarios (retry, DLQ)

### Performance Tests
- Large file processing (>10GB)
- Sustained load testing
- Memory leak detection (heap dumps)
- Rate limit validation

### Chaos Engineering
- Random sink failures
- Network partitions
- Slow sinks (verify backpressure)

## Metrics & Observability

### Collected Metrics
```
# Counter metrics
fanout.records.processed.total
fanout.records.success.total
fanout.records.failed.total
fanout.records.retry.total

# Gauge metrics
fanout.queue.size
fanout.thread.pool.active
fanout.sink.{name}.healthy

# Histogram metrics
fanout.processing.time.ms
fanout.sink.{name}.latency.ms

# Rate metrics (derived)
fanout.throughput.records_per_sec
```

### Log Levels
- **INFO**: Startup, shutdown, milestones
- **DEBUG**: Individual record processing
- **WARN**: Retries, degraded performance
- **ERROR**: Failures, exceptions

## Comparison with Alternatives

### vs. Apache Kafka Streams
- **FanOut Engine**: Better for batch file processing
- **Kafka Streams**: Better for real-time streaming

### vs. Apache NiFi
- **FanOut Engine**: Lightweight, code-first
- **NiFi**: Heavy, GUI-based, more features

### vs. AWS Kinesis
- **FanOut Engine**: Self-hosted, no vendor lock-in
- **Kinesis**: Managed service, easier ops

### vs. Spring Batch
- **FanOut Engine**: Custom, optimized for fan-out
- **Spring Batch**: More features, heavier framework

## Conclusion

The architecture is designed for:
- **Simplicity**: Easy to understand and modify
- **Performance**: Handles large files efficiently
- **Reliability**: Retries, DLQ, zero data loss
- **Extensibility**: New sinks/transformers without core changes
- **Observability**: Rich metrics and logging

The use of modern Java 21 features (Virtual Threads, Records, Sealed Classes potential) keeps the codebase clean and maintainable while delivering excellent performance.
