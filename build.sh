#!/bin/bash

# Build script for Fan-Out Engine

set -e

echo "╔══════════════════════════════════════════════════════════╗"
echo "║      Building Fan-Out Engine                            ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.8 or higher."
    exit 1
fi

echo "✓ Maven version: $(mvn -version | head -n 1)"
echo ""

# Clean
echo "🧹 Cleaning previous builds..."
mvn clean
echo "✓ Clean complete"
echo ""

# Compile
echo "🔨 Compiling source code..."
mvn compile
echo "✓ Compilation complete"
echo ""

# Run tests
echo "🧪 Running tests..."
mvn test
echo "✓ Tests passed"
echo ""

# Package
echo "📦 Creating JAR file..."
mvn package -DskipTests
echo "✓ JAR created: target/fan-out-engine-1.0.0.jar"
echo ""

# Show JAR size
JAR_SIZE=$(du -h target/fan-out-engine-1.0.0.jar | awk '{print $1}')
echo "📊 JAR size: $JAR_SIZE"
echo ""

echo "✅ Build successful!"
echo ""
echo "To run the application:"
echo "  ./run.sh"
echo "  or"
echo "  java -Xmx512m -jar target/fan-out-engine-1.0.0.jar"
