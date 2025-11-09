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
- H2 Database (development)
- JWT (JJWT 0.12.3)

## API Base URL

- Development: `http://localhost:8080/api`
- See [API Documentation](docs/api.md) for endpoint details

## Default Users

- Username: `admin`, Password: `admin123`
- Username: `demo`, Password: `demo123`

## License

[Add license information]



