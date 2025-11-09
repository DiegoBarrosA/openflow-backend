# Installation Guide

## Prerequisites

### Required Software
- **Java 17+**: JDK or JRE (Eclipse Temurin recommended)
- **Maven 3.9+**: Build tool (or use Maven Wrapper)
- **Podman**: Container runtime (for containerized deployment)
- **Git**: Version control (optional, for cloning)

### System Requirements
- **Operating System**: Linux, macOS, or Windows
- **Memory**: Minimum 512MB RAM
- **Disk Space**: ~500MB for application and dependencies
- **Network**: Internet access for Maven dependencies

## Installation Methods

### Method 1: Containerized Deployment (Recommended)

#### Step 1: Build Docker Image
```bash
cd openflow-backend
podman build -t openflow-backend:latest .
```

#### Step 2: Run Container
```bash
podman run -d \
  --name openflow-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:h2:mem:openflowdb \
  -e JWT_SECRET=your-secret-key \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  openflow-backend:latest
```

#### Step 3: Verify Installation
```bash
curl http://localhost:8080/api/auth/login
```

### Method 2: Local Development

#### Step 1: Clone Repository
```bash
git clone https://github.com/DiegoBarrosA/openflow-backend.git
cd openflow-backend
```

#### Step 2: Build Application
```bash
mvn clean package
```

#### Step 3: Run Application
```bash
mvn spring-boot:run
```

Or using the JAR:
```bash
java -jar target/trello-backend-1.0.0.jar
```

#### Step 4: Verify Installation
- Application starts on `http://localhost:8080`
- H2 Console available at `http://localhost:8080/h2-console`

## Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SPRING_DATASOURCE_URL` | Database JDBC URL | `jdbc:h2:mem:openflowdb` | No |
| `JWT_SECRET` | Secret key for JWT tokens | `openflow-secret-key` | Yes (production) |
| `JWT_EXPIRATION` | Token expiration in milliseconds | `86400000` (24h) | No |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000` | No |
| `SERVER_PORT` | Server port | `8080` | No |

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:h2:mem:openflowdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JWT Configuration
jwt.secret=your-secret-key-change-in-production
jwt.expiration=86400000

# CORS Configuration
cors.allowed.origins=http://localhost:3000
```

## Database Setup

### H2 (Development)
No setup required - in-memory database starts automatically.

### PostgreSQL (Production)
1. Install PostgreSQL
2. Create database:
```sql
CREATE DATABASE openflow;
CREATE USER openflow_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE openflow TO openflow_user;
```
3. Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/openflow
spring.datasource.username=openflow_user
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### MySQL (Production)
1. Install MySQL
2. Create database and user
3. Update `application.properties` accordingly

## Verification

### Health Check
```bash
curl http://localhost:8080/api/auth/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Expected response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin"
}
```

### H2 Console Access
1. Navigate to `http://localhost:8080/h2-console`
2. JDBC URL: `jdbc:h2:mem:openflowdb`
3. Username: `sa`
4. Password: (empty)

## Troubleshooting

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Change port in application.properties
server.port=8081
```

### Maven Build Fails
```bash
# Clean and rebuild
mvn clean install

# Skip tests
mvn clean package -DskipTests
```

### Container Issues
```bash
# Check container logs
podman logs openflow-backend

# Check container status
podman ps -a

# Remove and recreate
podman rm -f openflow-backend
podman build -t openflow-backend:latest .
```

## Next Steps

After installation:
1. Review [API Documentation](api.md)
2. Check [Testing Guide](testing.md)
3. Read [Workflows](workflows.md)
4. Configure production database
5. Set secure JWT secret

