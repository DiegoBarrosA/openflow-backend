#!/bin/bash
# Script to run tests in Podman container
# Usage: ./podman-test.sh [test-class-name]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building test image...${NC}"
podman build -t openflow-backend-test:latest -f Dockerfile.test .

echo -e "${BLUE}Running tests in Podman container...${NC}"

# Create target directories if they don't exist
mkdir -p target/surefire-reports target/site/jacoco

# Run tests
if [ -z "$1" ]; then
    echo -e "${GREEN}Running all tests...${NC}"
    podman run --rm \
        -v "$SCRIPT_DIR/target/surefire-reports:/app/target/surefire-reports:Z" \
        -v "$SCRIPT_DIR/target/site/jacoco:/app/target/site/jacoco:Z" \
        openflow-backend-test:latest \
        mvn test
else
    echo -e "${GREEN}Running test class: $1${NC}"
    podman run --rm \
        -v "$SCRIPT_DIR/target/surefire-reports:/app/target/surefire-reports:Z" \
        -v "$SCRIPT_DIR/target/site/jacoco:/app/target/site/jacoco:Z" \
        openflow-backend-test:latest \
        mvn test -Dtest="$1"
fi

echo -e "${GREEN}Tests completed!${NC}"
echo -e "${BLUE}Test reports available in: target/surefire-reports${NC}"
echo -e "${BLUE}Coverage reports available in: target/site/jacoco${NC}"

