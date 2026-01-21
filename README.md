<div align="center">

# 🚀 Feature Flag Management Service

### A modern, multi-tenant, multi-region feature flag management system with audit logging

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-green?style=flat-square&logo=vue.js)](https://vuejs.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue?style=flat-square&logo=docker)](https://www.docker.com/)

Built with Kotlin, Spring Boot, and PostgreSQL, providing multi-tenant workspace support with multi-region feature flags, automatic rollout percentage control, and comprehensive audit logging.

[Features](#-features) •
[Quick Start](#-getting-started) •
[API Docs](#-api-endpoints) •
[Architecture](#-architecture) •
[Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Technology Stack](#-technology-stack)
- [Features](#-features)
- [Getting Started](#-getting-started)
- [API Endpoints](#-api-endpoints)
- [Database Schema](#-database-schema)
- [API Usage Examples](#-api-usage-examples)
- [Testing](#-testing)
- [Monitoring](#-monitoring)
- [Architecture](#-architecture)
- [Feature Flag Rollout Logic](#-feature-flag-rollout-logic)
- [Multi-Region Support](#-multi-region-support)
- [Audit Logging](#-audit-logging)
- [Web UI](#-web-ui)
- [Development](#-development)
- [Known Limitations](#-known-limitations)
- [Future Enhancements](#-future-enhancements)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🛠 Technology Stack

| Category | Technology |
|----------|-----------|
| **Language & Runtime** | Kotlin 2.1.0, Java 21 |
| **Framework** | Spring Boot 3.4.0 |
| **Database** | PostgreSQL 15 with Flyway migrations |
| **Build Tool** | Gradle with Kotlin DSL |
| **Testing** | JUnit 5, MockK, TestContainers |
| **Security** | OWASP Dependency Check |
| **Containerization** | Docker & Docker Compose |
| **Frontend** | Vue.js 3, Axios, Font Awesome |

---

## ✨ Features

### 🎯 Core Functionality

<table>
<tr>
<td width="50%">

#### 🏢 Multi-Tenant Workspace Management
Complete workspace isolation with support for multiple regions per workspace

</td>
<td width="50%">

#### 🌍 Multi-Region Support
Target feature flags to specific Azure regions or deploy globally

</td>
</tr>
<tr>
<td width="50%">

#### 🎚️ Advanced Feature Flag Management
Team-based feature flags with intelligent rollout percentages (0-100%)

</td>
<td width="50%">

#### 🔄 Smart Rollout Algorithm
Improved consistent hash-based distribution ensuring stable rollout increases/decreases

</td>
</tr>
<tr>
<td width="50%">

#### 📝 Comprehensive Audit Logging
Track all CREATE, UPDATE, and DELETE operations with before/after values

</td>
<td width="50%">

#### 💻 Modern Web UI
Feature-rich Vue.js interface with real-time updates and region selection

</td>
</tr>
</table>

### 🌍 Supported Azure Regions

The system supports targeting feature flags to specific Azure regions:

- 🌐 **ALL** (Global deployment)
- 🇪🇺 **WESTEUROPE** - West Europe
- 🇺🇸 **EASTUS** - East US
- 🇨🇦 **CANADACENTRAL** - Canada Central
- 🇦🇺 **AUSTRALIAEAST** - Australia East
- 🇩🇪 **GERMANYWESTCENTRAL** - Germany West Central
- 🇨🇭 **SWITZERLANDNORTH** - Switzerland North
- 🇦🇪 **UAENORTH** - UAE North
- 🇬🇧 **UKSOUTH** - UK South
- 🇧🇷 **BRAZILSOUTH** - Brazil South
- 🇸🇬 **SOUTHEASTASIA** - Southeast Asia
- 🇯🇵 **JAPANEAST** - Japan East
- 🇪🇺 **NORTHEUROPE** - North Europe

---

## 📡 API Endpoints

### 🔧 API Testing with Bruno

A complete **Bruno API collection** is available in the `bruno-collection/` directory with all API endpoints organized and ready to use.

**Quick Start:**
1. Install [Bruno](https://www.usebruno.com/) - A fast and git-friendly API client
2. Open Bruno and select "Open Collection"
3. Navigate to the `bruno-collection` folder
4. Start testing all endpoints with pre-configured requests

**Collection includes:**
- ✅ 12 Feature Flag endpoints
- ✅ 3 Workspace endpoints
- ✅ 5 Audit Log endpoints
- ✅ Environment configuration (Local)
- ✅ Example request bodies
- ✅ Documentation for each endpoint

📖 **See [bruno-collection/README.md](bruno-collection/README.md) for complete documentation**

### 🏢 Workspaces

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/workspaces` | 📋 List all workspaces with their regions |
| `GET` | `/api/workspaces/{id}` | 🔍 Get workspace by ID with region information |

### 🎚️ Feature Flags

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/feature-flags` | 📋 List all feature flags with regions |
| `POST` | `/api/feature-flags` | ➕ Create feature flag with region targeting |
| `GET` | `/api/feature-flags/{id}` | 🔍 Get feature flag by ID |
| `PUT` | `/api/feature-flags/{id}` | ✏️ Update feature flag (regions, rollout, etc.) |
| `DELETE` | `/api/feature-flags/{id}` | ❌ Delete feature flag |
| `GET` | `/api/feature-flags/workspace/{workspaceId}` | 🏢 Get flags for workspace (region-filtered) |
| `GET` | `/api/feature-flags/team/{team}` | 👥 Get flags by team |
| `GET` | `/api/feature-flags/search?name={name}` | 🔎 Search feature flags |
| `PUT` | `/api/feature-flags/{id}/workspaces` | 🎯 Enable/disable flag for specific workspaces |
| `GET` | `/api/feature-flags/{id}/enabled-workspaces` | 🏢 Get paginated list of enabled workspaces |
| `GET` | `/api/feature-flags/{id}/workspace-counts-by-region` | 📊 Get enabled/total workspace counts per region |

### 📝 Audit Logs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/audit-logs` | 📋 List all audit logs (supports filtering) |
| `GET` | `/api/audit-logs/feature-flag/{id}` | 🎚️ Get audit logs for specific feature flag |
| `GET` | `/api/audit-logs/team/{team}` | 👥 Get audit logs by team |
| `GET` | `/api/audit-logs/operation/{operation}` | 🔍 Get logs by operation (CREATE/UPDATE/DELETE) |

**Query Parameters for `/api/audit-logs`:**
- `featureFlagId` - Filter by feature flag ID
- `team` - Filter by team name
- `operation` - Filter by operation type (CREATE, UPDATE, DELETE)

---

## 🗄️ Database Schema

The service uses PostgreSQL with the following main tables:

| Table | Description | Key Fields |
|-------|-------------|------------|
| **`workspaces`** | 🏢 Multi-tenant workspaces | `id`, `name`, `type`, `region`, `created_at`, `updated_at` |
| **`feature_flag`** | 🎚️ Feature flag definitions | `id`, `name`, `description`, `team`, `rollout_percentage`, `regions`, `created_at`, `updated_at` |
| **`workspace_feature_flag`** | 🔗 Workspace-flag relationships | `workspace_id`, `feature_flag_id`, `enabled`, `created_at`, `updated_at` |
| **`feature_flag_audit_log`** | 📝 Audit trail | `id`, `feature_flag_id`, `feature_flag_name`, `operation`, `team`, `old_values`, `new_values`, `changed_by`, `timestamp` |

<details>
<summary>📊 View ERD Diagram</summary>

```
┌─────────────────────┐         ┌──────────────────────────┐         ┌─────────────────────┐
│     workspaces      │         │ workspace_feature_flag   │         │   feature_flag      │
├─────────────────────┤         ├──────────────────────────┤         ├─────────────────────┤
│ id (PK)             │◄────────│ workspace_id (FK)        │         │ id (PK)             │
│ name                │         │ feature_flag_id (FK)     │────────►│ name                │
│ type                │         │ enabled                  │         │ description         │
│ region (ENUM)       │         │ created_at               │         │ team                │
│ created_at          │         │ updated_at               │         │ rollout_percentage  │
│ updated_at          │         └──────────────────────────┘         │ regions             │
└─────────────────────┘                                               │ created_at          │
                                                                      │ updated_at          │
                                                                      └──────────┬──────────┘
                                                                                 │
                                                                                 │
                                                         ┌───────────────────────▼──────────────────────┐
                                                         │       feature_flag_audit_log                 │
                                                         ├──────────────────────────────────────────────┤
                                                         │ id (PK)                                      │
                                                         │ feature_flag_id (FK, nullable)               │
                                                         │ feature_flag_name                            │
                                                         │ operation (CREATE/UPDATE/DELETE)             │
                                                         │ team                                         │
                                                         │ old_values (JSON)                            │
                                                         │ new_values (JSON)                            │
                                                         │ changed_by                                   │
                                                         │ timestamp                                    │
                                                         └──────────────────────────────────────────────┘
```


</details>

### 📊 Database Migrations

The system uses Flyway for version-controlled schema migrations:

| Migration | Description |
|-----------|-------------|
| `V1__Initial_schema.sql` | 🏗️ Complete initial schema with workspaces, feature flags, workspace_feature_flag join table, and audit logging. Includes all indexes for performance optimization. |
| `V2__Add_unique_constraint_to_feature_flag_name.sql` | 🔒 Added unique constraint on feature_flag.name to ensure global uniqueness |

**Note:** The initial migration (V1) includes all core tables with timestamps, region support, and audit logging built-in from the start.

---

## 🚀 Getting Started

### 📋 Prerequisites

- 🐳 Docker & Docker Compose
- ☕ Java 21 (for local development)

### 🐳 Running with Docker Compose

#### 1️⃣ **Clone and build the application:**
```bash
./gradlew clean build
```

#### 2️⃣ **Start the complete stack:**
```bash
docker-compose up -d
```

This starts:
- 🐘 PostgreSQL database (port 5432)
- 🚀 Feature Flag application (port 8080)

#### 3️⃣ **Access the application:**

| Service | URL | Description |
|---------|-----|-------------|
| 💻 **Web UI** | http://localhost:8080 | Main application interface |
| 📡 **API** | http://localhost:8080/api | REST API endpoints |
| ❤️ **Health Check** | http://localhost:8080/actuator/health | Application health status |

#### 4️⃣ **Optional: Start with pgAdmin:**
```bash
docker-compose --profile tools up -d
```

### 💻 Local Development

#### Option 1: Frontend + Backend Separately (Recommended for Development)

1. **Start PostgreSQL:**
```bash
docker-compose up postgres -d
```

2. **Run backend (Terminal 1):**
```bash
./gradlew bootRun
```

3. **Run frontend with hot reload (Terminal 2):**
```bash
cd frontend && npm run dev
```

Access frontend dev server at http://localhost:5173 (proxies API to :8080)

#### Option 2: Single JAR with Bundled Frontend

1. **Start PostgreSQL:**
```bash
docker-compose up postgres -d
```

2. **Build and run:**
```bash
./gradlew build
java -jar build/libs/feature-flag-1.0.0.jar
```

Access application at http://localhost:8080

### ⚙️ Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/feature-flag?currentSchema=public` | 🐘 Database URL |
| `SPRING_DATASOURCE_USERNAME` | `feature-flag` | 👤 Database username |
| `SPRING_DATASOURCE_PASSWORD` | `feature-flag123` | 🔒 Database password |
| `SERVER_PORT` | `8080` | 🌐 Application port |

---

## 📝 API Usage Examples

### 🏢 Get All Workspaces
```bash
curl http://localhost:8080/api/workspaces
```

**Response:**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Production Workspace",
    "type": "PRODUCTION",
    "region": "WESTEUROPE",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
]
```

### 🔍 Get Workspace by ID
```bash
curl http://localhost:8080/api/workspaces/{workspace-id}
```

### ➕ Create a Feature Flag with Region Targeting
```bash
curl -X POST http://localhost:8080/api/feature-flags \
  -H "Content-Type: application/json" \
  -d '{
    "name": "new-checkout-flow",
    "description": "Enable new checkout flow",
    "team": "checkout-team",
    "regions": ["WESTEUROPE", "EASTUS"],
    "rolloutPercentage": 25
  }'
```

**Response:**
```json
{
  "id": "456e7890-e89b-12d3-a456-426614174001",
  "name": "new-checkout-flow",
  "description": "Enable new checkout flow",
  "team": "checkout-team",
  "regions": ["WESTEUROPE", "EASTUS"],
  "rolloutPercentage": 25,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

> **💡 Note:** When a feature flag is created or updated, the system:
> 1. Automatically calculates which workspaces should have it enabled based on rollout percentage
> 2. Filters workspaces by region (only enables for workspaces in matching regions)
> 3. Uses consistent hashing for deterministic workspace selection
> 4. Logs the operation to the audit log

### ✏️ Update a Feature Flag
```bash
curl -X PUT http://localhost:8080/api/feature-flags/{feature-flag-id} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "new-checkout-flow",
    "description": "Enable new checkout flow for more users",
    "team": "checkout-team",
    "regions": ["ALL"],
    "rolloutPercentage": 50
  }'
```

### 👥 Get Feature Flags by Team
```bash
curl http://localhost:8080/api/feature-flags/team/checkout-team
```

### 🏢 Get Feature Flags for a Workspace
```bash
curl http://localhost:8080/api/feature-flags/workspace/{workspace-id}
```

> **ℹ️** This returns only the feature flags that are:
> 1. Enabled for the specified workspace based on rollout percentages
> 2. Targeted to the workspace's region (or flagged as "ALL")

### 🔎 Search Feature Flags
```bash
curl "http://localhost:8080/api/feature-flags/search?name=checkout"
```

### 🎯 Enable/Disable Feature Flag for Specific Workspaces
```bash
curl -X PUT http://localhost:8080/api/feature-flags/{feature-flag-id}/workspaces \
  -H "Content-Type: application/json" \
  -d '{
    "workspaceIds": [
      "workspace-id-1",
      "workspace-id-2",
      "workspace-id-3"
    ],
    "enabled": true
  }'
```

**Response:** `200 OK` (empty body on success)

> **💡 Use Case:** This endpoint allows manual override of the automatic rollout percentage logic. You can explicitly enable or disable a feature flag for specific workspaces by their IDs, regardless of the current rollout percentage. This is useful for:
> - Testing features in specific production workspaces
> - Quick hotfixes (enable/disable for problematic workspaces)
> - Manual control for VIP customers or critical workspaces
> - Gradual rollout with manual control

**To disable:**
```bash
curl -X PUT http://localhost:8080/api/feature-flags/{feature-flag-id}/workspaces \
  -H "Content-Type: application/json" \
  -d '{
    "workspaceIds": ["workspace-id-1"],
    "enabled": false
  }'
```

### ❌ Delete a Feature Flag
```bash
curl -X DELETE http://localhost:8080/api/feature-flags/{feature-flag-id}
```

### 🏢 Get Enabled Workspaces for a Feature Flag
```bash
# Get paginated list of workspaces where flag is enabled
curl "http://localhost:8080/api/feature-flags/{feature-flag-id}/enabled-workspaces?page=0&size=100"

# Search within enabled workspaces
curl "http://localhost:8080/api/feature-flags/{feature-flag-id}/enabled-workspaces?search=production"
```

**Response:**
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Production Workspace EU",
      "type": "PRODUCTION",
      "region": "WESTEUROPE",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ],
  "totalElements": 150,
  "totalPages": 2,
  "currentPage": 0,
  "pageSize": 100,
  "first": true,
  "last": false
}
```

### 📊 Get Workspace Counts by Region
```bash
curl http://localhost:8080/api/feature-flags/{feature-flag-id}/workspace-counts-by-region
```

**Response:**
```json
[
  {
    "region": "WESTEUROPE",
    "enabledCount": 100,
    "totalCount": 150
  },
  {
    "region": "EASTUS",
    "enabledCount": 50,
    "totalCount": 75
  },
  {
    "region": "CANADACENTRAL",
    "enabledCount": 0,
    "totalCount": 30
  }
]
```

> **💡 Use Case:** This endpoint provides a region-level summary showing how many workspaces have a feature flag enabled versus the total number of workspaces in each region. Perfect for understanding regional rollout status at a glance.

### 📝 Get All Audit Logs
```bash
curl http://localhost:8080/api/audit-logs
```

### 📝 Get Audit Logs with Filters
```bash
# Filter by feature flag ID
curl "http://localhost:8080/api/audit-logs?featureFlagId={id}"

# Filter by team
curl "http://localhost:8080/api/audit-logs?team=checkout-team"

# Filter by operation
curl "http://localhost:8080/api/audit-logs?operation=UPDATE"

# Combine filters
curl "http://localhost:8080/api/audit-logs?featureFlagId={id}&operation=UPDATE"
```

**Response:**
```json
[
  {
    "id": "789e1011-e89b-12d3-a456-426614174002",
    "featureFlagId": "456e7890-e89b-12d3-a456-426614174001",
    "featureFlagName": "new-checkout-flow",
    "operation": "UPDATE",
    "team": "checkout-team",
    "oldValues": "{\"rolloutPercentage\":25,\"regions\":[\"WESTEUROPE\"]}",
    "newValues": "{\"rolloutPercentage\":50,\"regions\":[\"ALL\"]}",
    "changedBy": null,
    "timestamp": "2024-01-01T11:00:00"
  }
]
```

### 📝 Get Audit Logs for a Specific Feature Flag
```bash
curl http://localhost:8080/api/audit-logs/feature-flag/{feature-flag-id}
```

### 📝 Get Audit Logs by Team
```bash
curl http://localhost:8080/api/audit-logs/team/checkout-team
```

### 📝 Get Audit Logs by Operation
```bash
curl http://localhost:8080/api/audit-logs/operation/CREATE
```

> **Operations:** `CREATE`, `UPDATE`, `DELETE`

---

## 🧪 Testing

### ▶️ Run All Tests
```bash
./gradlew test
```

The test suite includes:
- **Unit Tests:** Service layer business logic
- **Controller Tests:** REST API endpoints
- **Integration Tests:** End-to-end workflows with TestContainers
- **Exception Handler Tests:** Error handling scenarios

### 📊 View Test Reports
After running tests, view the HTML report at:
```
build/reports/tests/test/index.html
```

### 🔒 Security Vulnerability Check
```bash
./gradlew dependencyCheckAnalyze
```

---

## 📊 Monitoring

The application includes Spring Boot Actuator endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/actuator/health` | `GET` | ❤️ Application health status |
| `/actuator/metrics` | `GET` | 📈 Application metrics |
| `/actuator/info` | `GET` | ℹ️ Application information |

---

## 🏗️ Architecture

The application follows a **layered architecture** with clear separation of concerns:

```
┌─────────────────┐
│   Vue.js UI     │  💻 Frontend (SPA)
└────────┬────────┘
         │
┌────────▼────────┐
│   REST API      │  🌐 HTTP/JSON Interface
└────────┬────────┘
         │
┌────────▼────────┐
│  Controllers    │  📡 Request Handling & Validation
└────────┬────────┘
         │
┌────────▼────────┐
│   Services      │  ⚙️ Business Logic & Orchestration
└────────┬────────┘
         │
┌────────▼────────┐
│  Repositories   │  💾 Data Access Layer (Spring Data JPA)
└────────┬────────┘
         │
┌────────▼────────┐
│   PostgreSQL    │  🐘 Persistent Storage
└─────────────────┘
```

### 🔧 Components:

<table>
<tr>
<td width="33%">

**🎨 Frontend Layer**
- Vue.js 3 SPA
- Component-based architecture
- Axios for API communication
- Real-time UI updates
- Region selection dropdowns

</td>
<td width="33%">

**📡 Controller Layer**
- `FeatureFlagController`
- `WorkspaceController`
- `AuditLogController`
- Jakarta Bean Validation
- RESTful endpoints

</td>
<td width="33%">

**⚙️ Service Layer**
- `FeatureFlagService`
- `WorkspaceService`
- `AuditLogService`
- Business logic
- Transaction management
- Rollout calculations

</td>
</tr>
<tr>
<td width="33%">

**💾 Repository Layer**
- `FeatureFlagRepository`
- `WorkspaceRepository`
- `WorkspaceFeatureFlagRepository`
- `FeatureFlagAuditLogRepository`
- Spring Data JPA
- Custom queries

</td>
<td width="33%">

**📦 Entity Layer**
- `FeatureFlag`
- `Workspace`
- `WorkspaceFeatureFlag`
- `FeatureFlagAuditLog`
- `Region` enum
- `AuditOperation` enum
- JPA relationships

</td>
<td width="33%">

**📤 DTO Layer**
- `FeatureFlagDto`
- `CreateFeatureFlagRequest`
- `UpdateFeatureFlagRequest`
- `WorkspaceDto`
- `AuditLogDto`
- Validation rules

</td>
</tr>
</table>

### 📚 Key Design Patterns:

- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer and validation
- **Transaction Management** - ACID compliance
- **Dependency Injection** - Loose coupling via Spring

---

## 🎯 Feature Flag Rollout Logic

The service uses an **improved deterministic hash-based approach** for automatic feature flag distribution across workspaces with guaranteed consistency during rollout changes.

### ⚙️ How It Works:

<table>
<tr>
<td>

#### 1️⃣ **Creation/Update**
When a feature flag is created or its rollout percentage is updated, the system automatically calculates which workspaces should have it enabled.

</td>
</tr>
<tr>
<td>

#### 2️⃣ **Deterministic Workspace Selection**
```kotlin
// Sort all workspaces by a deterministic hash
val sortedWorkspaces = workspaces.sortedBy { workspace ->
    abs((featureFlagId + workspaceId).hashCode())
}

// Calculate exact number to enable
val targetCount = (totalWorkspaces * percentage / 100.0).toInt()

// Enable the first 'targetCount' workspaces in sorted order
```

</td>
</tr>
<tr>
<td>

#### 3️⃣ **Stability Guarantees**
- **Increasing percentage (30% → 50%)**: Previously enabled workspaces stay enabled + new ones get enabled
- **Decreasing percentage (50% → 30%)**: Only the top-ranked workspaces stay enabled, others get disabled
- **Same workspace-flag combination**: Always produces the same hash/rank

</td>
</tr>
<tr>
<td>

#### 4️⃣ **Region Filtering**
Before rollout calculation, workspaces are filtered by region:
- Feature flag with `regions: ["WESTEUROPE"]` only affects workspaces in WESTEUROPE
- Feature flag with `regions: ["ALL"]` affects all workspaces globally

</td>
</tr>
</table>

### 🎚️ Special Cases:

| Rollout % | Behavior | Description |
|-----------|----------|-------------|
| **0%** | 🔴 Disabled | Feature flag disabled for all workspaces |
| **1-99%** | 🟡 Partial | Deterministically distributed based on workspace ranking |
| **100%** | 🟢 Enabled | Feature flag enabled for all workspaces in target regions |

### 💡 Example Scenario:

> **1000 workspaces in WESTEUROPE, feature flag targeting WESTEUROPE**
>
> **At 30% rollout:**
> - System deterministically ranks all 1000 workspaces
> - Enables exactly 300 workspaces (those ranked 0-299)
> - These 300 workspaces are always the same for this flag
>
> **Increase to 50%:**
> - The original 300 stay enabled (ranks 0-299)
> - Additional 200 get enabled (ranks 300-499)
> - Total: 500 workspaces enabled
>
> **Decrease to 20%:**
> - Only top 200 stay enabled (ranks 0-199)
> - The other 100 get disabled (ranks 200-299)
> - Total: 200 workspaces enabled

### 🔄 Algorithm Benefits:

✅ **Deterministic** - Same workspace always gets same treatment at a given percentage
✅ **Stable on increase** - Previously enabled workspaces stay enabled
✅ **Minimal churn on decrease** - Only necessary workspaces get disabled
✅ **Region-aware** - Respects regional boundaries
✅ **Exact percentages** - Achieves precise target counts

---

## 🌍 Multi-Region Support

The system provides comprehensive multi-region functionality for both workspaces and feature flags.

### 🏢 Workspace Regions

Each workspace is assigned to a specific Azure region:

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "EU Production",
  "type": "PRODUCTION",
  "region": "WESTEUROPE"
}
```

### 🎚️ Feature Flag Region Targeting

Feature flags can target one or more regions, or be deployed globally:

```json
{
  "name": "new-feature",
  "team": "platform",
  "regions": ["WESTEUROPE", "EASTUS"],
  "rolloutPercentage": 50
}
```

### 🔍 Region Filtering Logic

When retrieving feature flags for a workspace:

1. **Check workspace region** - Determine the workspace's region (e.g., WESTEUROPE)
2. **Filter by regions** - Only return flags where:
   - `regions` contains "ALL", OR
   - `regions` contains the workspace's region
3. **Apply rollout** - Check if workspace is in the rollout percentage for matching flags

### 🌐 Region Matching Examples:

| Feature Flag Regions | Workspace Region | Match? | Explanation |
|---------------------|------------------|--------|-------------|
| `["ALL"]` | WESTEUROPE | ✅ Yes | ALL matches any region |
| `["WESTEUROPE"]` | WESTEUROPE | ✅ Yes | Exact region match |
| `["EASTUS"]` | WESTEUROPE | ❌ No | Region mismatch |
| `["WESTEUROPE", "EASTUS"]` | WESTEUROPE | ✅ Yes | Multi-region includes workspace |
| `["WESTEUROPE", "EASTUS"]` | JAPANEAST | ❌ No | Workspace region not in target list |
| `["ALL"]` | CANADACENTRAL | ✅ Yes | ALL matches any region |

**Important:** Region filtering occurs at the service layer, not in the database. The `region` field in the `workspaces` table is a VARCHAR, not an enum, allowing flexibility. The Region enum is only used for validation in the application layer.

### 🎯 Use Cases:

- **Global rollout**: Set `regions: ["ALL"]` to target all workspaces
- **Regional rollout**: Set `regions: ["WESTEUROPE"]` for EU-specific features
- **Multi-region rollout**: Set `regions: ["WESTEUROPE", "EASTUS", "UKSOUTH"]` for multiple regions
- **Regional testing**: Test features in specific regions before global deployment

---

## 📝 Audit Logging

Comprehensive audit logging tracks all feature flag lifecycle events.

### 📊 What's Logged:

Every CREATE, UPDATE, and DELETE operation on feature flags is automatically logged with:

- **Operation Type** - CREATE, UPDATE, or DELETE
- **Feature Flag Details** - ID and name
- **Team** - Team responsible for the flag
- **Old Values** - Previous state (for UPDATE operations)
- **New Values** - New state (for CREATE and UPDATE operations)
- **Changed By** - User who made the change (future enhancement)
- **Timestamp** - When the operation occurred

### 🔍 Audit Log Entry Example:

```json
{
  "id": "789e1011-e89b-12d3-a456-426614174002",
  "featureFlagId": "456e7890-e89b-12d3-a456-426614174001",
  "featureFlagName": "new-checkout-flow",
  "operation": "UPDATE",
  "team": "checkout-team",
  "oldValues": "{\"rolloutPercentage\":25,\"regions\":[\"WESTEUROPE\"],\"description\":\"Initial rollout\"}",
  "newValues": "{\"rolloutPercentage\":50,\"regions\":[\"ALL\"],\"description\":\"Expanded rollout\"}",
  "changedBy": null,
  "timestamp": "2024-01-01T11:00:00"
}
```

### 📋 Querying Audit Logs:

The system provides flexible audit log querying:

```bash
# All audit logs
GET /api/audit-logs

# Filter by feature flag
GET /api/audit-logs?featureFlagId={id}

# Filter by team
GET /api/audit-logs?team=checkout-team

# Filter by operation
GET /api/audit-logs?operation=UPDATE

# Combine filters
GET /api/audit-logs?team=checkout-team&operation=CREATE
```

### 🎯 Use Cases:

- **Compliance** - Track who changed what and when
- **Debugging** - Understand flag history and changes
- **Analytics** - Analyze team activity and flag lifecycle
- **Rollback** - See previous configurations for potential rollback
- **Accountability** - Maintain audit trail for all operations

### 🔐 Future Enhancement:

Currently, `changedBy` is nullable (set to `null`). Future versions will integrate with authentication to capture the actual user making changes.

---

## 💻 Web UI

The application includes a modern, feature-rich web interface built with **Vue.js 3**.

### 🌐 Access

**Location:** http://localhost:8080

### ✨ UI Features

<table>
<tr>
<td width="50%">

#### 🏢 Workspace Management
- View all workspaces with region information
- Filter and search workspaces
- See workspace-specific feature flag status

</td>
<td width="50%">

#### 🎚️ Feature Flag Management
- Create, edit, and delete feature flags
- Visual rollout percentage sliders
- Multi-region selection with checkboxes
- Team-based organization
- **Region breakdown view** - See enabled/total workspace counts per region
- Paginated enabled workspaces view with search

</td>
</tr>
<tr>
<td width="50%">

#### 📝 Audit Log Viewer
- View complete audit trail
- Filter by team, operation, or feature flag
- See before/after values for changes
- Timestamp-sorted history

</td>
<td width="50%">

#### 🎨 Modern UX
- Responsive design
- Real-time updates
- Toast notifications
- Modal dialogs
- Loading states
- Error handling

</td>
</tr>
</table>

### 📁 Frontend Architecture:

Built with **Vue 3 Composition API**, **Vite**, and **Tailwind CSS** following an **Atomic Design** pattern.

| Directory | Description |
|-----------|-------------|
| `src/App.vue` | 🎯 Main application component with routing logic and state management |
| `src/main.js` | 🚀 Application entry point |
| `src/services/api-service.js` | 🌐 Axios-based API client with error handling |
| `src/components/atoms/` | ⚛️ Basic UI elements (Button, Badge, Input, Switch, Card) |
| `src/components/molecules/` | 🔬 Composite components (SearchBar, FormGroup, Pagination, StatCard, FilterBar) |
| `src/components/organisms/` | 🧬 Complex components (FioriTable, FioriModal, FioriNotification, FeatureFlagForm, WorkspaceFeatureFlagForm, DashboardCharts) |
| `src/components/layouts/` | 🏗️ Layout components (FioriShellLayout, PageLayout) |
| `src/composables/` | 🪝 Reusable Vue composables (useApiCall, useFormState, useInfiniteScroll, useDebouncedSearch) |
| `src/assets/styles/tailwind.css` | 🎨 Tailwind CSS imports and custom styles |
| `tailwind.config.js` | ⚙️ Tailwind configuration with custom theme |

### 🎨 Design System:

Built with **SAP Fiori-inspired** design system and **Tailwind CSS**:

- **UI Framework** - Custom Fiori-inspired component library
- **CSS Framework** - Tailwind CSS 3 with custom configuration
- **Typography** - 72 font family with system fallbacks
- **Color Palette** - Primary (SAP Blue), success, warning, danger with full shade ranges
- **Spacing** - Tailwind's default spacing scale (4px base unit)
- **Components** - Atomic design pattern (Atoms, Molecules, Organisms)
- **Icons** - Lucide Vue Next icon library
- **Charts** - Chart.js for data visualization (rollout distribution, team statistics)
- **Responsive** - Mobile-first design with Tailwind breakpoints
- **Animations** - Tailwind CSS transitions and custom fade-in animations

---

## 🛠️ Development

### 📂 Project Structure

```
.
├── src/main/
│   ├── kotlin/com/featureflag/
│   │   ├── controller/              # 📡 REST API endpoints
│   │   ├── dto/                     # 📦 Data Transfer Objects
│   │   ├── entity/                  # 🗃️ JPA entities
│   │   ├── exception/               # ⚠️ Error handling
│   │   ├── repository/              # 💾 Data access layer
│   │   └── service/                 # ⚙️ Business logic
│   └── resources/
│       ├── application.yml          # ⚙️ Configuration
│       ├── db/migration/            # 📊 Flyway database migrations
│       └── static/                  # 🌐 Built frontend files (copied during build)
├── src/test/                        # 🧪 Backend test suites
├── frontend/
│   ├── src/                         # Vue 3 application source
│   ├── dist/                        # Vite build output (copied to static/)
│   ├── package.json                 # Frontend dependencies
│   └── vite.config.js               # Vite configuration
├── bruno-collection/                # Bruno API testing collection
├── build.gradle.kts                 # Gradle build with frontend integration
└── docker-compose.yml               # Docker services configuration
```

### 🔨 Building

```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Run tests only
./gradlew test

# Create Docker image
docker build -t feature-flag:latest .

# Build and run with Docker Compose
docker-compose up --build
```

### 🐛 Debugging

```bash
# Run with debug logging
./gradlew bootRun --args='--logging.level.com.featureflag=DEBUG'

# Run in debug mode (IDE attach on port 5005)
./gradlew bootRun --debug-jvm
```

---

## ⚠️ Known Limitations

| Issue | Description | Workaround |
|-------|-------------|------------|
| 🔒 **No Authentication** | No user authentication or authorization implemented | Use behind a secure proxy/gateway |
| 👤 **Audit Logging** | `changedBy` field is always null - no user tracking | Manual tracking or future auth integration |

---

## 🚧 Future Enhancements

### High Priority
- [ ] 🔒 Add authentication and authorization (OAuth2, JWT)
- [ ] 👤 Track user identity in audit logs (`changedBy` field)
- [ ] 📊 Add metrics dashboard for flag usage analytics

### Medium Priority
- [ ] ⏰ Add feature flag scheduling (enable/disable at specific times)
- [ ] 📦 Implement feature flag versioning and rollback

---

## 📄 License

This project is for **educational and demonstration purposes**.

---

<div align="center">

**[⬆ back to top](#-feature-flag-management-service)**

</div>
