#!/bin/bash
# =============================================================================
# JMeter Performance Test Runner for OpenFlow
# =============================================================================
# This script runs JMeter load tests against the OpenFlow API.
# 
# Usage:
#   ./run-perf-tests.sh [test_type] [target_url]
#
# Test Types:
#   smoke    - Quick validation (10 users, 1 minute)
#   load     - Normal load (50 users, 5 minutes)
#   stress   - High load (100 users, 10 minutes)
#   spike    - Spike test (200 users, 15 minutes)
#
# Examples:
#   ./run-perf-tests.sh smoke                    # Smoke test on localhost
#   ./run-perf-tests.sh load http://staging.com  # Load test on staging
#
# Requirements:
#   - Docker or Podman installed
#   - Target application running
#
# Output:
#   - JTL results: reports/results-{timestamp}.jtl
#   - HTML report: reports/report-{timestamp}/
# =============================================================================

set -e

# Configuration
TEST_TYPE="${1:-smoke}"
TARGET_URL="${2:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="$SCRIPT_DIR/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
CONTAINER_RUNTIME="${CONTAINER_RUNTIME:-docker}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configurations
case "$TEST_TYPE" in
    smoke)
        USERS=10
        RAMP_UP=10
        DURATION=60
        echo -e "${BLUE}Running SMOKE test: 10 users, 1 minute${NC}"
        ;;
    load)
        USERS=50
        RAMP_UP=30
        DURATION=300
        echo -e "${BLUE}Running LOAD test: 50 users, 5 minutes${NC}"
        ;;
    stress)
        USERS=100
        RAMP_UP=60
        DURATION=600
        echo -e "${BLUE}Running STRESS test: 100 users, 10 minutes${NC}"
        ;;
    spike)
        USERS=200
        RAMP_UP=30
        DURATION=900
        echo -e "${BLUE}Running SPIKE test: 200 users, 15 minutes${NC}"
        ;;
    *)
        echo -e "${RED}Unknown test type: $TEST_TYPE${NC}"
        echo "Available types: smoke, load, stress, spike"
        exit 1
        ;;
esac

echo -e "${GREEN}=== OpenFlow Performance Test ===${NC}"
echo "Test Type: $TEST_TYPE"
echo "Target: $TARGET_URL"
echo "Users: $USERS"
echo "Ramp-up: ${RAMP_UP}s"
echo "Duration: ${DURATION}s"
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
if ! curl -s --head --connect-timeout 5 "$TARGET_URL" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Target may not be reachable. Continuing anyway...${NC}"
fi

# Generate JMeter test plan dynamically
TEST_PLAN_FILE="$SCRIPT_DIR/openflow-test-${TEST_TYPE}.jmx"
cat > "$TEST_PLAN_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="OpenFlow Performance Test" enabled="true">
      <stringProp name="TestPlan.comments">Performance test for OpenFlow API</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
    </TestPlan>
    <hashTree>
      <Arguments guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">${TARGET_URL}</stringProp>
          </elementProp>
        </collectionProp>
      </Arguments>
      <hashTree/>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="API Users" enabled="true">
        <stringProp name="ThreadGroup.num_threads">${USERS}</stringProp>
        <stringProp name="ThreadGroup.ramp_time">${RAMP_UP}</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
        <stringProp name="ThreadGroup.duration">${DURATION}</stringProp>
        <stringProp name="ThreadGroup.delay">0</stringProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET Public Boards" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments"/>
          </elementProp>
          <stringProp name="HTTPSampler.domain">\${__P(host,localhost)}</stringProp>
          <stringProp name="HTTPSampler.port">\${__P(port,8080)}</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.path">/api/public/boards</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
        </HTTPSamplerProxy>
        <hashTree/>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="POST Login" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">{"username":"perftest","password":"perftest123"}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="HTTPSampler.domain">\${__P(host,localhost)}</stringProp>
          <stringProp name="HTTPSampler.port">\${__P(port,8080)}</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.path">/api/auth/login</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Headers" enabled="true">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="Content-Type" elementType="Header">
                <stringProp name="Header.name">Content-Type</stringProp>
                <stringProp name="Header.value">application/json</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
          <hashTree/>
        </hashTree>
        <ConstantTimer guiclass="ConstantTimerGui" testclass="ConstantTimer" testname="Think Time" enabled="true">
          <stringProp name="ConstantTimer.delay">1000</stringProp>
        </ConstantTimer>
        <hashTree/>
      </hashTree>
      <ResultCollector guiclass="SummaryReport" testclass="ResultCollector" testname="Summary Report" enabled="true">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
        <objProp>
          <name>saveConfig</name>
          <value class="SampleSaveConfiguration">
            <time>true</time>
            <latency>true</latency>
            <timestamp>true</timestamp>
            <success>true</success>
            <label>true</label>
            <code>true</code>
            <message>true</message>
            <threadName>true</threadName>
            <dataType>true</dataType>
            <encoding>false</encoding>
            <assertions>true</assertions>
            <subresults>true</subresults>
            <responseData>false</responseData>
            <samplerData>false</samplerData>
            <xml>false</xml>
            <fieldNames>true</fieldNames>
            <responseHeaders>false</responseHeaders>
            <requestHeaders>false</requestHeaders>
            <responseDataOnError>false</responseDataOnError>
            <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
            <assertionsResultsToSave>0</assertionsResultsToSave>
            <bytes>true</bytes>
            <sentBytes>true</sentBytes>
            <url>true</url>
            <threadCounts>true</threadCounts>
            <idleTime>true</idleTime>
            <connectTime>true</connectTime>
          </value>
        </objProp>
        <stringProp name="filename"></stringProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
EOF

# Run JMeter in Docker
echo -e "${YELLOW}Starting JMeter test...${NC}"
echo "This may take several minutes..."
echo ""

RESULT_FILE="results-${TEST_TYPE}-${TIMESTAMP}.jtl"
REPORT_FOLDER="report-${TEST_TYPE}-${TIMESTAMP}"

$CONTAINER_RUNTIME run --rm \
    -v "$SCRIPT_DIR:/test:rw" \
    -v "$REPORT_DIR:/reports:rw" \
    --network host \
    justb4/jmeter:latest \
    -n \
    -t "/test/openflow-test-${TEST_TYPE}.jmx" \
    -l "/reports/${RESULT_FILE}" \
    -e \
    -o "/reports/${REPORT_FOLDER}" \
    -Jhost=localhost \
    -Jport=8080 \
    || TEST_EXIT_CODE=$?

# Clean up generated test plan
rm -f "$TEST_PLAN_FILE"

# Display results
echo ""
if [ "${TEST_EXIT_CODE:-0}" -eq 0 ]; then
    echo -e "${GREEN}=== Test completed successfully ===${NC}"
else
    echo -e "${YELLOW}=== Test completed with warnings ===${NC}"
fi

echo ""
echo "Results:"
echo "  - JTL file: $REPORT_DIR/${RESULT_FILE}"
echo "  - HTML report: $REPORT_DIR/${REPORT_FOLDER}/index.html"
echo ""

# Create symlinks to latest results
ln -sf "${RESULT_FILE}" "$REPORT_DIR/results-latest.jtl"
ln -sfn "${REPORT_FOLDER}" "$REPORT_DIR/report-latest"

# Print summary if results file exists
if [ -f "$REPORT_DIR/${RESULT_FILE}" ]; then
    echo -e "${BLUE}=== Quick Summary ===${NC}"
    TOTAL_SAMPLES=$(tail -n +2 "$REPORT_DIR/${RESULT_FILE}" | wc -l)
    ERROR_COUNT=$(tail -n +2 "$REPORT_DIR/${RESULT_FILE}" | grep -c "false" || echo "0")
    echo "Total Samples: $TOTAL_SAMPLES"
    echo "Errors: $ERROR_COUNT"
    if [ "$TOTAL_SAMPLES" -gt 0 ]; then
        ERROR_RATE=$(echo "scale=2; $ERROR_COUNT * 100 / $TOTAL_SAMPLES" | bc)
        echo "Error Rate: ${ERROR_RATE}%"
    fi
fi

exit ${TEST_EXIT_CODE:-0}

