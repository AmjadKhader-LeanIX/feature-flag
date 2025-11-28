<div align="center">

# 🚀 Feature Flag Management Service

### A modern, multi-tenant feature flag management system

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-green?style=flat-square&logo=vue.js)](https://vuejs.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue?style=flat-square&logo=docker)](https://www.docker.com/)

Built with Kotlin, Spring Boot, and PostgreSQL, providing multi-tenant workspace support with team-based feature flags and automatic rollout percentage control.

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
| **Frontend** | Vue.js 3, Axios |

---

## ✨ Features

### 🎯 Core Functionality

<table>
<tr>
<td width="50%">

#### 🏢 Workspace Management
Multi-tenant architecture with complete workspace isolation

</td>
<td width="50%">

#### 🎚️ Feature Flag Management
Team-based feature flags with rollout percentages (0-100%)

</td>
</tr>
<tr>
<td width="50%">

#### 🔄 Automatic Rollout
Consistent hash-based distribution of feature flags to workspaces

</td>
<td width="50%">

#### 💻 Web UI
Modern Vue.js interface for managing feature flags and workspaces

</td>
</tr>
</table>

---

## 📡 API Endpoints

### 🏢 Workspaces

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/workspaces` | 📋 List all workspaces |
| `GET` | `/api/workspaces/{id}` | 🔍 Get workspace by ID |

### 🎚️ Feature Flags

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/feature-flags` | 📋 List all feature flags |
| `POST` | `/api/feature-flags` | ➕ Create feature flag |
| `GET` | `/api/feature-flags/{id}` | 🔍 Get feature flag by ID |
| `PUT` | `/api/feature-flags/{id}` | ✏️ Update feature flag |
| `DELETE` | `/api/feature-flags/{id}` | ❌ Delete feature flag |
| `GET` | `/api/feature-flags/workspace/{workspaceId}` | 🏢 Get flags for workspace |
| `GET` | `/api/feature-flags/team/{team}` | 👥 Get flags by team |
| `GET` | `/api/feature-flags/search?name={name}` | 🔎 Search feature flags |

---

## 🗄️ Database Schema

The service uses PostgreSQL with the following main tables:

| Table | Description | Key Fields |
|-------|-------------|------------|
| **`workspaces`** | 🏢 Multi-tenant workspaces | `id`, `name`, `type`, `created_at`, `updated_at` |
| **`feature_flag`** | 🎚️ Feature flag definitions | `id`, `name`, `description`, `team`, `rollout_percentage` |
| **`workspace_feature_flag`** | 🔗 Workspace-flag relationships | `workspace_id`, `feature_flag_id`, `enabled` |

<details>
<summary>📊 View ERD Diagram</summary>

```
┌─────────────────┐         ┌──────────────────────────┐         ┌─────────────────┐
│   workspaces    │         │ workspace_feature_flag   │         │  feature_flag   │
├─────────────────┤         ├──────────────────────────┤         ├─────────────────┤
│ id (PK)         │◄────────│ workspace_id (FK)        │         │ id (PK)         │
│ name            │         │ feature_flag_id (FK)     │────────►│ name            │
│ type            │         │ enabled                  │         │ description     │
│ created_at      │         │ created_at               │         │ team            │
│ updated_at      │         │ updated_at               │         │ rollout_%       │
└─────────────────┘         └──────────────────────────┘         │ created_at      │
                                                                  │ updated_at      │
                                                                  └─────────────────┘
```

</details>

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

#### 1️⃣ **Start PostgreSQL:**
```bash
docker-compose up postgres -d
```

#### 2️⃣ **Run the application:**
```bash
./gradlew bootRun
```

### ⚙️ Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/feature-flag?currentSchema=public` | 🐘 Database URL |
| `SPRING_DATASOURCE_USERNAME` | `feature-flag` | 👤 Database username |
| `SPRING_DATASOURCE_PASSWORD` | `feature-flag123` | 🔒 Database password |
| `SERVER_PORT` | `8080` | 🌐 Application port |

---
---

## 📝 API Usage Examples

### 🏢 Get All Workspaces
```bash
curl http://localhost:8080/api/workspaces
```

### 🔍 Get Workspace by ID
```bash
curl http://localhost:8080/api/workspaces/{workspace-id}
```

### ➕ Create a Feature Flag
```bash
curl -X POST http://localhost:8080/api/feature-flags \
  -H "Content-Type: application/json" \
  -d '{
    "name": "new-checkout-flow",
    "description": "Enable new checkout flow",
    "team": "checkout-team",
    "rolloutPercentage": 25
  }'
```

> **💡 Note:** When a feature flag is created or updated, the system automatically calculates which workspaces should have it enabled based on the rollout percentage using consistent hashing.

### ✏️ Update a Feature Flag
```bash
curl -X PUT http://localhost:8080/api/feature-flags/{feature-flag-id} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "new-checkout-flow",
    "description": "Enable new checkout flow for more users",
    "team": "checkout-team",
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

> **ℹ️** This returns only the feature flags that are enabled for the specified workspace based on rollout percentages.

### 🔎 Search Feature Flags
```bash
curl "http://localhost:8080/api/feature-flags/search?name=checkout"
```

### ❌ Delete a Feature Flag
```bash
curl -X DELETE http://localhost:8080/api/feature-flags/{feature-flag-id}
```

---
---

## 🧪 Testing

### ▶️ Run All Tests
```bash
./gradlew test
```

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

The application follows a **layered architecture**:

```
┌─────────────┐
│  Vue.js UI  │  💻 Frontend
└──────┬──────┘
       │
┌──────▼──────┐
│  REST API   │  🌐 HTTP Layer
└──────┬──────┘
       │
┌──────▼──────┐
│ Controllers │  📡 Request Handling
└──────┬──────┘
       │
┌──────▼──────┐
│  Services   │  ⚙️ Business Logic
└──────┬──────┘
       │
┌──────▼──────┐
│Repositories │  💾 Data Access
└──────┬──────┘
       │
┌──────▼──────┐
│  PostgreSQL │  🐘 Database
└─────────────┘
```

### 🔧 Components:

<table>
<tr>
<td width="33%">

**🎨 Frontend**
- Vue.js 3 SPA
- Axios for API calls
- Real-time updates

</td>
<td width="33%">

**📡 Controllers**
- REST endpoints
- Jakarta validation
- Request mapping

</td>
<td width="33%">

**⚙️ Services**
- Business logic
- Rollout calculation
- Transactions

</td>
</tr>
<tr>
<td width="33%">

**💾 Repositories**
- Spring Data JPA
- Custom queries
- Database access

</td>
<td width="33%">

**📦 Entities**
- JPA entities
- Relationships
- Constraints

</td>
<td width="33%">

**📤 DTOs**
- Request/Response objects
- Validation rules
- Data transfer

</td>
</tr>
</table>

### 📚 Key Entities:

- **`Workspace`** - Multi-tenant workspaces
- **`FeatureFlag`** - Feature flag definitions
- **`WorkspaceFeatureFlag`** - Join table with enabled status

### 📋 DTOs:

- **`FeatureFlagDto`** - Feature flag data
- **`CreateFeatureFlagRequest`** - Create request
- **`UpdateFeatureFlagRequest`** - Update request
- **`WorkspaceDto`** - Workspace data

---
---

## 🎯 Feature Flag Rollout Logic

The service uses a **consistent hash-based approach** for automatic feature flag distribution:

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

#### 2️⃣ **Consistent Hashing**
```
Hash = (feature_flag_id + workspace_id).hashCode()
Bucket = abs(Hash) % 100
Enabled = Bucket < rollout_percentage
```

</td>
</tr>
<tr>
<td>

#### 3️⃣ **Consistency Guarantee**
The same feature flag will always be enabled/disabled for the same workspace at a given rollout percentage.

</td>
</tr>
</table>

### 🎚️ Special Cases:

| Rollout % | Behavior | Description |
|-----------|----------|-------------|
| **0%** | 🔴 Disabled | Feature flag disabled for all workspaces |
| **1-99%** | 🟡 Partial | Distributed based on consistent hashing |
| **100%** | 🟢 Enabled | Feature flag enabled for all workspaces |

### 💡 Example:

> If a feature flag has a **25% rollout**:
> - System hashes each `(feature_flag_id + workspace_id)` combination
> - ~25% of workspaces will have the flag enabled
> - The same workspaces will **always** be in that 25% (deterministic)

---
---

## 💻 Web UI

The application includes a modern web interface built with **Vue.js 3**:

### 🌐 Access

**Location:** http://localhost:8080

### ✨ Features

<table>
<tr>
<td width="50%">

✅ View and manage workspaces
✅ Create, edit, and delete feature flags
✅ Control rollout percentages with visual feedback

</td>
<td width="50%">

✅ Search and filter functionality
✅ Real-time updates
✅ Modern, responsive design

</td>
</tr>
</table>

### 📁 Static Resources:

| File | Description |
|------|-------------|
| `index.html` | 📄 Main HTML page |
| `app.js` | ⚛️ Vue.js application logic |
| `styles.css` | 🎨 Styling |

---

## 🛠️ Development

### 📊 Database Migrations

Database schema changes are managed with **Flyway migrations** in `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| `V1__Initial_schema.sql` | 🏗️ Initial database schema |
| `V2__Add_timestamps_to_feature_flag.sql` | ⏰ Added timestamps to feature flags |

### 📂 Project Structure

```
src/
├── main/
│   ├── kotlin/com/featureflag/
│   │   ├── FeatureFlagApplication.kt       # 🚀 Main application
│   │   ├── controller/
│   │   │   ├── FeatureFlagController.kt    # 🎚️ Feature flag endpoints
│   │   │   └── WorkspaceController.kt      # 🏢 Workspace endpoints
│   │   ├── dto/
│   │   │   ├── FeatureFlagDto.kt           # 📦 Feature flag DTOs
│   │   │   └── WorkspaceDto.kt             # 📦 Workspace DTOs
│   │   ├── entity/
│   │   │   ├── FeatureFlag.kt              # 🗃️ Feature flag entity
│   │   │   ├── Workspace.kt                # 🗃️ Workspace entity
│   │   │   └── WorkspaceFeatureFlag.kt     # 🗃️ Join table entity
│   │   ├── exception/
│   │   │   └── ...                         # ⚠️ Exception handlers
│   │   ├── repository/
│   │   │   └── ...                         # 💾 JPA repositories
│   │   └── service/
│   │       ├── FeatureFlagService.kt       # ⚙️ Feature flag logic
│   │       └── WorkspaceService.kt         # ⚙️ Workspace logic
│   └── resources/
│       ├── application.yml                 # ⚙️ Configuration
│       ├── db/migration/                   # 📊 Database migrations
│       └── static/                         # 🌐 Web UI files
└── test/
    └── kotlin/com/featureflag/
        └── ...                             # 🧪 Tests
```

### ➕ Adding New Features

| Step | Action |
|------|--------|
| 1️⃣ | Create/update entities in `entity/` package |
| 2️⃣ | Add repository methods using Spring Data JPA |
| 3️⃣ | Implement business logic in `service/` package |
| 4️⃣ | Create REST endpoints in `controller/` package |
| 5️⃣ | Add DTOs for request/response in `dto/` package |
| 6️⃣ | Write unit and integration tests |
| 7️⃣ | Update database migrations if needed |
| 8️⃣ | Update this documentation |

### 🔨 Building

```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Create Docker image
docker build -t feature-flag:latest .
```

---

## ⚠️ Known Limitations

| Issue | Description |
|-------|-------------|
| 🔄 **Rollout Logic** | The rollout percentage logic currently resets all workspace associations when updating, which may cause unintended changes for workspaces that had the flag enabled |
| 🔒 **Authentication** | No authentication/authorization implemented |
| 📝 **Workspace API** | Workspace creation and management endpoints are read-only via API |

---

## 🚧 Future Enhancements

- [ ] 🔄 Improve rollout logic to handle incremental updates
- [ ] 🔒 Add authentication and authorization
- [ ] ✏️ Implement workspace CRUD operations (create, update, delete)
- [ ] ⏰ Add feature flag scheduling (enable/disable at specific times)
- [ ] 📝 Add audit logging for all changes
- [ ] 📦 Implement feature flag versioning
- [ ] 📊 Add metrics and analytics for flag usage
- [ ] 👤 Add user-specific overrides in addition to workspace-based rollout

---

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

| Step | Action |
|------|--------|
| 1️⃣ | Follow the existing code style |
| 2️⃣ | Add comprehensive tests for new functionality |
| 3️⃣ | Update API documentation and this README |
| 4️⃣ | Ensure all tests pass: `./gradlew test` |
| 5️⃣ | Run security checks: `./gradlew dependencyCheckAnalyze` |

---

## 📄 License

This project is for **educational and demonstration purposes**.

---

<div align="center">

**[⬆ back to top](#-feature-flag-management-service)**

</div>


