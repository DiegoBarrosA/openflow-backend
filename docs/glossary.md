# Glossary

## Terms and Acronyms

### A

**API (Application Programming Interface)**
- Set of protocols and tools for building software applications. In this project, REST API endpoints.

**Authentication**
- Process of verifying user identity. Implemented using JWT tokens.

**Authorization**
- Process of determining user permissions. Currently all authenticated users have equal access.

### B

**BCrypt**
- Password hashing algorithm used for secure password storage.

**Board**
- Main container for organizing tasks. Users can create multiple boards.

### C

**CORS (Cross-Origin Resource Sharing)**
- Mechanism allowing web pages to make requests to different domains. Configured to allow frontend access.

**Controller**
- Spring MVC component handling HTTP requests and responses. Located in `com.openflow.controller`.

**CRUD**
- Create, Read, Update, Delete operations. Basic operations for data management.

### D

**DTO (Data Transfer Object)**
- Objects used to transfer data between layers. Located in `com.openflow.dto`.

**DDL (Data Definition Language)**
- SQL commands for defining database structure. Hibernate generates DDL automatically.

### E

**Entity**
- JPA annotation marking a class as a database entity. Models are entities.

### H

**H2**
- In-memory database used for development. Data is lost on application restart.

**Hibernate**
- JPA implementation used by Spring Data JPA for ORM (Object-Relational Mapping).

### J

**JPA (Java Persistence API)**
- Java specification for managing relational data. Implemented via Spring Data JPA.

**JWT (JSON Web Token)**
- Compact token format for securely transmitting information. Used for authentication.

**JJWT**
- Java library for creating and validating JWT tokens.

### L

**Lombok**
- Java library reducing boilerplate code through annotations.

### M

**Maven**
- Build automation and dependency management tool.

**Model**
- Entity classes representing database tables. Located in `com.openflow.model`.

### R

**Repository**
- Spring Data interface for data access. Located in `com.openflow.repository`.

**REST (Representational State Transfer)**
- Architectural style for web services. API follows REST principles.

### S

**Service**
- Business logic layer. Located in `com.openflow.service`.

**Spring Boot**
- Framework simplifying Spring application development.

**Spring Data JPA**
- Spring module simplifying data access with JPA.

**Spring Security**
- Framework providing authentication and authorization.

**Status**
- Column in a board representing task state (e.g., "To Do", "In Progress", "Done").

### T

**Task**
- Individual work item within a board. Belongs to a status and board.

**Transaction**
- Database operation ensuring atomicity. Managed by Spring `@Transactional`.

### U

**User**
- Application user with authentication credentials. Owns boards and associated resources.

## Domain-Specific Terms

**OpenFlow**
- Project name for the Trello-like application.

**Board Owner**
- User who created a board. Has full access to board and its resources.

**Status Order**
- Numeric value determining display order of status columns.

**Task Status**
- Current status column a task belongs to. Tasks can be moved between statuses.

