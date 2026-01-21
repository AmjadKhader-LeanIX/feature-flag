const WorkspaceFeatureFlagComponent = {
    props: {
        featureFlag: {
            type: Object,
            required: true
        },
        loading: {
            type: Boolean,
            default: false
        }
    },

    setup(props, { emit }) {
        const searchTerm = ref('');
        const searchResults = ref([]);
        const loadingSearch = ref(false);
        const pickedWorkspaces = ref([]);
        const excludedWorkspaces = ref([]);
        const rolloutPercentage = ref(0);
        const initialRolloutPercentage = ref(0);
        const selectedRegion = ref(null);
        const showDropdown = ref(false);

        const excludeSearchTerm = ref('');
        const excludeSearchResults = ref([]);
        const loadingExcludeSearch = ref(false);
        const showExcludeDropdown = ref(false);

        const hasRolloutChanged = computed(() => rolloutPercentage.value !== initialRolloutPercentage.value);
        const pickedCount = computed(() => pickedWorkspaces.value.length);
        const excludedCount = computed(() => excludedWorkspaces.value.length);

        // Search for workspaces
        const searchWorkspaces = async () => {
            if (!searchTerm.value || searchTerm.value.length < 2) {
                searchResults.value = [];
                showDropdown.value = false;
                return;
            }

            try {
                loadingSearch.value = true;
                const response = await apiService.getWorkspaces(0, 20, searchTerm.value);
                searchResults.value = response.content || [];
                showDropdown.value = searchResults.value.length > 0;
            } catch (error) {
                console.error('Failed to search workspaces:', error);
                searchResults.value = [];
                showDropdown.value = false;
            } finally {
                loadingSearch.value = false;
            }
        };

        // Pick a workspace from search results
        const pickWorkspace = (workspace) => {
            // Check if already picked
            const alreadyPicked = pickedWorkspaces.value.some(w => w.id === workspace.id);
            if (!alreadyPicked) {
                pickedWorkspaces.value.push(workspace);
            }
            // Clear search
            searchTerm.value = '';
            searchResults.value = [];
            showDropdown.value = false;
        };

        // Remove a picked workspace
        const removePickedWorkspace = (workspaceId) => {
            pickedWorkspaces.value = pickedWorkspaces.value.filter(w => w.id !== workspaceId);
        };

        // Search for workspaces to exclude
        const searchExcludeWorkspaces = async () => {
            if (!excludeSearchTerm.value || excludeSearchTerm.value.length < 2) {
                excludeSearchResults.value = [];
                showExcludeDropdown.value = false;
                return;
            }

            try {
                loadingExcludeSearch.value = true;
                const response = await apiService.getWorkspaces(0, 20, excludeSearchTerm.value);
                excludeSearchResults.value = response.content || [];
                showExcludeDropdown.value = excludeSearchResults.value.length > 0;
            } catch (error) {
                console.error('Failed to search workspaces:', error);
                excludeSearchResults.value = [];
                showExcludeDropdown.value = false;
            } finally {
                loadingExcludeSearch.value = false;
            }
        };

        // Pick a workspace to exclude
        const excludeWorkspace = (workspace) => {
            // Check if already excluded
            const alreadyExcluded = excludedWorkspaces.value.some(w => w.id === workspace.id);
            if (!alreadyExcluded) {
                excludedWorkspaces.value.push(workspace);
            }
            // Clear search
            excludeSearchTerm.value = '';
            excludeSearchResults.value = [];
            showExcludeDropdown.value = false;
        };

        // Remove an excluded workspace
        const removeExcludedWorkspace = (workspaceId) => {
            excludedWorkspaces.value = excludedWorkspaces.value.filter(w => w.id !== workspaceId);
        };

        // Don't auto-load enabled workspaces
        // Users should manually search and pick only the workspaces they want to prioritize
        // This allows percentage-based rollout to work correctly when decreasing percentage

        const handleSubmit = () => {
            if (!hasRolloutChanged.value && pickedWorkspaces.value.length === 0 && excludedWorkspaces.value.length === 0) {
                alert('Please change the rollout percentage, pick workspaces to prioritize, or exclude workspaces');
                return;
            }

            emit('submit', {
                workspaceIds: pickedWorkspaces.value.map(w => w.id),
                excludedWorkspaceIds: excludedWorkspaces.value.map(w => w.id),
                enabled: true,
                rolloutPercentage: rolloutPercentage.value,
                targetRegion: selectedRegion.value
            });
        };

        const handleCancel = () => {
            emit('cancel');
        };

        // Debounced search for pin
        let searchTimeout = null;
        watch(searchTerm, () => {
            if (searchTimeout) {
                clearTimeout(searchTimeout);
            }
            searchTimeout = setTimeout(() => {
                searchWorkspaces();
            }, 300);
        });

        // Debounced search for exclude
        let excludeSearchTimeout = null;
        watch(excludeSearchTerm, () => {
            if (excludeSearchTimeout) {
                clearTimeout(excludeSearchTimeout);
            }
            excludeSearchTimeout = setTimeout(() => {
                searchExcludeWorkspaces();
            }, 300);
        });

        // Initialize component on mount
        onMounted(async () => {
            rolloutPercentage.value = props.featureFlag.rolloutPercentage || 0;
            initialRolloutPercentage.value = props.featureFlag.rolloutPercentage || 0;
            // Start with empty picked list - users manually search and pick workspaces they want to prioritize
        });

        // Watch for feature flag changes
        watch(() => props.featureFlag, (newFlag) => {
            if (newFlag) {
                rolloutPercentage.value = newFlag.rolloutPercentage || 0;
                initialRolloutPercentage.value = newFlag.rolloutPercentage || 0;
            }
        });

        // Close dropdown when clicking outside
        const closeDropdown = () => {
            showDropdown.value = false;
        };

        return {
            searchTerm,
            searchResults,
            loadingSearch,
            pickedWorkspaces,
            excludedWorkspaces,
            excludeSearchTerm,
            excludeSearchResults,
            loadingExcludeSearch,
            showExcludeDropdown,
            rolloutPercentage,
            selectedRegion,
            showDropdown,
            hasRolloutChanged,
            pickedCount,
            excludedCount,
            pickWorkspace,
            removePickedWorkspace,
            excludeWorkspace,
            removeExcludedWorkspace,
            handleSubmit,
            handleCancel,
            closeDropdown
        };
    },

    template: `
        <div class="workspace-feature-flag-form">
            <div class="form-section">
                <div class="feature-flag-info">
                    <h3>{{ featureFlag.name }}</h3>
                    <p>{{ featureFlag.description || 'No description' }}</p>
                    <div class="flag-meta">
                        <span class="badge badge-info">{{ featureFlag.team }}</span>
                        <span class="badge badge-secondary">Current Rollout: {{ featureFlag.rolloutPercentage }}%</span>
                    </div>
                </div>
            </div>

            <div class="form-section">
                <label class="form-label" for="rollout-slider">Rollout Percentage</label>
                <div class="range-input">
                    <input
                        id="rollout-slider"
                        v-model.number="rolloutPercentage"
                        type="range"
                        min="0"
                        max="100"
                        :disabled="loading"
                    />
                    <span class="rollout-display">{{ rolloutPercentage }}%</span>
                </div>
            </div>

            <div class="form-section">
                <label class="form-label" for="region-selector">Target Region (Optional)</label>
                <select id="region-selector" v-model="selectedRegion" class="form-select" :disabled="loading">
                    <option :value="null">All Regions (apply percentage globally)</option>
                    <option value="WESTEUROPE">West Europe</option>
                    <option value="EASTUS">East US</option>
                    <option value="CANADACENTRAL">Canada Central</option>
                    <option value="AUSTRALIAEAST">Australia East</option>
                    <option value="GERMANYWESTCENTRAL">Germany West Central</option>
                    <option value="SWITZERLANDNORTH">Switzerland North</option>
                    <option value="UAENORTH">UAE North</option>
                    <option value="UKSOUTH">UK South</option>
                    <option value="BRAZILSOUTH">Brazil South</option>
                    <option value="SOUTHEASTASIA">Southeast Asia</option>
                    <option value="JAPANEAST">Japan East</option>
                    <option value="NORTHEUROPE">North Europe</option>
                </select>
            </div>

            <div class="form-section">
                <label class="form-label" for="workspace-search">Search and Pick Workspaces</label>
                <div style="position: relative;">
                    <div class="search-bar">
                        <i class="fas fa-search"></i>
                        <input
                            id="workspace-search"
                            type="text"
                            v-model="searchTerm"
                            placeholder="Search for workspaces to pick..."
                            @focus="searchWorkspaces"
                        />
                        <i v-if="loadingSearch" class="fas fa-spinner fa-spin" style="position: absolute; right: 12px;"></i>
                    </div>

                    <div v-if="showDropdown" class="search-dropdown" @click.stop>
                        <div
                            v-for="workspace in searchResults"
                            :key="workspace.id"
                            class="search-result-item"
                            @click="pickWorkspace(workspace)"
                        >
                            <div class="workspace-info">
                                <div class="workspace-name">
                                    <i class="fas fa-building"></i>
                                    {{ workspace.name }}
                                </div>
                                <div class="workspace-meta">
                                    <span v-if="workspace.type" class="badge badge-secondary">{{ workspace.type }}</span>
                                    <span v-if="workspace.region" class="badge badge-warning">
                                        <i class="fas fa-globe"></i>
                                        {{ workspace.region }}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div v-if="searchResults.length === 0 && !loadingSearch" class="search-result-item" style="color: var(--text-secondary); font-style: italic;">
                            No workspaces found
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="pickedWorkspaces.length > 0" class="form-section">
                <label class="form-label">Picked Workspaces ({{ pickedCount }})</label>
                <div class="picked-workspaces-list">
                    <div
                        v-for="workspace in pickedWorkspaces"
                        :key="workspace.id"
                        class="picked-workspace-item"
                    >
                        <div class="workspace-info">
                            <div class="workspace-name">
                                <i class="fas fa-building"></i>
                                {{ workspace.name }}
                            </div>
                            <div class="workspace-meta">
                                <span v-if="workspace.type" class="badge badge-secondary">{{ workspace.type }}</span>
                                <span v-if="workspace.region" class="badge badge-warning">
                                    <i class="fas fa-globe"></i>
                                    {{ workspace.region }}
                                </span>
                            </div>
                        </div>
                        <button
                            type="button"
                            class="btn-remove"
                            @click="removePickedWorkspace(workspace.id)"
                            title="Remove workspace"
                        >
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </div>
            </div>

            <div class="form-section">
                <label class="form-label" for="exclude-workspace-search">Exclude Workspaces (Always Disabled)</label>
                <div style="position: relative;">
                    <div class="search-bar">
                        <i class="fas fa-search"></i>
                        <input
                            id="exclude-workspace-search"
                            type="text"
                            v-model="excludeSearchTerm"
                            placeholder="Search for workspaces to exclude..."
                            @focus="searchExcludeWorkspaces"
                        />
                        <i v-if="loadingExcludeSearch" class="fas fa-spinner fa-spin" style="position: absolute; right: 12px;"></i>
                    </div>

                    <div v-if="showExcludeDropdown" class="search-dropdown" @click.stop>
                        <div
                            v-for="workspace in excludeSearchResults"
                            :key="workspace.id"
                            class="search-result-item"
                            @click="excludeWorkspace(workspace)"
                        >
                            <div class="workspace-info">
                                <div class="workspace-name">
                                    <i class="fas fa-building"></i>
                                    {{ workspace.name }}
                                </div>
                                <div class="workspace-meta">
                                    <span v-if="workspace.type" class="badge badge-secondary">{{ workspace.type }}</span>
                                    <span v-if="workspace.region" class="badge badge-warning">
                                        <i class="fas fa-globe"></i>
                                        {{ workspace.region }}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div v-if="excludeSearchResults.length === 0 && !loadingExcludeSearch" class="search-result-item" style="color: var(--text-secondary); font-style: italic;">
                            No workspaces found
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="excludedWorkspaces.length > 0" class="form-section">
                <label class="form-label">Excluded Workspaces ({{ excludedCount }})</label>
                <div class="excluded-workspaces-list">
                    <div
                        v-for="workspace in excludedWorkspaces"
                        :key="workspace.id"
                        class="excluded-workspace-item"
                    >
                        <div class="workspace-info">
                            <div class="workspace-name">
                                <i class="fas fa-building"></i>
                                {{ workspace.name }}
                            </div>
                            <div class="workspace-meta">
                                <span v-if="workspace.type" class="badge badge-secondary">{{ workspace.type }}</span>
                                <span v-if="workspace.region" class="badge badge-warning">
                                    <i class="fas fa-globe"></i>
                                    {{ workspace.region }}
                                </span>
                            </div>
                        </div>
                        <button
                            type="button"
                            class="btn-remove"
                            @click="removeExcludedWorkspace(workspace.id)"
                            title="Remove from exclusion"
                        >
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </div>
            </div>

            <div class="form-actions">
                <button type="button" class="btn btn-secondary" @click="handleCancel" :disabled="loading">
                    Cancel
                </button>
                <button type="button" class="btn btn-primary" @click="handleSubmit" :disabled="loading">
                    <span v-if="loading">
                        <i class="fas fa-spinner fa-spin"></i>
                        Updating...
                    </span>
                    <span v-else>
                        Update Feature Flag
                    </span>
                </button>
            </div>
        </div>
    `
};
