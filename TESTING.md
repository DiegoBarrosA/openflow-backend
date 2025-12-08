# Testing in Podman Containers

This document explains how to run tests for OpenFlow backend in Podman containers.

## Test Profile

Tests use a dedicated `test` profile that:
- Disables OAuth2 and Azure AD authentication
- Uses simplified security configuration
- Uses H2 in-memory database
- Allows all requests for testing purposes

The test profile is automatically activated when running tests via Maven or the test script.

## Prerequisites

- Podman installed and configured
- Rootless Podman setup (recommended)

## Running Tests

### Option 1: Using the Test Script (Recommended)

```bash
cd openflow-backend

# Run all tests
./podman-test.sh

# Run a specific test class
./podman-test.sh CommentServiceTest

# Run tests matching a pattern
./podman-test.sh "*Comment*Test"
```

### Option 2: Using Podman Compose

```bash
cd openflow-backend

# Build and run tests
podman compose -f docker-compose.test.yml up --build

# View test results
cat target/surefire-reports/*.txt
```

### Option 3: Using Podman Play Kube

```bash
cd openflow-backend

# First, build the test image
podman build -t openflow-backend-test:latest -f Dockerfile.test .

# Then run using podman play kube
podman play kube test-kube.yaml

# View logs
podman logs openflow-backend-test
```

### Option 4: Direct Podman Run

```bash
cd openflow-backend

# Build test image
podman build -t openflow-backend-test:latest -f Dockerfile.test .

# Run tests
podman run --rm \
  -v "$(pwd)/target/surefire-reports:/app/target/surefire-reports:Z" \
  -v "$(pwd)/target/site/jacoco:/app/target/site/jacoco:Z" \
  openflow-backend-test:latest \
  mvn test

# Run specific test
podman run --rm \
  -v "$(pwd)/target/surefire-reports:/app/target/surefire-reports:Z" \
  openflow-backend-test:latest \
  mvn test -Dtest=CommentServiceTest
```

## Test Results

After running tests, results are available in:

- **Test Reports**: `target/surefire-reports/`
- **Coverage Reports**: `target/site/jacoco/` (if JaCoCo is configured)

## Test Configuration

### Test Profile

Tests use the `test` profile which provides:
- Simplified security configuration (`TestSecurityConfig.java`)
- H2 in-memory database
- Disabled OAuth2 and Azure AD
- Permissive security for testing

### Configuration Files

- `src/test/resources/application-test.properties` - Test-specific properties
- `src/test/java/com/openflow/config/TestSecurityConfig.java` - Test security configuration

### Security in Tests

The test security configuration:
- Disables CSRF protection
- Allows all requests without authentication
- Uses stateless session management
- Provides CORS configuration for localhost

This allows tests to focus on business logic without complex authentication setup.

## Troubleshooting

### Permission Issues

If you encounter permission issues with volumes:

```bash
# Use :Z flag for SELinux (already included in scripts)
# Or disable SELinux for the volume mount
podman run --rm \
  -v "$(pwd)/target:/app/target:rw" \
  openflow-backend-test:latest mvn test
```

### Container Build Fails

```bash
# Clean and rebuild
podman rmi openflow-backend-test:latest
podman build --no-cache -t openflow-backend-test:latest -f Dockerfile.test .
```

### Tests Fail Due to Missing Dependencies

```bash
# Rebuild with fresh dependencies
podman build --no-cache -t openflow-backend-test:latest -f Dockerfile.test .
```

## CI/CD Integration

### GitHub Actions

Tests run automatically in GitHub Actions on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual workflow dispatch

#### Test Workflow

The `.github/workflows/test.yml` workflow:
1. Checks out code
2. Sets up Podman
3. Runs tests using `./podman-test.sh`
4. Uploads test results as artifacts
5. Publishes test results summary

#### Build Workflow

The `.github/workflows/build.yml` workflow:
1. Runs tests first (must pass)
2. Builds Docker image only if tests pass
3. Pushes image to GHCR
4. Triggers deployment workflow

### Local CI/CD Integration

For local CI/CD pipelines, use the test script:

```yaml
- name: Run Tests
  run: |
    cd openflow-backend
    ./podman-test.sh
```

### Docker Build Integration

Tests run automatically during Docker build:
- The `Dockerfile` runs `mvn clean package` (without `-DskipTests`)
- Build fails if any test fails
- Ensures only tested code is deployed

## Test Coverage

To generate coverage reports:

```bash
podman run --rm \
  -v "$(pwd)/target:/app/target:Z" \
  openflow-backend-test:latest \
  mvn test jacoco:report
```

View coverage at: `target/site/jacoco/index.html`

