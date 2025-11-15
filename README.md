# OpenFlow Backend

RESTful API service for OpenFlow, a Trello-like project management application.

## Quick Start

### Containerized Deployment

```bash
podman build -t openflow-backend:latest .
podman run -d -p 8080:8080 openflow-backend:latest
```

### Local Development

```bash
mvn spring-boot:run
```

### Cloud Deployment (AWS EKS + GHCR)

#### Prerequisites

1. **AWS Account** with EKS permissions and free tier eligibility
2. **AWS CLI** configured with your credentials
3. **eksctl** installed (see setup script below)
4. **GitHub Repository** with Actions enabled

#### Setup AWS EKS Cluster

Run the automated setup script:

```bash
chmod +x ../setup-aws-eks.sh
../setup-aws-eks.sh
```

This creates a free-tier EKS cluster with:

- 1 t3.micro node (750 free hours/month)
- Managed node group
- OIDC provider for IAM roles

#### Configure GitHub Secrets

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

**AWS Credentials:**

- `AWS_ACCESS_KEY_ID` - Your AWS access key
- `AWS_SECRET_ACCESS_KEY` - Your AWS secret key

**Oracle Database:**

- `ORACLE_DB_USERNAME` - Oracle ADB username
- `ORACLE_DB_PASSWORD` - Oracle ADB password
- `ORACLE_DB_URL` - Oracle ADB connection URL

**Application Secrets:**

- `JWT_SECRET` - JWT signing secret (generate a secure random string)

**Oracle Wallet Files (Base64 encoded):**

- `ORACLE_WALLET_CWALLET` - Base64 encoded cwallet.sso
- `ORACLE_WALLET_EWALLET` - Base64 encoded ewallet.p12
- `ORACLE_WALLET_KEYSTORE` - Base64 encoded keystore.jks
- `ORACLE_WALLET_OJDBC` - Base64 encoded ojdbc.properties
- `ORACLE_WALLET_SQLNET` - Base64 encoded sqlnet.ora
- `ORACLE_WALLET_TNSNAMES` - Base64 encoded tnsnames.ora
- `ORACLE_WALLET_TRUSTSTORE` - Base64 encoded truststore.jks

**To encode wallet files:**

```bash
# For each wallet file
base64 -w 0 cwallet.sso > cwallet.sso.b64
```

#### Deploy to EKS

1. Push your code to GitHub
2. The GitHub Actions workflow (`.github/workflows/deploy-oracle.yml`) will automatically:
   - Build container images and push to GitHub Container Registry (GHCR)
   - Create Kubernetes secrets from GitHub secrets
   - Deploy to your EKS cluster with image pull secrets for GHCR access

#### Monitor Deployment

```bash
# Check cluster status
kubectl get nodes
kubectl get pods
kubectl get services

# View application logs
kubectl logs -l app=openflow-backend

# Access the application
kubectl get svc openflow-backend
```

## Documentation

Comprehensive documentation is available in the `/docs` directory:

- [Overview](docs/overview.md) - Project summary, goals, and features
- [Architecture](docs/architecture.md) - File structure and design patterns
- [Coding Standards](docs/coding-standards.md) - Naming conventions and best practices
- [Dependencies](docs/dependencies.md) - Dependency list with versions
- [Installation](docs/installation.md) - Setup and installation instructions
- [Testing](docs/testing.md) - Testing framework and guidelines
- [API Documentation](docs/api.md) - REST API endpoints and usage
- [Workflows](docs/workflows.md) - Common tasks and development workflows
- [Glossary](docs/glossary.md) - Terms and acronyms

## Features

- JWT-based authentication
- Board management (CRUD)
- Configurable status columns
- Task management with drag-and-drop support
- User isolation and data security

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Spring Security
- Spring Data JPA
- Oracle Autonomous Database
- JWT (JJWT 0.12.3)
- AWS EKS (Kubernetes)
- GitHub Container Registry (GHCR)

## API Base URL

- Development: `http://localhost:8080/api`
- Production: `http://<eks-service-url>/api`
- See [API Documentation](docs/api.md) for endpoint details

## Default Users

- Username: `admin`, Password: `admin123`
- Username: `demo`, Password: `demo123`

## License

[Add license information]



