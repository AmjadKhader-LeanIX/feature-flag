const apiService = {
    baseURL: '/api',

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
            });
            return response.data;
        } catch (error) {
            console.error('API request failed:', error);
            const message = error.response?.data?.message || error.message || 'An error occurred';
            throw new Error(message);
        }
    },

    getFeatureFlags() {
        return this.request('/feature-flags');
    },

    getFeatureFlagsByTeam(team) {
        return this.request(`/feature-flags/team/${encodeURIComponent(team)}`);
    },

    createFeatureFlag(data) {
        return this.request('/feature-flags', {
            method: 'POST',
            data
        });
    },

    updateFeatureFlag(id, data) {
        return this.request(`/feature-flags/${id}`, {
            method: 'PUT',
            data
        });
    },

    deleteFeatureFlag(id) {
        return this.request(`/feature-flags/${id}`, {
            method: 'DELETE'
        });
    },

    getAuditLogs(params = {}) {
        const queryString = new URLSearchParams(params).toString();
        return this.request(`/audit-logs${queryString ? '?' + queryString : ''}`);
    },

    getAuditLogsByFeatureFlagId(featureFlagId) {
        return this.request(`/audit-logs/feature-flag/${featureFlagId}`);
    },

    getWorkspaces() {
        return this.request('/workspaces');
    },

    updateWorkspaceFeatureFlags(featureFlagId, data) {
        return this.request(`/feature-flags/${featureFlagId}/workspaces`, {
            method: 'PUT',
            data
        });
    },

    // Get all workspaces that have a feature flag enabled
    getEnabledWorkspacesForFeatureFlag(featureFlagId) {
        return this.request(`/feature-flags/${featureFlagId}/enabled-workspaces`);
    },

    // Get all enabled feature flags for a workspace
    getEnabledFeatureFlagsForWorkspace(workspaceId) {
        return this.request(`/workspaces/${workspaceId}/enabled-feature-flags`);
    },
};
