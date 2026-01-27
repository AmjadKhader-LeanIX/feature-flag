# Technical Specification Document
## Multi-Tenant Feature Flag Management System

**Document Version:** 1.0
**Last Updated:** 2026-01-27
**Status:** Implemented

---

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Technology Stack](#technology-stack)
3. [Data Model](#data-model)
4. [Core Algorithms](#core-algorithms)
5. [API Specification](#api-specification)
6. [Frontend Architecture](#frontend-architecture)
7. [Build and Deployment](#build-and-deployment)
8. [Testing Strategy](#testing-strategy)
9. [Performance Considerations](#performance-considerations)
10. [Security Considerations](#security-considerations)

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Client Browser                       │
│                   (Vue.js 3 SPA)                        │
└─────────────────────────────────────────────────────────┘
                           │
                           │ HTTP/REST
                           ▼
┌─────────────────────────────────────────────────────────┐
│               Spring Boot Application                    │
│  ┌───────────────────────────────────────────────────┐ │
│  │         Controller Layer (REST API)                │ │
│  │  - Input validation (Jakarta Bean Validation)     │ │
│  │  - HTTP status code mapping                       │ │
│  │  - Request/Response DTOs                          │ │
│  └───────────────────────────────────────────────────┘ │
│                           │                              │
│  ┌───────────────────────────────────────────────────┐ │
│  │           Service Layer                            │ │
│  │  - Business logic                                  │ │
│  │  - Transaction management                         │ │
│  │  - Rollout algorithm                              │ │
│  │  - Audit logging                                  │ │
│  └───────────────────────────────────────────────────┘ │
│                           │                              │
│  ┌───────────────────────────────────────────────────┐ │
│  │      Repository Layer (Spring Data JPA)           │ │
│  │  - Database abstraction                           │ │
│  │  - Custom queries                                 │ │
│  │  - Pagination support                             │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                           │
                           │ JDBC
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  PostgreSQL Database                     │
│  - Tables: workspaces, feature_flag,                    │
│    workspace_feature_flag, feature_flag_audit_log       │
│  - Schema managed by Flyway migrations                  │
└─────────────────────────────────────────────────────────┘
```

### Architecture Pattern

**Layered Architecture** with clear separation of concerns:

1. **Presentation Layer (Controllers)**
   - REST endpoint definitions
   - Request/response mapping
   - HTTP-specific concerns

2. **Business Layer (Services)**
   - Domain logic
   - Transaction boundaries
   - Cross-cutting concerns (audit logging)

3. **Data Access Layer (Repositories)**
   - Database operations
   - Query abstraction

4. **Database Layer**
   - Persistent storage
   - Referential integrity
   - Constraints and indexes

### Deployment Architecture

**Monorepo Structure:**
```
feature-flag/
├── frontend/          # Vue.js SPA
│   └── dist/         # Built frontend assets (generated)
├── src/              # Backend (Kotlin + Spring Boot)
│   └── main/
│       └── resources/
│           └── static/ # Frontend assets copied here during build
└── build/libs/       # Final JAR with embedded frontend
```

**Single JAR Deployment:**
- Frontend built with Vite → `frontend/dist/`
- Backend Gradle build copies `frontend/dist/` → `src/main/resources/static/`
- Spring Boot serves static assets from classpath
- Single executable JAR contains both frontend and backend

---

## Technology Stack

### Backend Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Language | Kotlin | 2.1.0 | Primary backend language |
| Framework | Spring Boot | 3.4.0 | Application framework |
| ORM | Spring Data JPA | (Spring Boot) | Database abstraction |
| Database | PostgreSQL | 15+ | Relational data storage |
| Migration | Flyway | (Spring Boot) | Schema version control |
| Validation | Jakarta Bean Validation | (Spring Boot) | Input validation |
| Testing | JUnit 5 + MockK | (Spring Boot) | Unit and integration tests |
| Container Testing | TestContainers | Latest | Integration test database |

### Frontend Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | Vue.js | 3.x | Reactive UI framework |
| Build Tool | Vite | 6.x | Fast build and dev server |
| Styling | Tailwind CSS | 3.x | Utility-first CSS |
| HTTP Client | Axios | Latest | API communication |
| State Management | Composition API | Vue 3 | Local component state |

### Build and Deployment

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Backend Build | Gradle | 8.x | Kotlin compilation, dependency management |
| Frontend Build | npm | Latest | Package management and build |
| Containerization | Docker | Latest | Container images |
| Orchestration | Docker Compose | Latest | Local development stack |

---

## Data Model

### Entity Relationship Diagram

```
┌─────────────────────┐
│    workspaces       │
├─────────────────────┤
│ id (PK)            │
│ name               │
│ workspace_type     │
│ region             │◄──────────┐
└─────────────────────┘           │
                                  │
                                  │ Many-to-One
                                  │
┌─────────────────────┐           │
│   feature_flag      │           │
├─────────────────────┤           │
│ id (PK)            │           │
│ name               │           │
│ description        │           │
│ team               │           │
│ rollout_percentage │           │
│ regions (ARRAY)    │           │
│ created_at         │           │
│ updated_at         │◄──────────┼──────────┐
└─────────────────────┘           │          │
         │                        │          │
         │                        │          │ Many-to-One
         │ One-to-Many            │          │
         │                        │          │
         ▼                        │          │
┌─────────────────────────────────┐          │
│  workspace_feature_flag         │          │
├─────────────────────────────────┤          │
│ id (PK)                        │          │
│ workspace_id (FK) ─────────────┘          │
│ feature_flag_id (FK) ─────────────────────┘
│ enabled                        │
│ updated_at                     │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  feature_flag_audit_log         │
├─────────────────────────────────┤
│ id (PK)                        │
│ feature_flag_id (FK) ──────────┐
│ operation (CREATE/UPDATE/DELETE)│
│ old_values (JSONB)             │
│ new_values (JSONB)             │
│ changed_by                     │
│ team                           │
│ timestamp                      │
└─────────────────────────────────┘
                │
                └──────────────────┐
                                   │
                         (Soft reference)
```

### Database Schema

#### Table: `workspaces`

```sql
CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    workspace_type VARCHAR(100),
    region VARCHAR(50) NOT NULL
);

CREATE INDEX idx_workspaces_region ON workspaces(region);
CREATE INDEX idx_workspaces_name ON workspaces(name);
```

**Columns:**
- `id`: Auto-incrementing primary key
- `name`: Workspace display name (searchable)
- `workspace_type`: Workspace classification (e.g., "development", "production")
- `region`: Azure region code (e.g., "WESTEUROPE", "EASTUS")

#### Table: `feature_flag`

```sql
CREATE TABLE feature_flag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    team VARCHAR(255) NOT NULL,
    rollout_percentage INTEGER NOT NULL DEFAULT 0,
    regions TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_flag_per_team UNIQUE (team, name),
    CONSTRAINT valid_rollout_percentage CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100)
);

CREATE INDEX idx_feature_flag_team ON feature_flag(team);
```

**Columns:**
- `id`: Auto-incrementing primary key
- `name`: Feature flag name (unique within team)
- `description`: Human-readable description
- `team`: Owning team identifier
- `rollout_percentage`: Percentage of target workspaces to enable (0-100)
- `regions`: Array of target regions or ["ALL"]
- `created_at`: Creation timestamp
- `updated_at`: Last modification timestamp

**Constraints:**
- `unique_flag_per_team`: Prevents duplicate flag names within same team
- `valid_rollout_percentage`: Ensures percentage in valid range

#### Table: `workspace_feature_flag`

```sql
CREATE TABLE workspace_feature_flag (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    feature_flag_id BIGINT NOT NULL REFERENCES feature_flag(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_workspace_flag UNIQUE (workspace_id, feature_flag_id)
);

CREATE INDEX idx_workspace_feature_flag_workspace ON workspace_feature_flag(workspace_id);
CREATE INDEX idx_workspace_feature_flag_flag ON workspace_feature_flag(feature_flag_id);
CREATE INDEX idx_workspace_feature_flag_enabled ON workspace_feature_flag(enabled);
```

**Columns:**
- `id`: Auto-incrementing primary key
- `workspace_id`: Foreign key to workspaces
- `feature_flag_id`: Foreign key to feature_flag
- `enabled`: Whether flag is enabled for this workspace
- `updated_at`: Last state change timestamp

**Constraints:**
- `unique_workspace_flag`: One entry per workspace-flag pair
- Cascade delete: Removing workspace or flag removes associations

#### Table: `feature_flag_audit_log`

```sql
CREATE TABLE feature_flag_audit_log (
    id BIGSERIAL PRIMARY KEY,
    feature_flag_id BIGINT,
    operation VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(255),
    team VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_flag_id ON feature_flag_audit_log(feature_flag_id);
CREATE INDEX idx_audit_log_team ON feature_flag_audit_log(team);
CREATE INDEX idx_audit_log_operation ON feature_flag_audit_log(operation);
CREATE INDEX idx_audit_log_timestamp ON feature_flag_audit_log(timestamp DESC);
```

**Columns:**
- `id`: Auto-incrementing primary key
- `feature_flag_id`: Reference to feature flag (nullable for DELETE)
- `operation`: Operation type (CREATE, UPDATE, DELETE)
- `old_values`: Previous state as JSON (null for CREATE)
- `new_values`: New state as JSON (null for DELETE)
- `changed_by`: User identifier (currently null - no auth)
- `team`: Team that owns the flag
- `timestamp`: When operation occurred

**Note:** No foreign key constraint on `feature_flag_id` to preserve audit history after flag deletion.

---

## Core Algorithms

### 1. Deterministic Rollout Algorithm

**Purpose:** Select a consistent subset of workspaces to enable a feature flag based on rollout percentage.

**Requirements:**
- Same workspace must always have same rank for a given flag
- Increasing percentage must keep previously enabled workspaces
- Decreasing percentage must disable predictable workspaces
- Distribution should be roughly uniform

**Algorithm Implementation:**

```kotlin
fun applyRolloutPercentage(featureFlagId: Long, rolloutPercentage: Int) {
    // Step 1: Get target workspaces (filtered by region)
    val targetWorkspaces = if (featureFlag.regions.contains("ALL")) {
        workspaceRepository.findAll()
    } else {
        workspaceRepository.findByRegionIn(featureFlag.regions)
    }

    // Step 2: Sort deterministically by hash
    val sortedWorkspaces = targetWorkspaces.sortedBy { workspace ->
        val hashInput = "${featureFlagId}_${workspace.id}"
        abs(hashInput.hashCode())
    }

    // Step 3: Calculate target count
    val targetCount = (sortedWorkspaces.size * rolloutPercentage / 100.0).toInt()

    // Step 4: Enable first N workspaces, disable rest
    sortedWorkspaces.forEachIndexed { index, workspace ->
        val shouldEnable = index < targetCount
        updateWorkspaceFeatureFlag(workspace.id, featureFlagId, shouldEnable)
    }
}
```

**Key Properties:**

1. **Determinism:**
   - Hash based on `featureFlagId + workspaceId`
   - String concatenation ensures unique hash per pair
   - `abs()` converts to positive number for sorting

2. **Stability:**
   - Same inputs always produce same sort order
   - Rollout 30% → 50% enables additional workspaces without changing first 30%
   - Rollout 50% → 30% disables last 20% of sorted list

3. **Distribution:**
   - Hash function provides pseudo-random distribution
   - Not cryptographically secure but sufficient for fair distribution
   - Workspaces with similar IDs don't cluster together

**Example:**

Given workspaces [1, 2, 3, 4, 5] and feature flag 100:

```
Workspace 1: hash("100_1") = 48281 → rank 2
Workspace 2: hash("100_2") = 48282 → rank 3
Workspace 3: hash("100_3") = 48283 → rank 4
Workspace 4: hash("100_4") = 48284 → rank 5
Workspace 5: hash("100_5") = 48285 → rank 1

Sorted order: [5, 1, 2, 3, 4]

20% rollout → enable workspace 5
40% rollout → enable workspaces 5, 1
60% rollout → enable workspaces 5, 1, 2
```

### 2. Region Filtering Algorithm

**Purpose:** Apply feature flags only to workspaces in target regions.

**Implementation:**

```kotlin
fun getEligibleWorkspaces(featureFlag: FeatureFlag): List<Workspace> {
    return if (featureFlag.regions.contains("ALL")) {
        // Global flag - all workspaces eligible
        workspaceRepository.findAll()
    } else {
        // Regional flag - filter by region
        workspaceRepository.findByRegionIn(featureFlag.regions)
    }
}
```

**Region Matching Logic:**

```kotlin
fun isFeatureFlagApplicableToWorkspace(
    workspaceRegion: String,
    flagRegions: List<String>
): Boolean {
    return flagRegions.contains("ALL") || flagRegions.contains(workspaceRegion)
}
```

**Examples:**

- Flag regions: `["ALL"]` → All workspaces match
- Flag regions: `["WESTEUROPE"]` → Only WESTEUROPE workspaces
- Flag regions: `["WESTEUROPE", "EASTUS"]` → WESTEUROPE OR EASTUS workspaces
- Flag regions: `["WESTEUROPE"]`, Workspace: `EASTUS` → No match

### 3. Workspace Count by Region Algorithm

**Purpose:** Calculate enabled and total workspace counts for each region.

**SQL Query:**

```sql
SELECT
    w.region,
    COUNT(*) as total_count,
    SUM(CASE WHEN wff.enabled = true THEN 1 ELSE 0 END) as enabled_count
FROM workspace_feature_flag wff
JOIN workspaces w ON wff.workspace_id = w.id
WHERE wff.feature_flag_id = :featureFlagId
GROUP BY w.region
ORDER BY w.region
```

**Implementation:**

```kotlin
fun getWorkspaceCountsByRegion(featureFlagId: Long): List<RegionWorkspaceCountDto> {
    return workspaceFeatureFlagRepository
        .findWorkspaceCountsByRegion(featureFlagId)
        .map { (region, total, enabled) ->
            RegionWorkspaceCountDto(
                region = region,
                totalCount = total,
                enabledCount = enabled
            )
        }
}
```

**Performance:** O(n) single table scan with GROUP BY, indexed on `feature_flag_id`.

---

## API Specification

### Base URL
- **Development:** `http://localhost:8080/api`
- **Production:** `https://<domain>/api`

### Common Response Patterns

**Success Response (200 OK):**
```json
{
  "id": 1,
  "name": "feature-name",
  ...
}
```

**Created Response (201 Created):**
```json
{
  "id": 1,
  "name": "feature-name",
  ...
}
```

**Error Response (4xx/5xx):**
```json
{
  "timestamp": "2026-01-27T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: name must not be blank",
  "path": "/api/feature-flags"
}
```

**Paginated Response:**
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```

### Feature Flag Endpoints

#### Create Feature Flag

**Endpoint:** `POST /api/feature-flags`

**Request Body:**
```json
{
  "name": "new-checkout-flow",
  "description": "New streamlined checkout process",
  "team": "payments",
  "rolloutPercentage": 10,
  "regions": ["WESTEUROPE", "EASTUS"]
}
```

**Validation Rules:**
- `name`: Required, 1-255 characters
- `description`: Optional, max 1000 characters
- `team`: Required, 1-255 characters
- `rolloutPercentage`: Required, 0-100
- `regions`: Required, non-empty array

**Response:** `201 Created`
```json
{
  "id": 42,
  "name": "new-checkout-flow",
  "description": "New streamlined checkout process",
  "team": "payments",
  "rolloutPercentage": 10,
  "regions": ["WESTEUROPE", "EASTUS"],
  "createdAt": "2026-01-27T10:00:00Z",
  "updatedAt": "2026-01-27T10:00:00Z"
}
```

**Error Cases:**
- `400 Bad Request`: Validation failure
- `409 Conflict`: Flag name already exists for team

#### Update Feature Flag

**Endpoint:** `PUT /api/feature-flags/{id}`

**Request Body:**
```json
{
  "name": "new-checkout-flow",
  "description": "Updated description",
  "team": "payments",
  "rolloutPercentage": 25,
  "regions": ["WESTEUROPE", "EASTUS", "UKSOUTH"]
}
```

**Response:** `200 OK` (same structure as create)

**Side Effects:**
- Recalculates workspace associations based on new percentage
- Logs update to audit trail with before/after values

#### Delete Feature Flag

**Endpoint:** `DELETE /api/feature-flags/{id}`

**Response:** `204 No Content`

**Side Effects:**
- Cascade deletes workspace associations
- Logs deletion to audit trail

#### Get All Feature Flags

**Endpoint:** `GET /api/feature-flags`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "feature-a",
    ...
  },
  {
    "id": 2,
    "name": "feature-b",
    ...
  }
]
```

#### Get Enabled Workspaces (Paginated)

**Endpoint:** `GET /api/feature-flags/{id}/enabled-workspaces`

**Query Parameters:**
- `page`: Page number (0-indexed, default: 0)
- `size`: Page size (default: 100, max: 500)
- `search`: Search term for workspace name/type/region

**Example:** `GET /api/feature-flags/42/enabled-workspaces?page=0&size=100&search=prod`

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "name": "customer-a-prod",
      "workspaceType": "production",
      "region": "WESTEUROPE"
    },
    ...
  ],
  "page": 0,
  "size": 100,
  "totalElements": 523,
  "totalPages": 6,
  "last": false
}
```

#### Get Workspace Counts by Region

**Endpoint:** `GET /api/feature-flags/{id}/workspace-counts-by-region`

**Response:** `200 OK`
```json
[
  {
    "region": "EASTUS",
    "totalCount": 1500,
    "enabledCount": 375
  },
  {
    "region": "WESTEUROPE",
    "totalCount": 2000,
    "enabledCount": 500
  },
  {
    "region": "UKSOUTH",
    "totalCount": 800,
    "enabledCount": 200
  }
]
```

#### Manual Workspace Enable/Disable

**Endpoint:** `PUT /api/feature-flags/{id}/workspaces`

**Request Body:**
```json
{
  "workspaceId": 12345,
  "enabled": true
}
```

**Response:** `200 OK`

**Use Cases:**
- Manual testing in specific production workspace
- VIP customer override
- Quick hotfix enable/disable

### Workspace Endpoints

#### Get All Workspaces (Paginated)

**Endpoint:** `GET /api/workspaces`

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `search`: Search term for name/type/region

**Example:** `GET /api/workspaces?page=0&size=20&search=europe`

**Response:** `200 OK` (paginated response structure)

#### Get Enabled Flags for Workspace

**Endpoint:** `GET /api/workspaces/{id}/enabled-feature-flags`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "feature-a",
    "description": "...",
    "team": "team-a",
    "rolloutPercentage": 50,
    "regions": ["ALL"]
  },
  ...
]
```

**Logic:** Returns flags where:
- `workspace_feature_flag.enabled = true` for this workspace
- Feature flag regions contain "ALL" OR workspace region

### Audit Log Endpoints

#### Get Audit Logs

**Endpoint:** `GET /api/audit-logs`

**Query Parameters:**
- `featureFlagId`: Filter by feature flag ID
- `team`: Filter by team name
- `operation`: Filter by operation type (CREATE/UPDATE/DELETE)

**Example:** `GET /api/audit-logs?team=payments&operation=UPDATE`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "featureFlagId": 42,
    "operation": "UPDATE",
    "oldValues": {
      "rolloutPercentage": 10,
      "regions": ["WESTEUROPE"]
    },
    "newValues": {
      "rolloutPercentage": 25,
      "regions": ["WESTEUROPE", "EASTUS"]
    },
    "changedBy": null,
    "team": "payments",
    "timestamp": "2026-01-27T10:15:00Z"
  },
  ...
]
```

---

## Frontend Architecture

### Component Structure

```
App.vue (root component)
├── components/
│   ├── FeatureFlagList.vue         # Main list view
│   ├── CreateFeatureFlag.vue       # Creation form
│   ├── EditFeatureFlag.vue         # Edit form
│   ├── EnabledWorkspacesModal.vue  # Workspace list modal
│   ├── RegionBadge.vue             # Region display component
│   └── LoadingSpinner.vue          # Reusable loader
└── composables/
    ├── useApiCall.js               # API wrapper with state
    ├── useFormState.js             # Form state management
    ├── useDebouncedSearch.js       # Debounced search input
    └── useInfiniteScroll.js        # Infinite scroll pagination
```

### Composables (Reusable Logic)

#### useApiCall

**Purpose:** Wrap API calls with loading/error states

```javascript
export function useApiCall() {
  const loading = ref(false)
  const error = ref(null)

  async function execute(apiFunction) {
    loading.value = true
    error.value = null
    try {
      const result = await apiFunction()
      return result
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  return { loading, error, execute }
}
```

#### useDebouncedSearch

**Purpose:** Debounce search input to reduce API calls

```javascript
export function useDebouncedSearch(callback, delay = 300) {
  const searchTerm = ref('')
  let timeout = null

  watch(searchTerm, (newValue) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => {
      callback(newValue)
    }, delay)
  })

  return { searchTerm }
}
```

#### useInfiniteScroll

**Purpose:** Handle infinite scroll pagination

```javascript
export function useInfiniteScroll(fetchFunction) {
  const items = ref([])
  const page = ref(0)
  const hasMore = ref(true)
  const loading = ref(false)

  async function loadMore() {
    if (loading.value || !hasMore.value) return

    loading.value = true
    const response = await fetchFunction(page.value)
    items.value.push(...response.content)
    hasMore.value = !response.last
    page.value++
    loading.value = false
  }

  return { items, loading, hasMore, loadMore }
}
```

### API Service Layer

**File:** `services/api-service.js`

```javascript
import axios from 'axios'

const API_BASE = '/api'

export const featureFlagService = {
  getAll: () => axios.get(`${API_BASE}/feature-flags`),
  create: (data) => axios.post(`${API_BASE}/feature-flags`, data),
  update: (id, data) => axios.put(`${API_BASE}/feature-flags/${id}`, data),
  delete: (id) => axios.delete(`${API_BASE}/feature-flags/${id}`),
  getEnabledWorkspaces: (id, page, size, search) =>
    axios.get(`${API_BASE}/feature-flags/${id}/enabled-workspaces`, {
      params: { page, size, search }
    }),
  getWorkspaceCountsByRegion: (id) =>
    axios.get(`${API_BASE}/feature-flags/${id}/workspace-counts-by-region`)
}
```

### State Management

**Approach:** Local component state using Vue 3 Composition API

**Rationale:**
- No complex global state requirements
- Component-level state sufficient for current features
- Composition API provides reactive state out of the box
- No need for Vuex/Pinia overhead

**Example:**

```javascript
// In component
const featureFlags = ref([])
const selectedFlag = ref(null)
const isModalOpen = ref(false)

// Reactive derived state
const sortedFlags = computed(() =>
  featureFlags.value.sort((a, b) => a.name.localeCompare(b.name))
)
```

---

## Build and Deployment

### Monorepo Build Integration

**Gradle Build Tasks:**

```kotlin
// Custom task: Build frontend
tasks.register<Exec>("buildFrontend") {
    workingDir("../frontend")
    commandLine("npm", "run", "build")
}

// Custom task: Copy frontend dist to backend static
tasks.register<Copy>("copyFrontendDist") {
    dependsOn("buildFrontend")
    from("../frontend/dist")
    into("src/main/resources/static")
}

// Hook into bootJar task
tasks.named("bootJar") {
    dependsOn("copyFrontendDist")
}
```

**Build Flow:**

```
1. Developer runs: ./gradlew build
2. Gradle executes: buildFrontend task
   └─> npm run build in frontend/
   └─> Generates frontend/dist/
3. Gradle executes: copyFrontendDist task
   └─> Copies frontend/dist/* → backend/src/main/resources/static/
4. Gradle executes: bootJar task
   └─> Packages backend + static resources into single JAR
5. Output: build/libs/feature-flag-1.0.0.jar
```

### Docker Multi-Stage Build

**Dockerfile:**

```dockerfile
# Stage 1: Build frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend
FROM gradle:8-jdk17 AS backend-builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src/ ./src/
COPY --from=frontend-builder /app/frontend/dist/ ./src/main/resources/static/
RUN gradle bootJar --no-daemon

# Stage 3: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose Stack

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: feature-flag
      POSTGRES_USER: feature-flag
      POSTGRES_PASSWORD: feature-flag123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/feature-flag
      SPRING_DATASOURCE_USERNAME: feature-flag
      SPRING_DATASOURCE_PASSWORD: feature-flag123
    depends_on:
      - postgres

volumes:
  postgres-data:
```

### Environment Configuration

**application.yml:**

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/feature-flag}
    username: ${SPRING_DATASOURCE_USERNAME:feature-flag}
    password: ${SPRING_DATASOURCE_PASSWORD:feature-flag123}
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: ${SERVER_PORT:8080}
```

---

## Testing Strategy

### Test Pyramid

```
        ┌─────────────┐
        │    E2E      │  (None - manual testing)
        │   Tests     │
        └─────────────┘
       ┌─────────────────┐
       │  Integration    │  (TestContainers)
       │     Tests       │
       └─────────────────┘
    ┌───────────────────────┐
    │     Unit Tests        │  (Service + Controller)
    │  (MockK + JUnit 5)    │
    └───────────────────────┘
```

### Unit Tests

**Focus:** Service layer business logic

**Example:** `FeatureFlagServiceTest.kt`

```kotlin
@Test
fun `should apply rollout percentage deterministically`() {
    // Given
    val featureFlag = createFeatureFlag(rolloutPercentage = 30)
    val workspaces = createWorkspaces(100)

    // When
    service.applyRolloutPercentage(featureFlag.id, 30)

    // Then
    val enabled = workspaceFeatureFlagRepo.findByFeatureFlagIdAndEnabled(
        featureFlag.id, true
    )
    assertThat(enabled).hasSize(30)
}

@Test
fun `increasing rollout should keep existing workspaces enabled`() {
    // Given
    val featureFlag = createFeatureFlag(rolloutPercentage = 30)
    service.applyRolloutPercentage(featureFlag.id, 30)
    val initialEnabled = getEnabledWorkspaceIds(featureFlag.id)

    // When
    service.applyRolloutPercentage(featureFlag.id, 60)

    // Then
    val newEnabled = getEnabledWorkspaceIds(featureFlag.id)
    assertThat(newEnabled).containsAll(initialEnabled)
    assertThat(newEnabled).hasSize(60)
}
```

### Integration Tests

**Focus:** End-to-end API tests with real database

**Setup:** TestContainers for PostgreSQL

```kotlin
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class FeatureFlagIntegrationTest {

    @Container
    val postgres = PostgreSQLContainer("postgres:15-alpine")

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Test
    fun `create feature flag returns 201 with location header`() {
        // Given
        val request = CreateFeatureFlagRequest(
            name = "test-flag",
            team = "test-team",
            rolloutPercentage = 50,
            regions = listOf("ALL")
        )

        // When
        val response = testRestTemplate.postForEntity(
            "/api/feature-flags",
            request,
            FeatureFlagDto::class.java
        )

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.name).isEqualTo("test-flag")
    }
}
```

### Controller Tests

**Focus:** Request validation and HTTP status codes

**Setup:** MockMvc with mocked service layer

```kotlin
@WebMvcTest(FeatureFlagController::class)
class FeatureFlagControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var featureFlagService: FeatureFlagService

    @Test
    fun `create with invalid rollout percentage returns 400`() {
        // When/Then
        mockMvc.perform(
            post("/api/feature-flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "test",
                        "team": "test",
                        "rolloutPercentage": 150,
                        "regions": ["ALL"]
                    }
                """)
        )
        .andExpect(status().isBadRequest)
    }
}
```

---

## Performance Considerations

### Database Indexing

**Critical Indexes:**

```sql
-- Workspace lookups by region
CREATE INDEX idx_workspaces_region ON workspaces(region);

-- Feature flag team filtering
CREATE INDEX idx_feature_flag_team ON feature_flag(team);

-- Workspace-flag join queries
CREATE INDEX idx_workspace_feature_flag_workspace
    ON workspace_feature_flag(workspace_id);
CREATE INDEX idx_workspace_feature_flag_flag
    ON workspace_feature_flag(feature_flag_id);
CREATE INDEX idx_workspace_feature_flag_enabled
    ON workspace_feature_flag(enabled);

-- Audit log queries
CREATE INDEX idx_audit_log_flag_id
    ON feature_flag_audit_log(feature_flag_id);
CREATE INDEX idx_audit_log_timestamp
    ON feature_flag_audit_log(timestamp DESC);
```

### Query Optimization

**N+1 Problem Prevention:**

```kotlin
// Bad: N+1 query problem
val flags = featureFlagRepo.findAll()
flags.forEach { flag ->
    val workspaces = workspaceRepo.findByFeatureFlagId(flag.id) // N queries
}

// Good: Single query with join
val flags = featureFlagRepo.findAllWithWorkspaces() // 1 query with JOIN
```

**Pagination for Large Datasets:**

```kotlin
// Always use pagination for unbounded queries
fun getEnabledWorkspaces(
    featureFlagId: Long,
    pageable: Pageable
): Page<Workspace> {
    return workspaceRepo.findEnabledByFeatureFlagId(featureFlagId, pageable)
}
```

### Caching Strategy (Future Enhancement)

**Candidates for caching:**
- Feature flag metadata (low write, high read)
- Workspace metadata (rarely changes)
- Region workspace counts (expensive aggregation)

**Not suitable for caching:**
- Workspace-flag associations (changes frequently with rollouts)
- Audit logs (write-heavy, sequential reads)

---

## Security Considerations

### Input Validation

**Jakarta Bean Validation:**

```kotlin
data class CreateFeatureFlagRequest(
    @field:NotBlank(message = "Name must not be blank")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    @field:Min(value = 0, message = "Rollout percentage must be >= 0")
    @field:Max(value = 100, message = "Rollout percentage must be <= 100")
    val rolloutPercentage: Int,

    @field:NotEmpty(message = "Regions must not be empty")
    val regions: List<String>
)
```

### SQL Injection Prevention

**Spring Data JPA parameterized queries:**

```kotlin
// Safe: Parameterized query
@Query("SELECT w FROM Workspace w WHERE w.region IN :regions")
fun findByRegionIn(@Param("regions") regions: List<String>): List<Workspace>

// Never do this (SQL injection risk):
// entityManager.createQuery("SELECT w FROM Workspace w WHERE w.region = '$userInput'")
```

### CORS Configuration (Production)

```kotlin
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://yourdomain.com")  // Restrict in production
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
    }
}
```

### Known Security Limitations

1. **No Authentication:**
   - All endpoints are public
   - No user identity tracking
   - `changedBy` field in audit logs is always null

2. **No Authorization:**
   - No team-level access control
   - Any client can modify any team's flags
   - No rate limiting

3. **No Encryption:**
   - No TLS enforcement
   - Database passwords in environment variables (use secrets manager in production)

---

## Appendix: File Locations

### Backend (Kotlin)

- **Controllers:** `src/main/kotlin/com/example/featureflag/controller/`
- **Services:** `src/main/kotlin/com/example/featureflag/service/`
- **Repositories:** `src/main/kotlin/com/example/featureflag/repository/`
- **Entities:** `src/main/kotlin/com/example/featureflag/entity/`
- **DTOs:** `src/main/kotlin/com/example/featureflag/dto/`
- **Migrations:** `src/main/resources/db/migration/`
- **Config:** `src/main/resources/application.yml`

### Frontend (Vue.js)

- **Components:** `frontend/src/components/`
- **Composables:** `frontend/src/composables/`
- **Services:** `frontend/src/services/`
- **Root:** `frontend/src/App.vue`
- **Config:** `frontend/vite.config.js`

### Docker

- **Dockerfile:** `Dockerfile`
- **Compose:** `docker-compose.yml`

### Tests

- **Unit Tests:** `src/test/kotlin/com/example/featureflag/service/`
- **Integration Tests:** `src/test/kotlin/com/example/featureflag/integration/`

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-27 | System | Initial technical specification |
