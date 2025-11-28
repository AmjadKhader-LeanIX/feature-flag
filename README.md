# Feature Flag Management Service

A complete feature flag management service built with Kotlin, Spring Boot, and PostgreSQL, providing multi-tenant workspace support, customer-specific overrides, and rollout percentage control.

## Technology Stack

- **Language & Runtime**: Kotlin 1.8.21, Java 17
- **Framework**: Spring Boot 3.1.0
- **Database**: PostgreSQL 15 with Flyway migrations
- **Build Tool**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, MockK, TestContainers
- **Code Quality**: Ktlint, OWASP Dependency Check
- **Containerization**: Docker & Docker Compose

## Features

### Core Functionality
- **Workspace Management**: Multi-tenant architecture with workspace isolation
- **User Management**: Email-based users with active/inactive status
- **Feature Flag Management**: Create, update, delete feature flags with rollout percentages
- **Customer Overrides**: Customer-specific feature flag states that override rollout percentages
- **Feature Evaluation**: Consistent hash-based evaluation with customer override support

### API Endpoints

#### Workspaces
- `GET /api/workspaces` - List all workspaces
- `POST /api/workspaces` - Create workspace
- `GET /api/workspaces/{id}` - Get workspace by ID
- `PUT /api/workspaces/{id}` - Update workspace
- `DELETE /api/workspaces/{id}` - Delete workspace
- `GET /api/workspaces/search?name={name}` - Search workspaces

#### Users
- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `GET /api/users/workspace/{workspaceId}` - Get users by workspace
- `GET /api/users/workspace/{workspaceId}/active` - Get active users

#### Feature Flags
- `GET /api/feature-flags` - List all feature flags
- `POST /api/feature-flags` - Create feature flag
- `GET /api/feature-flags/{id}` - Get feature flag by ID
- `PUT /api/feature-flags/{id}` - Update feature flag
- `DELETE /api/feature-flags/{id}` - Delete feature flag
- `GET /api/feature-flags/workspace/{workspaceId}` - Get feature flags by workspace
- `GET /api/feature-flags/search?name={name}` - Search feature flags

#### Customer Feature Flags
- `GET /api/customer-feature-flags` - List all customer overrides
- `POST /api/customer-feature-flags` - Create customer override
- `GET /api/customer-feature-flags/{id}` - Get customer override by ID
- `PUT /api/customer-feature-flags/{id}` - Update customer override
- `DELETE /api/customer-feature-flags/{id}` - Delete customer override
- `GET /api/customer-feature-flags/customer/{customerId}` - Get overrides by customer
- `GET /api/customer-feature-flags/workspace/{workspaceId}` - Get overrides by workspace

## Database Schema

The service uses PostgreSQL with the following main tables:

- `workspaces` - Multi-tenant workspaces
- `feature_flag` - Feature flag definitions with rollout percentages

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 17 (for local development)

### Running with Docker Compose

1. **Clone and build the application:**
```bash
./gradlew clean build
```

2. **Start the complete stack:**
```bash
docker-compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- Feature Flag application (port 8080)

3. **Optional: Start with pgAdmin:**
```bash
docker-compose --profile tools up -d
```

Access pgAdmin at http://localhost:5050 (admin@featureflag.com / admin123)

### Local Development

1. **Start PostgreSQL:**
```bash
docker-compose up postgres -d
```

2. **Run the application:**
```bash
./gradlew bootRun
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/feature-flag?currentSchema=public` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `feature-flag` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `feature-flag123` | Database password |
| `SERVER_PORT` | `8080` | Application port |

## API Usage Examples

### Create a Workspace
```bash
curl -X POST http://localhost:8080/api/workspaces \
  -H "Content-Type: application/json" \
  -d '{"name": "My Workspace"}'
```

### Create a Feature Flag
```bash
curl -X POST http://localhost:8080/api/feature-flags \
  -H "Content-Type: application/json" \
  -d '{
    "name": "new-checkout-flow",
    "description": "Enable new checkout flow",
    "workspaceId": "your-workspace-id",
    "rolloutPercentage": 25
  }'
```

### Evaluate a Feature Flag
```bash
curl "http://localhost:8080/api/feature-flags/{feature-flag-id}/check?customerId={customer-id}"
```

Response:
```json
{
  "enabled": true,
  "reason": "Enabled by rollout percentage (25%)"
}
```

### Create Customer Override
```bash
curl -X POST http://localhost:8080/api/customer-feature-flags \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-uuid",
    "featureFlagId": "feature-flag-uuid",
    "isEnabled": true
  }'
```

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Integration Tests
```bash
./gradlew integrationTest
```

### Code Quality Checks
```bash
# Kotlin linting
./gradlew ktlintCheck

# Security vulnerability check
./gradlew dependencyCheckAnalyze
```

## Monitoring

The application includes Spring Boot Actuator endpoints:

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Info: `GET /actuator/info`

## Architecture

The application follows a layered architecture:

```
Controller → Service → Repository → Database
```

- **Controllers**: REST API endpoints with validation
- **Services**: Business logic and transaction management
- **Repositories**: Data access with Spring Data JPA
- **Entities**: JPA entities with proper relationships
- **DTOs**: Data transfer objects for API communication
- **Exceptions**: Global exception handling with proper HTTP responses

## Feature Flag Evaluation Logic

The service uses a consistent hash-based approach for feature flag evaluation:

1. **Customer Override Check**: First checks if there's a customer-specific override
2. **Rollout Percentage**: If no override, uses consistent hashing based on customer ID
3. **Consistency**: Same customer ID always gets the same result for the same rollout percentage

## Development

### Code Style
The project uses Ktlint for Kotlin code formatting. Configuration is in `.editorconfig`.

### Database Migrations
Database schema changes are managed with Flyway migrations in `src/main/resources/db/migration/`.

### Adding New Features
1. Create/update entities
2. Add repository methods
3. Implement service logic
4. Create REST endpoints
5. Add tests
6. Update documentation

## Contributing

1. Follow the existing code style
2. Add tests for new functionality
3. Update documentation
4. Ensure all quality checks pass

## License


