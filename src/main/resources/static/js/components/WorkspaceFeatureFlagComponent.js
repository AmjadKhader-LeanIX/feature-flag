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
        const workspaces = ref([]);
        const loadingWorkspaces = ref(false);
        const selectedWorkspaceIds = ref([]);
        const searchTerm = ref('');
        const rolloutPercentage = ref(0);
        const initialRolloutPercentage = ref(0);

        // Pagination state - using 0-based for API
        const currentPage = ref(0);
        const pageSize = ref(20);
        const totalElements = ref(0);
        const totalPages = ref(0);

        const selectedCount = computed(() => selectedWorkspaceIds.value.length);

        const hasRolloutChanged = computed(() => rolloutPercentage.value !== initialRolloutPercentage.value);

        const hasPreviousPage = computed(() => currentPage.value > 0);
        const hasNextPage = computed(() => currentPage.value < totalPages.value - 1);

        const loadWorkspaces = async (resetPage = false) => {
            try {
                loadingWorkspaces.value = true;

                if (resetPage) {
                    currentPage.value = 0;
                }

                // Use server-side search and pagination
                const response = await apiService.getWorkspaces(
                    currentPage.value,
                    pageSize.value,
                    searchTerm.value
                );

                workspaces.value = response.content || [];
                totalElements.value = response.totalElements || 0;
                totalPages.value = response.totalPages || 0;
            } catch (error) {
                console.error('Failed to load workspaces:', error);
                workspaces.value = [];
                totalElements.value = 0;
                totalPages.value = 0;
            } finally {
                loadingWorkspaces.value = false;
            }
        };

        const loadEnabledWorkspaces = async () => {
            try {
                // Fetch all enabled workspaces for this feature flag
                const response = await apiService.getEnabledWorkspacesForFeatureFlag(props.featureFlag.id, 0, 10000);
                const enabledWorkspaces = response.content || [];

                // Pre-select the workspaces that have this flag enabled
                selectedWorkspaceIds.value = enabledWorkspaces.map(ws => ws.id);
            } catch (error) {
                console.error('Failed to load enabled workspaces:', error);
                selectedWorkspaceIds.value = [];
            }
        };

        const toggleWorkspace = (workspaceId) => {
            const index = selectedWorkspaceIds.value.indexOf(workspaceId);
            if (index > -1) {
                selectedWorkspaceIds.value.splice(index, 1);
            } else {
                selectedWorkspaceIds.value.push(workspaceId);
            }
        };

        const selectAll = () => {
            selectedWorkspaceIds.value = workspaces.value.map(w => w.id);
        };

        const deselectAll = () => {
            selectedWorkspaceIds.value = [];
        };

        const handleSubmit = () => {
            if (selectedWorkspaceIds.value.length === 0 && !hasRolloutChanged.value) {
                alert('Please select at least one workspace or change the rollout percentage');
                return;
            }

            emit('submit', {
                workspaceIds: selectedWorkspaceIds.value,
                enabled: true, // Always enable for selected workspaces
                rolloutPercentage: rolloutPercentage.value
            });
        };

        const handleCancel = () => {
            emit('cancel');
        };

        const goToPage = (page) => {
            if (page >= 0 && page < totalPages.value) {
                currentPage.value = page;
                loadWorkspaces();
            }
        };

        const nextPage = () => {
            if (hasNextPage.value) {
                currentPage.value++;
                loadWorkspaces();
            }
        };

        const previousPage = () => {
            if (hasPreviousPage.value) {
                currentPage.value--;
                loadWorkspaces();
            }
        };

        // Debounced search - reload workspaces when search term changes
        let searchTimeout = null;
        watch(searchTerm, () => {
            if (searchTimeout) {
                clearTimeout(searchTimeout);
            }
            searchTimeout = setTimeout(() => {
                loadWorkspaces(true); // Reset to page 0 when searching
            }, 500); // 500ms debounce
        });

        // Load workspaces when component is mounted (lazy loading when modal opens)
        onMounted(async () => {
            rolloutPercentage.value = props.featureFlag.rolloutPercentage || 0;
            initialRolloutPercentage.value = props.featureFlag.rolloutPercentage || 0;

            // Load enabled workspaces first to pre-select them
            await loadEnabledWorkspaces();

            // Then load the first page of workspaces
            await loadWorkspaces();
        });

        // Watch for feature flag changes
        watch(() => props.featureFlag, (newFlag) => {
            if (newFlag) {
                rolloutPercentage.value = newFlag.rolloutPercentage || 0;
                initialRolloutPercentage.value = newFlag.rolloutPercentage || 0;
            }
        });

        return {
            workspaces,
            loadingWorkspaces,
            selectedWorkspaceIds,
            searchTerm,
            rolloutPercentage,
            selectedCount,
            hasRolloutChanged,
            currentPage,
            totalPages,
            totalElements,
            pageSize,
            hasPreviousPage,
            hasNextPage,
            toggleWorkspace,
            selectAll,
            deselectAll,
            handleSubmit,
            handleCancel,
            goToPage,
            nextPage,
            previousPage
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
                <small style="color: var(--text-secondary); font-size: var(--text-xs); margin-top: var(--spacing-2); display: block;">
                    Adjust the rollout percentage to control how many workspaces will have this flag enabled
                </small>
            </div>

            <div class="form-section">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                    <label class="form-label" style="margin-bottom: 0;">Select Workspaces ({{ selectedCount }} selected)</label>
                    <div style="display: flex; gap: 8px;">
                        <button type="button" class="btn btn-sm btn-secondary" @click="selectAll" :disabled="workspaces.length === 0">
                            Select All on Page
                        </button>
                        <button type="button" class="btn btn-sm btn-secondary" @click="deselectAll" :disabled="selectedCount === 0">
                            Deselect All
                        </button>
                    </div>
                </div>

                <div class="search-bar" style="margin-bottom: 12px;">
                    <i class="fas fa-search"></i>
                    <input
                        type="text"
                        v-model="searchTerm"
                        placeholder="Search workspaces..."
                    />
                </div>

                <div v-if="loadingWorkspaces" class="loading" style="padding: 20px;">
                    Loading workspaces...
                </div>
                <div v-else-if="workspaces.length === 0" class="loading" style="padding: 20px;">
                    No workspaces match your search
                </div>
                <div v-else>
                    <div class="workspace-list">
                        <label
                            v-for="workspace in workspaces"
                            :key="workspace.id"
                            class="workspace-item"
                            :class="{ selected: selectedWorkspaceIds.includes(workspace.id) }"
                        >
                            <input
                                type="checkbox"
                                :value="workspace.id"
                                :checked="selectedWorkspaceIds.includes(workspace.id)"
                                @change="toggleWorkspace(workspace.id)"
                            />
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
                        </label>
                    </div>

                    <div v-if="totalPages > 1" class="pagination-controls" style="display: flex; justify-content: space-between; align-items: center; padding: 12px; border-top: 1px solid var(--border-color); margin-top: 12px;">
                        <div style="color: var(--text-secondary); font-size: var(--text-sm);">
                            Showing {{ (currentPage * pageSize) + 1 }} - {{ Math.min((currentPage + 1) * pageSize, totalElements) }} of {{ totalElements }} workspaces
                        </div>
                        <div style="display: flex; gap: 8px; align-items: center;">
                            <button
                                type="button"
                                class="btn btn-sm btn-secondary"
                                @click="previousPage"
                                :disabled="!hasPreviousPage"
                            >
                                <i class="fas fa-chevron-left"></i>
                                Previous
                            </button>
                            <span style="color: var(--text-secondary); font-size: var(--text-sm); padding: 0 12px;">
                                Page {{ currentPage + 1 }} of {{ totalPages }}
                            </span>
                            <button
                                type="button"
                                class="btn btn-sm btn-secondary"
                                @click="nextPage"
                                :disabled="!hasNextPage"
                            >
                                Next
                                <i class="fas fa-chevron-right"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="form-actions">
                <button type="button" class="btn btn-secondary" @click="handleCancel" :disabled="loading">
                    Cancel
                </button>
                <button type="button" class="btn btn-primary" @click="handleSubmit" :disabled="loading || (selectedCount === 0 && !hasRolloutChanged)">
                    <i class="fas fa-save"></i>
                    {{ loading ? 'Saving...' : 'Save' }}
                </button>
            </div>
        </div>
    `
};
