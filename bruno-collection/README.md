# Feature Flag API - Bruno Collection

This Bruno collection contains all API endpoints for the Feature Flag Management System.

## Getting Started

### Prerequisites

1. Install [Bruno](https://www.usebruno.com/) - A fast and git-friendly API client
2. Ensure the Feature Flag application is running on `http://localhost:8080`

### How to Use

1. Open Bruno and click "Open Collection"
2. Navigate to this `bruno-collection` folder and select it
3. The collection will be loaded with all available API endpoints organized by category

### Environment Variables

The collection includes a `Local` environment with the following variable:
- `baseUrl`: http://localhost:8080

You can create additional environments (e.g., Dev, Staging, Production) by copying the `Local.bru` file in the `environments` folder.

## API Endpoints Overview

### Feature Flags (12 endpoints)

1. **Get All Feature Flags** - `GET /api/feature-flags`
   - Retrieves all feature flags in the system

2. **Get Feature Flag By ID** - `GET /api/feature-flags/:id`
   - Retrieves a specific feature flag by UUID

3. **Get Feature Flags By Workspace** - `GET /api/feature-flags/workspace/:workspaceId`
   - Retrieves all feature flags for a specific workspace

4. **Get Feature Flags By Team** - `GET /api/feature-flags/team/:team`
   - Retrieves all feature flags for a specific team

5. **Search Feature Flags** - `GET /api/feature-flags/search?name=<search-term>`
   - Search feature flags by name (partial match)

6. **Create Feature Flag** - `POST /api/feature-flags`
   - Creates a new feature flag
   - Required fields: `name`, `team`
   - Optional fields: `description`, `regions`, `rolloutPercentage`

7. **Update Feature Flag** - `PUT /api/feature-flags/:id`
   - Updates an existing feature flag
   - All fields required in request body

8. **Delete Feature Flag** - `DELETE /api/feature-flags/:id`
   - Deletes a feature flag by UUID

9. **Update Workspace Feature Flags** - `PUT /api/feature-flags/:id/workspaces`
   - Enable or disable a feature flag for specific workspaces
   - Body: `{ "workspaceIds": ["uuid1", "uuid2"], "enabled": true }`

10. **Get Enabled Workspaces** - `GET /api/feature-flags/:id/enabled-workspaces`
    - Retrieves paginated list of workspaces where the feature flag is enabled
    - Supports pagination and search query parameters

11. **Get Workspace Counts By Region** - `GET /api/feature-flags/:id/workspace-counts-by-region`
    - Get a summary of enabled and total workspace counts per region
    - Returns array with `region`, `enabledCount`, and `totalCount` for each region

### Workspaces (3 endpoints)

1. **Get All Workspaces** - `GET /api/workspaces`
   - Retrieves all workspaces in the system

2. **Get Workspace By ID** - `GET /api/workspaces/:id`
   - Retrieves a specific workspace by UUID

3. **Get Enabled Feature Flags** - `GET /api/workspaces/:id/enabled-feature-flags`
   - Retrieves all feature flags enabled for a specific workspace

### Audit Logs (5 endpoints)

1. **Get All Audit Logs** - `GET /api/audit-logs`
   - Retrieves all audit logs
   - Supports optional query parameters for filtering

2. **Get Audit Logs By Feature Flag** - `GET /api/audit-logs/feature-flag/:featureFlagId`
   - Retrieves audit logs for a specific feature flag

3. **Get Audit Logs By Team** - `GET /api/audit-logs/team/:team`
   - Retrieves audit logs for a specific team

4. **Get Audit Logs By Operation** - `GET /api/audit-logs/operation/:operation`
   - Retrieves audit logs for a specific operation type
   - Valid operations: `CREATE`, `UPDATE`, `DELETE`

5. **Get Audit Logs With Filters** - `GET /api/audit-logs?<filters>`
   - Retrieves audit logs with multiple filters
   - Available filters: `featureFlagId`, `team`, `operation`

## Common Variables

The following variables are used throughout the collection:
- `{{baseUrl}}` - Base URL of the API (default: http://localhost:8080)
- `{{featureFlagId}}` - UUID of a feature flag
- `{{workspaceId}}` - UUID of a workspace

You can set these as environment variables or replace them directly in the requests.

## Example Request Bodies

### Create Feature Flag
```json
{
  "name": "new-feature",
  "description": "New feature flag",
  "team": "platform",
  "regions": ["ALL"],
  "rolloutPercentage": 0
}
```

### Update Feature Flag
```json
{
  "name": "updated-feature",
  "description": "Updated description",
  "team": "platform",
  "regions": ["US", "EU"],
  "rolloutPercentage": 50
}
```

### Update Workspace Feature Flags
```json
{
  "workspaceIds": [
    "00000000-0000-0000-0000-000000000001",
    "00000000-0000-0000-0000-000000000002"
  ],
  "enabled": true
}
```

## Available Regions

The following regions are supported:
- `ALL` - All regions (default)
- `US` - United States
- `EU` - Europe
- `APAC` - Asia-Pacific

## Testing Workflow

A typical testing workflow might be:

1. **Get all workspaces** to find workspace IDs
2. **Create a feature flag** with default settings
3. **Update the feature flag** to enable it for specific workspaces
4. **Search or filter** feature flags to verify changes
5. **Check audit logs** to see the history of changes
6. **Clean up** by deleting test feature flags

## Notes

- All UUID path parameters should be replaced with actual UUIDs from your database
- The API uses standard HTTP status codes (200, 201, 204, 400, 404, etc.)
- All timestamps are in ISO-8601 format
- The collection does not include authentication as the current API doesn't require it

## Contributing

To add new endpoints:
1. Create a new `.bru` file in the appropriate folder
2. Use the existing files as templates
3. Update this README with the new endpoint documentation
