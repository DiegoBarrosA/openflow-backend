# Testing Framework and Guidelines

## Testing Strategy

Tests run in Podman containers to ensure consistency and isolation. All tests execute in a containerized environment without requiring native executables or package manager installations.

## Test Structure

```
src/test/java/com/openflow/
├── controller/          # Controller integration tests
├── service/             # Service unit tests
├── repository/          # Repository integration tests
└── config/             # Configuration tests
```

## Testing Framework

### Core Testing Dependencies
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **Spring Boot Test**: Integration testing support
- **AssertJ**: Fluent assertions
- **Testcontainers**: Container-based testing (optional)

### Test Types

#### Unit Tests
- Test individual components in isolation
- Mock dependencies
- Fast execution
- High coverage

#### Integration Tests
- Test component interactions
- Use test database (H2)
- Test REST endpoints
- Verify database operations

#### Container Tests
- Run in Podman containers
- Test full application stack
- Verify container configuration
- End-to-end scenarios

## Running Tests

### Local Development (Maven)
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report
```

### Containerized Testing
```bash
# Build test image
podman build -t openflow-backend-test:latest -f Dockerfile.test .

# Run tests in container
podman run --rm openflow-backend-test:latest mvn test
```

## Test Examples

### Service Unit Test
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private JwtService jwtService;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testRegisterUser() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        
        // When
        AuthResponse response = userService.register(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }
}
```

### Controller Integration Test
```java
@SpringBootTest
@AutoConfigureMockMvc
class BoardControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    private String authToken;
    
    @BeforeEach
    void setUp() {
        // Create test user and get token
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.save(user);
        
        authToken = jwtUtil.generateToken("testuser");
    }
    
    @Test
    void testCreateBoard() throws Exception {
        mockMvc.perform(post("/api/boards")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Board\",\"description\":\"Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Board"));
    }
}
```

## Test Configuration

### Application Properties for Testing
Create `src/test/resources/application-test.properties`:

```properties
# Test Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop

# Disable security for unit tests (if needed)
spring.security.enabled=false

# JWT Test Secret
jwt.secret=test-secret-key
```

## Best Practices

### Test Naming
- Use descriptive test method names
- Follow pattern: `test[MethodName][Scenario]`
- Example: `testRegisterUserWithDuplicateUsername`

### Test Organization
- One test class per production class
- Group related tests
- Use `@Nested` for test organization

### Assertions
- Use AssertJ for fluent assertions
- One logical assertion per test
- Provide meaningful failure messages

### Mocks
- Mock external dependencies
- Verify interactions when necessary
- Use `@Mock` and `@InjectMocks`

### Test Data
- Use builders or factories for test data
- Keep test data minimal and focused
- Avoid shared mutable state

## Coverage Goals

- **Unit Tests**: 80%+ coverage for services
- **Integration Tests**: Critical paths covered
- **Controller Tests**: All endpoints tested

## Continuous Integration

Tests run automatically in CI/CD pipeline:
- On every commit
- Before deployment
- In containerized environment
- With coverage reporting

## Troubleshooting

### Tests Fail in Container
- Check container logs
- Verify test database configuration
- Ensure network connectivity

### Slow Test Execution
- Use `@DirtiesContext` sparingly
- Mock external services
- Use test slices (`@WebMvcTest`, `@DataJpaTest`)

