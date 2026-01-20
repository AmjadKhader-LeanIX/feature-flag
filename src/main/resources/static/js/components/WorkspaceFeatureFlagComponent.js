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
        const enabledAction = ref(true);
        const rolloutPercentage = ref(0);
        const initialRolloutPercentage = ref(0);

        // Pagination state
        const currentPage = ref(1);
        const pageSize = ref(20);

        const filteredWorkspaces = computed(() => {
            if (!searchTerm.value) {
                return workspaces.value;
            }
            return workspaces.value.filter(workspace =>
                workspace.name.toLowerCase().includes(searchTerm.value.toLowerCase()) ||
                workspace.type?.toLowerCase().includes(searchTerm.value.toLowerCase()) ||
                workspace.region?.toLowerCase().includes(searchTerm.value.toLowerCase())
            );
        });

        const totalPages = computed(() => {
            return Math.ceil(filteredWorkspaces.value.length / pageSize.value);
        });

        const paginatedWorkspaces = computed(() => {
            const start = (currentPage.value - 1) * pageSize.value;
            const end = start + pageSize.value;
            return filteredWorkspaces.value.slice(start, end);
        });

        const selectedCount = computed(() => selectedWorkspaceIds.value.length);

        const hasRolloutChanged = computed(() => rolloutPercentage.value !== initialRolloutPercentage.value);

        const hasPreviousPage = computed(() => currentPage.value > 1);
        const hasNextPage = computed(() => currentPage.value < totalPages.value);

        const loadWorkspaces = async () => {
            try {
                loadingWorkspaces.value = true;
                const response = await apiService.getWorkspaces(0, 1000); // Load up to 1000 workspaces
                workspaces.value = response.content || [];
            } catch (error) {
                console.error('Failed to load workspaces:', error);
                workspaces.value = [];
            } finally {
                loadingWorkspaces.value = false;
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
            selectedWorkspaceIds.value = filteredWorkspaces.value.map(w => w.id);
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
                enabled: enabledAction.value,
                rolloutPercentage: rolloutPercentage.value
            });
        };

        const handleCancel = () => {
            emit('cancel');
        };

        const goToPage = (page) => {
            if (page >= 1 && page <= totalPages.value) {
                currentPage.value = page;
            }
        };

        const nextPage = () => {
            if (hasNextPage.value) {
                currentPage.value++;
            }
        };

        const previousPage = () => {
            if (hasPreviousPage.value) {
                currentPage.value--;
            }
        };

        // Reset to page 1 when search term changes
        watch(searchTerm, () => {
            currentPage.value = 1;
        });

        // Load workspaces when component is mounted (lazy loading when modal opens)
        onMounted(() => {
            loadWorkspaces();
            rolloutPercentage.value = props.featureFlag.rolloutPercentage || 0;
            initialRolloutPercentage.value = props.featureFlag.rolloutPercentage || 0;
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
            enabledAction,
            rolloutPercentage,
            filteredWorkspaces,
            paginatedWorkspaces,
            selectedCount,
            hasRolloutChanged,
            currentPage,
            totalPages,
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
                <label class="form-label">Action</label>
                <div class="radio-group">
                    <label class="radio-option">
                        <input type="radio" :value="true" v-model="enabledAction" />
                        <i class="fas fa-check-circle" style="color: var(--success-color);"></i>
                        <span>Enable for selected workspaces</span>
                    </label>
                    <label class="radio-option">
                        <input type="radio" :value="false" v-model="enabledAction" />
                        <i class="fas fa-times-circle" style="color: var(--danger-color);"></i>
                        <span>Disable for selected workspaces</span>
                    </label>
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
                        <button type="button" class="btn btn-sm btn-secondary" @click="selectAll" :disabled="filteredWorkspaces.length === 0">
                            Select All
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
                    No workspaces available
                </div>
                <div v-else>
                    <div v-if="filteredWorkspaces.length === 0" class="loading" style="padding: 20px;">
                        No workspaces match your search
                    </div>
                    <div v-else>
                        <div class="workspace-list">
                            <label
                                v-for="workspace in paginatedWorkspaces"
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
                                Showing {{ ((currentPage - 1) * pageSize) + 1 }} - {{ Math.min(currentPage * pageSize, filteredWorkspaces.length) }} of {{ filteredWorkspaces.length }} workspaces
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
                                    Page {{ currentPage }} of {{ totalPages }}
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
