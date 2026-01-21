# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **multi-tenant, multi-region feature flag management system** with audit logging. The project uses a **monorepo structure** with separate backend (Spring Boot + Kotlin) and frontend (Vue 3) directories, but packages into a single deployable JAR for production.

**Technology Stack:**
- Backend: Kotlin 2.1.0, Spring Boot 3.4.0, PostgreSQL 15, Flyway migrations
- Frontend: Vue.js 3, Vite, Tailwind CSS, Axios
- Build: Gradle (backend), npm (frontend), Docker multi-stage build
- Testing: JUnit 5, MockK, TestContainers

## Build & Run Commands

### Backend Commands
```bash
# Run backend locally (requires PostgreSQL)
cd backend && ./gradlew bootRun

# Build backend (automatically builds frontend and bundles it)
cd backend && ./gradlew build

# Build without tests
cd backend && ./gradlew build -x test

# Run tests only
cd backend && ./gradlew test

# Run single test class
cd backend && ./gradlew test --tests "FeatureFlagServiceTest"

# Run single test method
cd backend && ./gradlew test --tests "FeatureFlagServiceTest.should create feature flag"

# Security vulnerability scan
cd backend && ./gradlew dependencyCheckAnalyze
```

### Frontend Commands
```bash
# Run frontend dev server (with hot reload)
cd frontend && npm run dev

# Build frontend for production
cd frontend && npm run build

# Install dependencies
cd frontend && npm install
```

### Docker Commands
```bash
# Build and run entire stack (PostgreSQL + app)
docker-compose up -d

# Build and run with pgAdmin
docker-compose --profile tools up -d

# Build Docker image
docker build -f docker/Dockerfile -t feature-flag:latest .

# Stop services
docker-compose down
```

### Monorepo Commands (from root)
```bash
# Run frontend dev
npm run dev:frontend

# Run backend dev
npm run dev:backend

# Build both
npm run build

# Docker commands
npm run docker:build
npm run docker:up
npm run docker:down
```

## Architecture & Key Concepts

### Monorepo Structure
- **`/backend/`** - Spring Boot backend with Gradle build
- **`/frontend/`** - Vue 3 SPA with Vite build
- **`/docker/`** - Dockerfile and docker-compose.yml
- **Root** - Monorepo scripts in package.json

**Build Integration:** The backend build.gradle.kts has custom tasks (`buildFrontend`, `copyFrontendDist`) that automatically build the frontend and copy `frontend/dist/` into `backend/src/main/resources/static/` during JAR creation. This allows deploying a single JAR containing both backend and frontend.

### Backend Architecture (Layered)

```
Controller → Service → Repository → Database
    ↓          ↓          ↓
   DTO      Business   JPA Entity
           Logic
```

**Key Packages:**
- `controller/` - REST API endpoints with validation
- `service/` - Business logic, transactions, rollout algorithms
- `repository/` - Spring Data JPA repositories
- `entity/` - JPA entities (FeatureFlag, Workspace, WorkspaceFeatureFlag, FeatureFlagAuditLog)
- `dto/` - Request/response objects with Jakarta Bean Validation
- `exception/` - Custom exceptions and global exception handler

**Important Services:**
- `FeatureFlagService` - Core feature flag CRUD and rollout logic
- `WorkspaceService` - Workspace management
- `AuditLogService` - Automatic audit logging for all operations

### Frontend Architecture (Vue 3 Composition API)

```
App.vue (main component)
   ├── components/ (reusable UI components)
   ├── composables/ (reusable logic hooks)
   │   ├── useApiCall - API call wrapper with loading/error states
   │   ├── useFormState - Form state management
   │   ├── useDebouncedSearch - Debounced search functionality
   │   └── useInfiniteScroll - Infinite scroll pagination
   ├── services/api-service.js (Axios API client)
   └── utils/ (helper functions)
```

**Frontend Build Output:** Vite builds to `frontend/dist/` which gets copied to backend static resources for production deployment.

### Database Schema

**Main Tables:**
- `workspaces` - Multi-tenant workspaces with region assignment (WESTEUROPE, EASTUS, etc.)
- `feature_flag` - Feature flag definitions with rollout percentage and target regions
- `workspace_feature_flag` - Many-to-many join table tracking which flags are enabled for which workspaces
- `feature_flag_audit_log` - Audit trail for all CREATE/UPDATE/DELETE operations

**Migrations:** Flyway SQL migrations in `src/main/resources/db/migration/` (V1-V5). Never modify existing migrations - always create new ones.

### Feature Flag Rollout Algorithm

This is the **core business logic** of the application. Located in `FeatureFlagService.kt`:

**Key Principle:** Deterministic, hash-based workspace selection ensuring:
1. Same workspace always gets same hash rank for a given feature flag
2. Increasing rollout percentage keeps previously enabled workspaces enabled
3. Decreasing rollout percentage disables only necessary workspaces
4. Region filtering happens before rollout calculation

**Algorithm (simplified):**
```kotlin
// 1. Filter workspaces by region
val targetWorkspaces = if (featureFlag.regions.contains("ALL")) {
    allWorkspaces
} else {
    allWorkspaces.filter { it.region in featureFlag.regions }
}

// 2. Sort deterministically by hash
val sortedWorkspaces = targetWorkspaces.sortedBy {
    abs((featureFlagId + workspaceId).hashCode())
}

// 3. Enable first N workspaces
val targetCount = (sortedWorkspaces.size * rolloutPercentage / 100.0).toInt()
sortedWorkspaces.take(targetCount).forEach { enableFlag(it) }
```

**Important:** Changing rollout from 30% → 50% enables additional workspaces while keeping the original 30% enabled. This is NOT random - the same workspaces are always selected for a given percentage.

### Multi-Region Support

**Workspace Regions:** Each workspace belongs to one Azure region (WESTEUROPE, EASTUS, CANADACENTRAL, etc.)

**Feature Flag Regions:** Each feature flag can target:
- `["ALL"]` - Global deployment to all workspaces
- `["WESTEUROPE"]` - Single region
- `["WESTEUROPE", "EASTUS", "UKSOUTH"]` - Multiple regions

**Region Filtering:** When querying feature flags for a workspace, only flags with matching regions (or "ALL") are returned.

### Audit Logging

**Automatic:** All CREATE, UPDATE, DELETE operations on feature flags are automatically logged by `AuditLogService`.

**Logged Data:**
- Operation type (CREATE/UPDATE/DELETE)
- Old values (JSON)
- New values (JSON)
- Team
- Timestamp
- Changed by (currently null - no auth implemented)

**Access:** Query via `/api/audit-logs` with filters for featureFlagId, team, operation.

## Development Workflow

### Local Development (Recommended)
1. Start PostgreSQL: `docker-compose up postgres -d`
2. Terminal 1 - Backend: `cd backend && ./gradlew bootRun`
3. Terminal 2 - Frontend: `cd frontend && npm run dev`
4. Access frontend dev server: http://localhost:5173 (proxies API to :8080)

### Production Build
```bash
cd backend && ./gradlew build
# This automatically:
# 1. Builds frontend (npm run build in frontend/)
# 2. Copies frontend/dist/ to backend/src/main/resources/static/
# 3. Creates JAR with both backend and frontend
```

### Testing Strategy
- **Unit tests:** Service layer business logic
- **Controller tests:** REST endpoint validation with MockMvc
- **Integration tests:** End-to-end with TestContainers (real PostgreSQL)
- **No frontend tests** currently implemented

## Important Implementation Details

### Feature Flag Creation Flow
1. Validate name uniqueness within team
2. Create feature flag entity
3. Create `WorkspaceFeatureFlag` associations for ALL workspaces (initially disabled)
4. Apply rollout percentage using deterministic algorithm
5. Log to audit log

### Feature Flag Update Flow
1. Validate feature flag exists
2. Store old values for audit
3. Update feature flag properties
4. Recalculate workspace associations based on new rollout percentage
5. Log changes to audit log with before/after values

### Manual Workspace Override
The `/api/feature-flags/{id}/workspaces` endpoint allows manual enable/disable for specific workspaces, bypassing the automatic rollout calculation. This is useful for:
- Testing in specific production workspaces
- VIP customer control
- Quick hotfixes

## Known Limitations & Considerations

1. **No Authentication:** No user authentication or authorization implemented. The `changedBy` field in audit logs is always null.
2. **Rollout is Workspace-Level:** Rollout percentages apply to workspaces, not individual users within workspaces.
3. **No Feature Flag Versioning:** No rollback capability - changes are immediate and permanent.
4. **No Scheduled Flags:** Cannot schedule enable/disable at specific times.

## API Testing

A complete **Bruno API collection** is available in `bruno-collection/` with all endpoints pre-configured. Use this for API testing instead of Postman/Insomnia.

## Environment Variables

**Backend:**
- `SPRING_DATASOURCE_URL` - PostgreSQL connection URL (default: `jdbc:postgresql://localhost:5432/feature-flag?currentSchema=public`)
- `SPRING_DATASOURCE_USERNAME` - Database user (default: `feature-flag`)
- `SPRING_DATASOURCE_PASSWORD` - Database password (default: `feature-flag123`)
- `SERVER_PORT` - Application port (default: `8080`)

**Frontend:**
The Vite dev server proxies `/api` requests to `http://localhost:8080` (configured in `frontend/vite.config.js`).
