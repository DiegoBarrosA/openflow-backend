# Coding Standards

## Naming Conventions

### Classes
- **Controllers**: End with `Controller` (e.g., `BoardController`)
- **Services**: End with `Service` (e.g., `UserService`)
- **Repositories**: End with `Repository` (e.g., `BoardRepository`)
- **Models/Entities**: Singular noun, PascalCase (e.g., `User`, `Board`)
- **DTOs**: End with descriptive suffix (e.g., `AuthRequest`, `AuthResponse`)
- **Configuration**: End with `Config` (e.g., `SecurityConfig`)

### Methods
- **Controllers**: HTTP verb prefixes (e.g., `getBoard`, `createBoard`, `updateBoard`, `deleteBoard`)
- **Services**: Business action verbs (e.g., `findById`, `save`, `delete`)
- **Repositories**: Follow Spring Data conventions (e.g., `findByUserId`, `existsById`)

### Variables
- **Fields**: camelCase (e.g., `boardName`, `userId`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `JWT_SECRET`, `DEFAULT_EXPIRATION`)
- **Parameters**: camelCase (e.g., `boardId`, `statusId`)

### Packages
- Lowercase, no underscores
- Domain-based organization
- Avoid deep nesting (max 3-4 levels)

## Code Style

### Java Version
- Target Java 17
- Use modern Java features (records, pattern matching where appropriate)
- Prefer `var` for local variables when type is obvious

### Formatting
- 4 spaces for indentation
- 120 character line limit
- Blank lines between logical sections
- Consistent brace placement (K&R style)

### Annotations
- Group related annotations
- Place validation annotations on DTOs
- Use `@Transactional` on service methods
- Prefer constructor injection over field injection

### Exception Handling
- Use specific exception types
- Provide meaningful error messages
- Log exceptions appropriately
- Return appropriate HTTP status codes

## Best Practices

### Dependency Injection
- Use constructor injection
- Avoid `@Autowired` on fields
- Prefer `@RequiredArgsConstructor` from Lombok

### Transaction Management
- Declare transactions at service layer
- Use `@Transactional(readOnly = true)` for read operations
- Handle transaction boundaries explicitly

### Security
- Never expose sensitive data in responses
- Validate all user inputs
- Use parameterized queries (JPA handles this)
- Implement proper authentication checks

### Testing
- Write unit tests for services
- Write integration tests for controllers
- Mock external dependencies
- Test edge cases and error scenarios

## Code Organization

### File Structure
- One class per file
- Related classes in same package
- Keep packages focused and cohesive

### Method Length
- Prefer short, focused methods
- Extract complex logic into private methods
- Maximum 50 lines per method (guideline)

### Class Responsibilities
- Single Responsibility Principle
- Controllers handle HTTP concerns only
- Services contain business logic
- Repositories handle data access only

## Documentation

### JavaDoc
- Document public APIs
- Include parameter descriptions
- Document return values
- Note exceptions thrown

### Comments
- Explain "why", not "what"
- Keep comments up-to-date
- Remove commented-out code
- Use meaningful variable names instead of comments

## Dependencies

### Version Management
- Use Spring Boot parent POM for version management
- Pin specific versions for non-Spring dependencies
- Document version choices in `dependencies.md`

### Dependency Scope
- Use appropriate Maven scopes
- Avoid unnecessary dependencies
- Review dependencies regularly



