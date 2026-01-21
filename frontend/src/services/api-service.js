import axios from 'axios'

const API_BASE_URL = '/api'

/**
 * API Service for Feature Flag Manager
 * Handles all API requests to the backend
 */
const apiService = {
  baseURL: API_BASE_URL,

  async request(endpoint, options = {}) {
    try {
      const response = await axios({
        url: `${this.baseURL}${endpoint}`,
        method: options.method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...options.headers
        },
        ...options
      })
      return response.data
    } catch (error) {
      console.error('API request failed:', error)
      const message = error.response?.data?.message || error.message || 'An error occurred'
      throw new Error(message)
    }
  },

  // Feature Flags
  getFeatureFlags() {
    return this.request('/feature-flags')
  },

  getFeatureFlagsByTeam(team) {
    return this.request(`/feature-flags/team/${encodeURIComponent(team)}`)
  },

  createFeatureFlag(data) {
    return this.request('/feature-flags', {
      method: 'POST',
      data
    })
  },

  updateFeatureFlag(id, data) {
    return this.request(`/feature-flags/${id}`, {
      method: 'PUT',
      data
    })
  },

  deleteFeatureFlag(id) {
    return this.request(`/feature-flags/${id}`, {
      method: 'DELETE'
    })
  },

  // Audit Logs
  getAuditLogs(page = 0, size = 100, searchTerm = '') {
    const search = searchTerm ? `&search=${encodeURIComponent(searchTerm)}` : ''
    return this.request(`/audit-logs?paginated=true&page=${page}&size=${size}${search}`)
  },

  getAuditLogsByFeatureFlagId(featureFlagId) {
    return this.request(`/audit-logs/feature-flag/${featureFlagId}`)
  },

  // Workspaces
  getWorkspaces(page = 0, size = 100, searchTerm = '') {
    const search = searchTerm ? `&search=${encodeURIComponent(searchTerm)}` : ''
    return this.request(`/workspaces?paginated=true&page=${page}&size=${size}${search}`)
  },

  updateWorkspaceFeatureFlags(featureFlagId, data) {
    return this.request(`/feature-flags/${featureFlagId}/workspaces`, {
      method: 'PUT',
      data
    })
  },

  // Get all workspaces that have a feature flag enabled
  getEnabledWorkspacesForFeatureFlag(featureFlagId, page = 0, size = 100, searchTerm = '') {
    const search = searchTerm ? `&search=${encodeURIComponent(searchTerm)}` : ''
    return this.request(`/feature-flags/${featureFlagId}/enabled-workspaces?paginated=true&page=${page}&size=${size}${search}`)
  },

  // Get all enabled feature flags for a workspace
  getEnabledFeatureFlagsForWorkspace(workspaceId, page = 0, size = 100, searchTerm = '') {
    const search = searchTerm ? `&search=${encodeURIComponent(searchTerm)}` : ''
    return this.request(`/workspaces/${workspaceId}/enabled-feature-flags?paginated=true&page=${page}&size=${size}${search}`)
  },

  // Get workspace counts by region for a feature flag
  getWorkspaceCountsByRegion(featureFlagId) {
    return this.request(`/feature-flags/${featureFlagId}/workspace-counts-by-region`)
  }
}

export default apiService
