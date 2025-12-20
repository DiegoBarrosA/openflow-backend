# Security Testing - OpenFlow

This directory contains security testing configurations and scripts for OpenFlow.

## OWASP ZAP Security Scanning (SEC-01)

### Overview

OWASP ZAP (Zed Attack Proxy) is used to perform automated security scanning of the OpenFlow API. The scan identifies common web application vulnerabilities.

### Quick Start

1. **Start the application**:
   ```bash
   cd openflow-backend
   ./mvnw spring-boot:run
   ```

2. **Run the security scan**:
   ```bash
   ./security/zap-scan.sh
   ```

3. **Review the report**:
   Open `security/reports/zap-report-latest.html` in a browser.

### Scan Types

#### Baseline Scan (Default)
A passive scan that doesn't modify any data:
```bash
./security/zap-scan.sh http://localhost:8080
```

#### Full Scan (CI/CD)
For comprehensive scanning in CI/CD pipelines:
```bash
docker run --rm \
  -v $(pwd)/security/reports:/zap/wrk:rw \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-full-scan.py \
  -t http://localhost:8080 \
  -r zap-full-report.html
```

### CI/CD Integration

Add to your GitHub Actions workflow:

```yaml
security-scan:
  runs-on: ubuntu-latest
  needs: [build]
  steps:
    - uses: actions/checkout@v4
    
    - name: Start Application
      run: |
        cd openflow-backend
        ./mvnw spring-boot:run &
        sleep 30
        
    - name: OWASP ZAP Scan
      uses: zaproxy/action-baseline@v0.10.0
      with:
        target: 'http://localhost:8080'
        rules_file_name: 'security/zap-rules.tsv'
        
    - name: Upload Report
      uses: actions/upload-artifact@v3
      with:
        name: zap-report
        path: report_html.html
```

### Understanding Results

| Alert Level | Action Required |
|------------|-----------------|
| High | Critical - Fix immediately |
| Medium | Important - Plan to fix |
| Low | Review and assess risk |
| Informational | Best practice recommendations |

### Common Vulnerabilities Checked

- SQL Injection
- Cross-Site Scripting (XSS)
- Cross-Site Request Forgery (CSRF)
- Insecure Cookies
- Missing Security Headers
- Authentication Issues
- Information Disclosure

### Configuration

Edit `zap-config.yaml` to customize:
- Target URLs
- Excluded paths
- Scan duration
- Alert thresholds

### Troubleshooting

**Connection refused**:
Ensure the application is running and accessible.

**Scan takes too long**:
Reduce `maxDuration` and `maxScanDurationInMins` in the config.

**Too many false positives**:
Create a `zap-rules.tsv` file to ignore specific rules.

### Resources

- [OWASP ZAP Documentation](https://www.zaproxy.org/docs/)
- [ZAP Docker Images](https://www.zaproxy.org/docs/docker/)
- [GitHub Action](https://github.com/zaproxy/action-baseline)

