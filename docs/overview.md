# OpenFlow Backend Overview

## Project Summary

OpenFlow Backend is a RESTful API service built with Spring Boot that provides the core functionality for a Trello-like project management application. It handles user authentication, board management, status configuration, and task operations.

## Goals

- Provide a secure, scalable REST API for project management operations
- Implement JWT-based authentication and authorization
- Support CRUD operations for boards, statuses, and tasks
- Maintain data integrity and user isolation
- Enable easy integration with frontend applications

## Key Features

### Authentication & Authorization
- User registration with email validation
- JWT-based authentication
- Password encryption using BCrypt
- Role-based access control (prepared for future expansion)

### Board Management
- Create, read, update, and delete boards
- User-specific board ownership
- Board descriptions and metadata

### Status Management
- Configurable status columns per board
- Customizable status colors
- Order management for status display
- Status deletion with cascade handling

### Task Management
- Full CRUD operations for tasks
- Task assignment to status columns
- Task descriptions and metadata
- Task movement between statuses

### Database
- H2 in-memory database for development
- Spring Data JPA for data persistence
- Prepared for PostgreSQL/MySQL migration

## Technology Stack

- **Java 17**: Modern Java features and performance
- **Spring Boot 3.2.0**: Rapid application development framework
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Data persistence abstraction
- **H2 Database**: In-memory database for development
- **JWT (JJWT 0.12.3)**: Token-based authentication
- **Maven**: Build and dependency management

## Architecture Principles

- RESTful API design
- Layered architecture (Controller → Service → Repository)
- Dependency injection for loose coupling
- Configuration-driven behavior
- Container-ready deployment



