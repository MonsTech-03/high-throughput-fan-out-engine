#!/bin/bash

# Run script for Fan-Out Engine

set -e

echo "╔══════════════════════════════════════════════════════════╗"
echo "║      High-Throughput Fan-Out Engine                     ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# Check Java version
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21 or higher."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✓ Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Build if needed
if [ ! -f "target/fan-out-engine-1.0.0.jar" ]; then
    echo "📦 Building project..."
    mvn clean package -DskipTests
    echo "✓ Build complete"
    echo ""
fi

# Create necessary directories
mkdir -p logs dlq

# Run the application
echo "🚀 Starting Fan-Out Engine..."
echo ""

CONFIG_FILE="${1:-src/main/resources/application.yaml}"
HEAP_SIZE="${HEAP_SIZE:-512m}"

java -Xmx${HEAP_SIZE} \
     -XX:+UseZGC \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xlog:gc*:file=logs/gc.log \
     -jar target/fan-out-engine-1.0.0.jar \
     "$CONFIG_FILE"

echo ""
echo "✓ Fan-Out Engine completed"
