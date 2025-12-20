#!/bin/bash
# =============================================================================
# OWASP ZAP Security Scan Script (SEC-01)
# =============================================================================
# This script runs OWASP ZAP baseline security scan against the OpenFlow API.
# 
# Usage:
#   ./zap-scan.sh [target_url]
#
# Examples:
#   ./zap-scan.sh                          # Scan localhost:8080
#   ./zap-scan.sh http://staging.example.com  # Scan staging environment
#
# Requirements:
#   - Docker or Podman installed
#   - Target application running
#
# Output:
#   - HTML report: reports/zap-report.html
#   - JSON report: reports/zap-report.json
# =============================================================================

set -e

# Configuration
TARGET_URL="${1:-http://localhost:8080}"
REPORT_DIR="$(dirname "$0")/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
CONTAINER_RUNTIME="${CONTAINER_RUNTIME:-docker}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== OWASP ZAP Security Scan ===${NC}"
echo "Target: $TARGET_URL"
echo "Container Runtime: $CONTAINER_RUNTIME"
echo ""

# Create report directory
mkdir -p "$REPORT_DIR"

# Check if container runtime is available
if ! command -v "$CONTAINER_RUNTIME" &> /dev/null; then
    echo -e "${RED}Error: $CONTAINER_RUNTIME is not installed${NC}"
    exit 1
fi

# Check if target is reachable
echo -e "${YELLOW}Checking target availability...${NC}"
if ! curl -s --head "$TARGET_URL" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Target may not be reachable. Continuing anyway...${NC}"
fi

# Run ZAP Baseline Scan
echo -e "${YELLOW}Starting ZAP baseline scan...${NC}"
echo "This may take several minutes..."
echo ""

$CONTAINER_RUNTIME run --rm \
    -v "$(pwd)/$REPORT_DIR:/zap/wrk:rw" \
    -t ghcr.io/zaproxy/zaproxy:stable \
    zap-baseline.py \
    -t "$TARGET_URL" \
    -g gen.conf \
    -r "zap-report-${TIMESTAMP}.html" \
    -J "zap-report-${TIMESTAMP}.json" \
    -I \
    || SCAN_EXIT_CODE=$?

# Check scan results
if [ "${SCAN_EXIT_CODE:-0}" -eq 0 ]; then
    echo -e "${GREEN}=== Scan completed successfully ===${NC}"
    echo "No high-risk vulnerabilities found."
elif [ "${SCAN_EXIT_CODE:-0}" -eq 1 ]; then
    echo -e "${YELLOW}=== Scan completed with warnings ===${NC}"
    echo "Some alerts were raised. Review the report."
elif [ "${SCAN_EXIT_CODE:-0}" -eq 2 ]; then
    echo -e "${RED}=== Scan completed with FAIL alerts ===${NC}"
    echo "Critical vulnerabilities found! Review the report immediately."
else
    echo -e "${RED}=== Scan failed ===${NC}"
    echo "Exit code: $SCAN_EXIT_CODE"
fi

echo ""
echo "Reports generated:"
echo "  - $REPORT_DIR/zap-report-${TIMESTAMP}.html"
echo "  - $REPORT_DIR/zap-report-${TIMESTAMP}.json"
echo ""

# Create symlink to latest report
ln -sf "zap-report-${TIMESTAMP}.html" "$REPORT_DIR/zap-report-latest.html"
ln -sf "zap-report-${TIMESTAMP}.json" "$REPORT_DIR/zap-report-latest.json"

exit ${SCAN_EXIT_CODE:-0}

