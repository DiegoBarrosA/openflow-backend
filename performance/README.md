# Performance Testing - OpenFlow

This directory contains performance testing configurations and scripts for OpenFlow.

## JMeter Load Testing

### Overview

Apache JMeter is used to perform load, stress, and spike testing of the OpenFlow API to ensure it meets performance requirements.

### Performance Criteria

Based on the test plan:

| Scenario | Users | Duration | Target Response Time |
|----------|-------|----------|---------------------|
| Normal Load | 50 | 5 min | < 200ms |
| High Load | 100 | 10 min | < 500ms |
| Stress | 200 | 15 min | No 5xx errors |

### Quick Start

1. **Start the application**:
   ```bash
   cd openflow-backend
   ./mvnw spring-boot:run
   ```

2. **Create a test user** (for authenticated endpoints):
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"perftest","email":"perf@test.com","password":"perftest123"}'
   ```

3. **Run performance tests**:
   ```bash
   # Smoke test (quick validation)
   ./performance/run-perf-tests.sh smoke
   
   # Load test (normal conditions)
   ./performance/run-perf-tests.sh load
   
   # Stress test (high load)
   ./performance/run-perf-tests.sh stress
   
   # Spike test (sudden traffic surge)
   ./performance/run-perf-tests.sh spike
   ```

4. **Review the results**:
   Open `performance/reports/report-latest/index.html` in a browser.

### Test Types

#### Smoke Test
Quick validation that the system works under minimal load.
- Users: 10
- Duration: 1 minute
- Use case: Pre-deployment sanity check

#### Load Test
Simulate expected normal load conditions.
- Users: 50
- Duration: 5 minutes
- Use case: Baseline performance metrics

#### Stress Test
Push the system beyond normal load.
- Users: 100
- Duration: 10 minutes
- Use case: Find breaking points

#### Spike Test
Simulate sudden traffic surge.
- Users: 200
- Duration: 15 minutes
- Use case: Test auto-scaling, recovery

### Endpoints Tested

1. **GET /api/public/boards** - Public endpoint (no auth)
2. **POST /api/auth/login** - Authentication endpoint

### CI/CD Integration

Add to your GitHub Actions workflow:

```yaml
performance-test:
  runs-on: ubuntu-latest
  needs: [build]
  steps:
    - uses: actions/checkout@v4
    
    - name: Start Application
      run: |
        cd openflow-backend
        ./mvnw spring-boot:run &
        sleep 30
        
    - name: Run Load Test
      run: ./openflow-backend/performance/run-perf-tests.sh load
      
    - name: Upload Results
      uses: actions/upload-artifact@v3
      with:
        name: jmeter-report
        path: openflow-backend/performance/reports/report-latest/
```

### Understanding Results

Key metrics to monitor:

| Metric | Target | Description |
|--------|--------|-------------|
| Average Response Time | < 200ms | Mean response time |
| 90th Percentile | < 500ms | 90% of requests complete within |
| 95th Percentile | < 1000ms | 95% of requests complete within |
| Throughput | > 100 req/s | Requests per second |
| Error Rate | < 1% | Percentage of failed requests |

### Custom Test Plans

To create custom test plans:

1. Download [Apache JMeter](https://jmeter.apache.org/)
2. Create/modify test plan in GUI mode
3. Save as `.jmx` file in this directory
4. Run with:
   ```bash
   docker run --rm \
     -v $(pwd)/performance:/test:rw \
     --network host \
     justb4/jmeter:latest \
     -n -t /test/custom-test.jmx \
     -l /test/reports/results.jtl \
     -e -o /test/reports/report/
   ```

### Troubleshooting

**Connection refused**:
Ensure the application is running on the expected port.

**OutOfMemoryError**:
Increase JMeter heap size:
```bash
export JVM_ARGS="-Xms512m -Xmx2g"
```

**Results file not created**:
Check container permissions and volume mounts.

### Resources

- [Apache JMeter Documentation](https://jmeter.apache.org/usermanual/index.html)
- [JMeter Docker Image](https://hub.docker.com/r/justb4/jmeter)
- [JMeter Best Practices](https://jmeter.apache.org/usermanual/best-practices.html)

