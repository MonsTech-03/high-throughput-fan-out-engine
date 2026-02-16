# Quick Setup Guide

This guide will help you get the High-Throughput Fan-Out Engine up and running in minutes.

## Prerequisites

1. **Java 21** or higher
   ```bash
   # Check Java version
   java -version
   
   # Should show: openjdk version "21" or higher
   ```

2. **Maven 3.8+**
   ```bash
   # Check Maven version
   mvn -version
   ```

3. **Git** (to clone the repository)

## Step-by-Step Setup

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd fan-out-engine
```

### Step 2: Build the Project

**Option A: Using the build script (recommended)**
```bash
./build.sh
```

**Option B: Using Maven directly**
```bash
mvn clean package
```

This will:
- Clean any previous builds
- Compile all source code
- Run unit tests
- Create executable JAR file

**Expected output:**
```
✅ Build successful!
📊 JAR size: ~15M
```

### Step 3: Run the Application

**Option A: Using the run script (recommended)**
```bash
./run.sh
```

**Option B: Using Java directly**
```bash
java -Xmx512m -jar target/fan-out-engine-1.0.0.jar
```

**Option C: Run with custom configuration**
```bash
./run.sh path/to/custom-config.yaml
```

### Step 4: Observe the Output

You should see:
1. Startup banner and configuration details
2. Status updates every 5 seconds showing:
   - Total records processed
   - Success/failure counts
   - Throughput (records/sec)
   - Per-sink metrics

Example output:
```
================================================================================
📊 STATUS UPDATE
================================================================================
Total Processed:    1,000 records
Success:            980 (98.0%)
Failures:           20 (2.0%)
Current Throughput: 234.56 records/sec
Overall Throughput: 198.23 records/sec
--------------------------------------------------------------------------------
Per-Sink Metrics:
  REST_API:
    Success: 245 | Failures: 5 | Avg Time: 42.35ms
  ...
================================================================================
```

## Verify Installation

### Check Logs
```bash
# View application logs
tail -f logs/fan-out-engine.log

# View GC logs
tail -f logs/gc.log
```

### Check Dead Letter Queue
```bash
# View failed records (if any)
cat dlq/failed-records.jsonl
```

## Configuration

### Default Configuration
The default configuration is in `src/main/resources/application.yaml`

### Custom Configuration
1. Copy the default config:
   ```bash
   cp src/main/resources/application.yaml my-config.yaml
   ```

2. Edit as needed:
   ```yaml
   source:
     file-path: "path/to/your/data.csv"  # Change this
   
   sinks:
     - name: "REST_API"
       enabled: true  # Enable/disable sinks
       rate-limit: 100  # Adjust rate limits
   ```

3. Run with custom config:
   ```bash
   ./run.sh my-config.yaml
   ```

## Testing with Custom Data

### Create Test CSV File
```bash
cat > my-data.csv << 'EOF'
id,name,email,value
1,Alice,alice@test.com,100
2,Bob,bob@test.com,200
3,Charlie,charlie@test.com,300
EOF
```

### Update Configuration
```yaml
source:
  type: "CSV"
  file-path: "my-data.csv"
```

### Run
```bash
./run.sh
```

## Troubleshooting

### "Java command not found"
- Install Java 21: https://adoptium.net/

### "Maven command not found"
- Install Maven: https://maven.apache.org/install.html

### "OutOfMemoryError"
- Increase heap size:
  ```bash
  HEAP_SIZE=1g ./run.sh
  ```

### "File not found" for data file
- Check the `file-path` in `application.yaml`
- Ensure the file exists and is readable
- Use absolute path if needed

### Tests failing during build
- Run without tests first:
  ```bash
  mvn clean package -DskipTests
  ```
- Then investigate test failures:
  ```bash
  mvn test
  ```

### Port already in use (for future real implementations)
- The mock sinks don't use real ports
- When implementing real sinks, change ports in config

## Running Tests Only

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JsonTransformerTest

# Run with verbose output
mvn test -X
```

## Development Mode

### Run from source (without building JAR)
```bash
mvn exec:java -Dexec.mainClass="com.fanout.FanOutEngine"
```

### Enable debug logging
Edit `src/main/resources/logback.xml`:
```xml
<logger name="com.fanout" level="DEBUG"/>
```

### Hot reload with Maven
```bash
mvn clean compile exec:java -Dexec.mainClass="com.fanout.FanOutEngine"
```

## Performance Tuning

### For High Throughput
```yaml
thread-pool:
  type: "VIRTUAL"
  max-pool-size: 200  # Increase

backpressure:
  queue-capacity: 50000  # Increase

sinks:
  - rate-limit: 500  # Increase per sink
```

### For Low Memory
```bash
# Run with minimal heap
java -Xmx256m -jar target/fan-out-engine-1.0.0.jar
```

### Monitor Performance
```bash
# Use JConsole
jconsole

# Or VisualVM
jvisualvm

# Connect to the running process
```

## Next Steps

1. **Read the Architecture**: `docs/ARCHITECTURE.md`
2. **Review the Code**: Start with `FanOutEngine.java`
3. **Customize**: Add your own sinks or transformers
4. **Deploy**: See production considerations in README

## Quick Reference

| Command | Description |
|---------|-------------|
| `./build.sh` | Build the project |
| `./run.sh` | Run with default config |
| `./run.sh config.yaml` | Run with custom config |
| `mvn test` | Run tests |
| `mvn clean` | Clean build artifacts |
| `tail -f logs/*.log` | View logs |

## Getting Help

- Check `README.md` for detailed documentation
- Review `docs/ARCHITECTURE.md` for design details
- Look at test files in `src/test/` for examples
- Check logs in `logs/` directory

## Success Checklist

- [ ] Java 21+ installed
- [ ] Maven 3.8+ installed
- [ ] Project built successfully (`./build.sh`)
- [ ] Application runs without errors (`./run.sh`)
- [ ] Sample data processed successfully
- [ ] Logs created in `logs/` directory
- [ ] Status updates printed every 5 seconds

If all items are checked, you're ready to start customizing!

---

**Need more help?** Open an issue in the repository or refer to the comprehensive README.md
