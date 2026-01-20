const { createApp, ref, computed, reactive, onMounted, watch } = Vue;

const App = {
    components: {
        ToastComponent,
        ModalComponent,
        FeatureFlagFormComponent,
        WorkspaceFeatureFlagComponent,
        SkeletonLoader,
        DataVisualizationCharts
    },

    setup() {
        const currentTab = ref('dashboard');
        const loading = reactive({
            featureFlags: false,
            auditLogs: false,
            enabledWorkspaces: false,
            workspaceFlags: false,
            workspaces: false,
        });

        const featureFlags = ref([]);
        const auditLogs = ref([]);
        const enabledWorkspaces = ref([]);
        const workspaceFlags = ref([]);
        const workspaces = ref([]);

        // Pagination state for enabled workspaces
        const currentEnabledWorkspacesPage = ref(0);
        const enabledWorkspacesPerPage = ref(100);
        const hasMoreEnabledWorkspaces = ref(true);
        const isLoadingMoreEnabledWorkspaces = ref(false);
        const totalEnabledWorkspacesCount = ref(0);
        const workspacesWithFlags = ref([]); // Store workspaces with their enabled flags
        const selectedFeatureFlagForWorkspaces = ref(null);
        const selectedWorkspaceForFlags = ref(null);
        const selectedWorkspaceId = ref('');
        const workspaceSearchTerm = ref('');
        const enabledWorkspacesSearchTerm = ref('');
        const currentWorkspacePage = ref(0); // Changed to 0-based for API
        const workspacesPerPage = ref(100); // Load 100 at a time
        const hasMoreWorkspaces = ref(true);
        const isLoadingMoreWorkspaces = ref(false);
        const totalWorkspacesCount = ref(0);

        const searchTerms = reactive({
            featureFlag: '',
            auditLog: '',
        });

        const filters = reactive({
            team: '',
            region: '',
            auditFlagId: '',
            auditOperation: '',
        });

        const modals = reactive({
            featureFlag: false,
            workspaceFeatureFlag: false,
            auditDetail: false,
        });

        const editingItems = reactive({
            featureFlag: null,
            workspaceFeatureFlag: null,
            selectedAuditLog: null,
        });

        const formLoading = ref(false);

        const toast = reactive({
            visible: false,
            message: '',
            type: 'info'
        });

        const filteredFeatureFlags = computed(() => {
            return featureFlags.value.filter(flag => {
                const matchesSearch = !searchTerms.featureFlag ||
                    flag.name.toLowerCase().includes(searchTerms.featureFlag.toLowerCase()) ||
                    (flag.description && flag.description.toLowerCase().includes(searchTerms.featureFlag.toLowerCase())) ||
                    flag.team.toLowerCase().includes(searchTerms.featureFlag.toLowerCase());

                const matchesTeam = !filters.team ||
                    flag.team.toLowerCase().includes(filters.team.toLowerCase());

                const matchesRegion = !filters.region ||
                    (flag.regions && flag.regions.includes(filters.region));

                return matchesSearch && matchesTeam && matchesRegion;
            });
        });

        const filteredAuditLogs = computed(() => {
            return auditLogs.value.filter(log => {
                const matchesSearch = !searchTerms.auditLog ||
                    log.featureFlagName.toLowerCase().includes(searchTerms.auditLog.toLowerCase()) ||
                    log.team.toLowerCase().includes(searchTerms.auditLog.toLowerCase());

                const matchesFlagId = !filters.auditFlagId ||
                    (log.featureFlagId && log.featureFlagId === filters.auditFlagId);

                const matchesOperation = !filters.auditOperation ||
                    log.operation === filters.auditOperation;

                return matchesSearch && matchesFlagId && matchesOperation;
            });
        });

        const uniqueTeams = computed(() => {
            const teams = [...new Set(featureFlags.value.map(flag => flag.team))];
            return teams.sort();
        });

        const uniqueRegions = computed(() => {
            const regionsSet = new Set();
            featureFlags.value.forEach(flag => {
                if (flag.regions) {
                    flag.regions.forEach(region => regionsSet.add(region));
                }
            });
            return Array.from(regionsSet).sort();
        });

        // No client-side filtering - all workspaces are already filtered by the server
        const filteredWorkspacesWithFlags = computed(() => {
            return workspacesWithFlags.value;
        });

        // No longer need pagination for display - show all loaded workspaces
        const paginatedWorkspacesWithFlags = computed(() => {
            return filteredWorkspacesWithFlags.value;
        });

        const showToast = (message, type = 'info') => {
            toast.message = message;
            toast.type = type;
            toast.visible = true;
            setTimeout(() => {
                toast.visible = false;
            }, 5000);
        };

        const closeToast = () => {
            toast.visible = false;
        };

        const switchTab = (tabId) => {
            currentTab.value = tabId;
            if (tabId === 'feature-flags') {
                loadFeatureFlags();
            } else if (tabId === 'audit-logs') {
                loadAuditLogs();
            } else if (tabId === 'workspaces') {
                loadWorkspaces();
            } else if (tabId === 'dashboard') {
                loadFeatureFlags();
                loadAuditLogs();
            }
        };

        const loadFeatureFlags = async () => {
            try {
                loading.featureFlags = true;
                featureFlags.value = await apiService.getFeatureFlags();
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.featureFlags = false;
            }
        };

        const loadAuditLogs = async () => {
            try {
                loading.auditLogs = true;
                const params = {};
                if (filters.auditFlagId) params.featureFlagId = filters.auditFlagId;
                if (filters.auditOperation) params.operation = filters.auditOperation;
                console.log('Loading audit logs with params:', params);
                auditLogs.value = await apiService.getAuditLogs(params);
                console.log(`Loaded ${auditLogs.value.length} audit logs`);
            } catch (error) {
                console.error('Error loading audit logs:', error);
                showToast(`Failed to load audit logs: ${error.message}`, 'error');
            } finally {
                loading.auditLogs = false;
            }
        };

        const loadWorkspaces = async (reset = true) => {
            if (reset) {
                loading.workspaces = true;
                currentWorkspacePage.value = 0;
                workspacesWithFlags.value = [];
                hasMoreWorkspaces.value = true;
            } else {
                if (!hasMoreWorkspaces.value || isLoadingMoreWorkspaces.value) {
                    return;
                }
                isLoadingMoreWorkspaces.value = true;
            }

            try {
                const response = await apiService.getWorkspaces(
                    currentWorkspacePage.value,
                    workspacesPerPage.value,
                    workspaceSearchTerm.value
                );

                // Response contains: content, pageNumber, pageSize, totalElements, totalPages, isLast, hasNext
                const newWorkspaces = response.content;
                totalWorkspacesCount.value = response.totalElements;
                hasMoreWorkspaces.value = response.hasNext;

                console.log(`Loading ${newWorkspaces.length} workspaces from page ${currentWorkspacePage.value}`);

                // Load enabled feature flags for each workspace in this batch
                const workspacesWithFlagsData = await Promise.all(
                    newWorkspaces.map(async (workspace) => {
                        try {
                            const enabledFlags = await apiService.getEnabledFeatureFlagsForWorkspace(workspace.id);
                            return {
                                workspace,
                                enabledFlags
                            };
                        } catch (error) {
                            console.error(`Failed to load flags for workspace ${workspace.name}:`, error);
                            return {
                                workspace,
                                enabledFlags: []
                            };
                        }
                    })
                );

                console.log(`Loaded flags for ${workspacesWithFlagsData.length} workspaces`);

                // Append to existing workspaces
                workspacesWithFlags.value = [...workspacesWithFlags.value, ...workspacesWithFlagsData];
                workspaces.value = [...workspaces.value, ...newWorkspaces];

                // Increment page for next load
                currentWorkspacePage.value++;
            } catch (error) {
                console.error('Error loading workspaces:', error);
                showToast(`Failed to load workspaces: ${error.message}`, 'error');
            } finally {
                loading.workspaces = false;
                isLoadingMoreWorkspaces.value = false;
            }
        };

        const loadWorkspaceFeatureFlags = async () => {
            if (!selectedWorkspaceId.value) {
                workspaceFlags.value = [];
                return;
            }

            try {
                loading.workspaceFlags = true;
                workspaceFlags.value = await apiService.getEnabledFeatureFlagsForWorkspace(selectedWorkspaceId.value);
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.workspaceFlags = false;
            }
        };

        const viewEnabledWorkspaces = async (flag, reset = true) => {
            if (reset) {
                loading.enabledWorkspaces = true;
                selectedFeatureFlagForWorkspaces.value = flag;
                currentEnabledWorkspacesPage.value = 0;
                enabledWorkspaces.value = [];
                hasMoreEnabledWorkspaces.value = true;
                currentTab.value = 'enabled-workspaces';
            } else {
                if (!hasMoreEnabledWorkspaces.value || isLoadingMoreEnabledWorkspaces.value) {
                    return;
                }
                isLoadingMoreEnabledWorkspaces.value = true;
            }

            try {
                const response = await apiService.getEnabledWorkspacesForFeatureFlag(
                    selectedFeatureFlagForWorkspaces.value.id,
                    currentEnabledWorkspacesPage.value,
                    enabledWorkspacesPerPage.value,
                    enabledWorkspacesSearchTerm.value
                );

                // Response contains paginated data
                const newWorkspaces = response.content;
                totalEnabledWorkspacesCount.value = response.totalElements;
                hasMoreEnabledWorkspaces.value = response.hasNext;

                // Append to existing workspaces
                enabledWorkspaces.value = [...enabledWorkspaces.value, ...newWorkspaces];

                // Increment page for next load
                currentEnabledWorkspacesPage.value++;
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.enabledWorkspaces = false;
                isLoadingMoreEnabledWorkspaces.value = false;
            }
        };

        const viewWorkspaceFlags = async (workspace) => {
            try {
                loading.workspaceFlags = true;
                selectedWorkspaceForFlags.value = workspace;
                workspaceFlags.value = await apiService.getEnabledFeatureFlagsForWorkspace(workspace.id);
                currentTab.value = 'workspace-flags';
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.workspaceFlags = false;
            }
        };

        const createFeatureFlag = () => {
            editingItems.featureFlag = null;
            modals.featureFlag = true;
        };

        const editFeatureFlag = (flag) => {
            editingItems.featureFlag = flag;
            modals.featureFlag = true;
        };

        const editWorkspaces = (flag) => {
            editingItems.workspaceFeatureFlag = flag;
            modals.workspaceFeatureFlag = true;
        };

        const submitWorkspaceFeatureFlag = async (data) => {
            try {
                formLoading.value = true;
                await apiService.updateWorkspaceFeatureFlags(editingItems.workspaceFeatureFlag.id, data);
                showToast(`Feature flag ${data.enabled ? 'enabled' : 'disabled'} for ${data.workspaceIds.length} workspace(s)`, 'success');
                modals.workspaceFeatureFlag = false;
                await loadFeatureFlags();
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                formLoading.value = false;
            }
        };

        const submitFeatureFlag = async (data) => {
            try {
                formLoading.value = true;
                if (editingItems.featureFlag) {
                    await apiService.updateFeatureFlag(editingItems.featureFlag.id, data);
                    showToast('Feature flag updated successfully', 'success');
                } else {
                    await apiService.createFeatureFlag(data);
                    showToast('Feature flag created successfully', 'success');
                }
                modals.featureFlag = false;
                await loadFeatureFlags();
                if (currentTab.value === 'audit-logs') {
                    await loadAuditLogs();
                }
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                formLoading.value = false;
            }
        };

        const deleteFeatureFlag = async (flag) => {
            if (!confirm(`Are you sure you want to delete feature flag "${flag.name}"?`)) {
                return;
            }

            try {
                await apiService.deleteFeatureFlag(flag.id);
                showToast('Feature flag deleted successfully', 'success');
                await loadFeatureFlags();
                if (currentTab.value === 'audit-logs') {
                    await loadAuditLogs();
                }
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        const viewAuditDetails = (log) => {
            editingItems.selectedAuditLog = log;
            modals.auditDetail = true;
        };

        const formatDate = (dateString) => {
            return new Date(dateString).toLocaleString();
        };

        const formatJsonDiff = (oldValues, newValues) => {
            const changes = [];
            if (!oldValues && newValues) {
                Object.entries(newValues).forEach(([key, value]) => {
                    changes.push({ field: key, old: null, new: value, changed: true });
                });
            } else if (oldValues && !newValues) {
                Object.entries(oldValues).forEach(([key, value]) => {
                    changes.push({ field: key, old: value, new: null, changed: true });
                });
            } else if (oldValues && newValues) {
                const allKeys = new Set([...Object.keys(oldValues), ...Object.keys(newValues)]);
                allKeys.forEach(key => {
                    const oldVal = oldValues[key];
                    const newVal = newValues[key];
                    if (JSON.stringify(oldVal) !== JSON.stringify(newVal)) {
                        changes.push({
                            field: key,
                            old: oldVal !== undefined ? oldVal : null,
                            new: newVal !== undefined ? newVal : null,
                            changed: true
                        });
                    }
                });
            }
            return changes;
        };

        // Calculate workspace counts per region for a feature flag
        const getWorkspaceCountsByRegion = (flagId) => {
            const regionCounts = {};

            // Go through all loaded workspaces with flags
            workspacesWithFlags.value.forEach(item => {
                const workspace = item.workspace;
                const enabledFlags = item.enabledFlags || [];

                // Check if this workspace has the flag enabled
                const hasFlagEnabled = enabledFlags.some(flag => flag.id === flagId);

                if (hasFlagEnabled && workspace.region) {
                    regionCounts[workspace.region] = (regionCounts[workspace.region] || 0) + 1;
                }
            });

            return regionCounts;
        };

        watch(() => filters.auditFlagId, () => {
            if (currentTab.value === 'audit-logs') {
                loadAuditLogs();
            }
        });

        watch(() => filters.auditOperation, () => {
            if (currentTab.value === 'audit-logs') {
                loadAuditLogs();
            }
        });

        // Debounced search - reload workspaces when search term changes
        let searchTimeout = null;
        watch(workspaceSearchTerm, () => {
            if (searchTimeout) {
                clearTimeout(searchTimeout);
            }
            searchTimeout = setTimeout(() => {
                loadWorkspaces(true);
            }, 500); // 500ms debounce
        });

        // Debounced search for enabled workspaces
        let enabledWorkspacesSearchTimeout = null;
        watch(enabledWorkspacesSearchTerm, () => {
            if (enabledWorkspacesSearchTimeout) {
                clearTimeout(enabledWorkspacesSearchTimeout);
            }
            enabledWorkspacesSearchTimeout = setTimeout(() => {
                if (selectedFeatureFlagForWorkspaces.value) {
                    viewEnabledWorkspaces(selectedFeatureFlagForWorkspaces.value, true);
                }
            }, 500); // 500ms debounce
        });

        // Infinite scroll handler for workspaces
        const handleWorkspaceScroll = (event) => {
            const container = event.target;
            const scrollPosition = container.scrollTop + container.clientHeight;
            const scrollHeight = container.scrollHeight;

            // Load more when user scrolls to 80% of the page
            if (scrollPosition >= scrollHeight * 0.8 && hasMoreWorkspaces.value && !isLoadingMoreWorkspaces.value) {
                loadWorkspaces(false);
            }
        };

        // Infinite scroll handler for enabled workspaces
        const handleEnabledWorkspacesScroll = (event) => {
            const container = event.target;
            const scrollPosition = container.scrollTop + container.clientHeight;
            const scrollHeight = container.scrollHeight;

            // Load more when user scrolls to 80% of the page
            if (scrollPosition >= scrollHeight * 0.8 && hasMoreEnabledWorkspaces.value && !isLoadingMoreEnabledWorkspaces.value) {
                viewEnabledWorkspaces(selectedFeatureFlagForWorkspaces.value, false);
            }
        };

        onMounted(() => {
            loadFeatureFlags();
            loadAuditLogs();
        });

        return {
            currentTab,
            loading,
            featureFlags,
            auditLogs,
            enabledWorkspaces,
            workspaceFlags,
            workspaces,
            selectedFeatureFlagForWorkspaces,
            selectedWorkspaceForFlags,
            selectedWorkspaceId,
            searchTerms,
            filters,
            modals,
            editingItems,
            toast,
            formLoading,
            filteredFeatureFlags,
            filteredAuditLogs,
            uniqueTeams,
            uniqueRegions,
            showToast,
            closeToast,
            switchTab,
            createFeatureFlag,
            editFeatureFlag,
            editWorkspaces,
            submitFeatureFlag,
            submitWorkspaceFeatureFlag,
            deleteFeatureFlag,
            viewAuditDetails,
            viewEnabledWorkspaces,
            viewWorkspaceFlags,
            loadWorkspaceFeatureFlags,
            formatDate,
            formatJsonDiff,
            getWorkspaceCountsByRegion,
            workspaceSearchTerm,
            filteredWorkspacesWithFlags,
            paginatedWorkspacesWithFlags,
            workspacesWithFlags,
            hasMoreWorkspaces,
            isLoadingMoreWorkspaces,
            totalWorkspacesCount,
            handleWorkspaceScroll,
            hasMoreEnabledWorkspaces,
            isLoadingMoreEnabledWorkspaces,
            totalEnabledWorkspacesCount,
            handleEnabledWorkspacesScroll,
            enabledWorkspacesSearchTerm
        };
    },

    template: `
        <div>
            <nav class="navbar">
                <div class="nav-container">
                    <div class="nav-brand">
                        <img src="https://www.leanix.net/hubfs/2024-Website/branding/logo/favicon/256x256.svg" alt="SAP LeanIX" style="height: 48px; margin-right: 16px; object-fit: contain;" />
                        <span>Feature Flag Manager</span>
                    </div>
                    <div class="nav-menu">
                        <button
                            class="nav-item"
                            :class="{ active: currentTab === 'dashboard' }"
                            @click="switchTab('dashboard')"
                        >
                            <i class="fas fa-home"></i>
                            <span>Dashboard</span>
                        </button>
                        <button
                            class="nav-item"
                            :class="{ active: currentTab === 'feature-flags' }"
                            @click="switchTab('feature-flags')"
                        >
                            <i class="fas fa-flag"></i>
                            <span>Feature Flags</span>
                        </button>
                        <button
                            class="nav-item"
                            :class="{ active: currentTab === 'workspaces' }"
                            @click="switchTab('workspaces')"
                        >
                            <i class="fas fa-building"></i>
                            <span>Workspaces</span>
                        </button>
                        <button
                            class="nav-item"
                            :class="{ active: currentTab === 'audit-logs' }"
                            @click="switchTab('audit-logs')"
                        >
                            <i class="fas fa-history"></i>
                            <span>Audit Logs</span>
                        </button>
                    </div>
                </div>
            </nav>

            <main class="main-content">
                <!-- Debug: Current Tab = {{ currentTab }} -->
                <Transition name="slide-fade">
                    <div v-if="currentTab === 'dashboard'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Dashboard</h1>
                                <p>Overview of your feature flags</p>
                            </div>
                        </div>

                        <!-- Loading Skeleton for Stats -->
                        <SkeletonLoader v-if="loading.featureFlags" type="stat" :count="3" />

                        <!-- Enhanced Stats Cards -->
                        <div v-else class="stats-grid animate-fade-in">
                            <div class="stat-card">
                                <div class="stat-icon">
                                    <i class="fas fa-flag"></i>
                                </div>
                                <div>
                                    <div class="stat-number">{{ featureFlags.length }}</div>
                                    <div class="stat-label">Total Flags</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-icon" style="background: linear-gradient(135deg, var(--success-color), #059669);">
                                    <i class="fas fa-check-circle"></i>
                                </div>
                                <div>
                                    <div class="stat-number">{{ featureFlags.filter(f => f.rolloutPercentage > 0).length }}</div>
                                    <div class="stat-label">Active Flags</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-icon" style="background: linear-gradient(135deg, var(--info-color), #0891b2);">
                                    <i class="fas fa-users"></i>
                                </div>
                                <div>
                                    <div class="stat-number">{{ uniqueTeams.length }}</div>
                                    <div class="stat-label">Teams</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6, #6d28d9);">
                                    <i class="fas fa-globe"></i>
                                </div>
                                <div>
                                    <div class="stat-number">{{ uniqueRegions.length }}</div>
                                    <div class="stat-label">Regions</div>
                                </div>
                            </div>
                        </div>

                        <!-- Data Visualization Charts -->
                        <div v-if="!loading.featureFlags && !loading.auditLogs && featureFlags.length > 0" class="animate-fade-in-scale">
                            <DataVisualizationCharts
                                :feature-flags="featureFlags"
                                :audit-logs="auditLogs"
                                :workspaces="[]"
                            />
                        </div>

                        <!-- Loading Skeleton for Charts -->
                        <div v-else-if="loading.featureFlags || loading.auditLogs">
                            <SkeletonLoader type="chart" :count="2" />
                        </div>

                        <!-- Recent Activity Section -->
                        <div class="dashboard-sections">
                            <div class="recent-activity">
                                <h2><i class="fas fa-clock"></i> Recent Activity</h2>

                                <!-- Loading Skeleton for Activity -->
                                <SkeletonLoader v-if="loading.auditLogs && auditLogs.length === 0" type="list" :count="5" />

                                <!-- Empty State -->
                                <div v-else-if="auditLogs.length === 0" class="empty-state">
                                    <div class="empty-state-icon">
                                        <i class="fas fa-history"></i>
                                    </div>
                                    <div class="empty-state-title">No Recent Activity</div>
                                    <div class="empty-state-description">
                                        There are no recent changes to your feature flags. Start by creating or modifying a flag to see activity here.
                                    </div>
                                </div>

                                <!-- Activity List -->
                                <div v-else class="activity-list stagger-fade-in">
                                    <div v-for="log in auditLogs.slice(0, 10)" :key="log.id" class="activity-item">
                                        <div class="activity-icon" :style="{
                                            background: log.operation === 'CREATE' ? 'var(--success-color)' :
                                                       log.operation === 'UPDATE' ? 'var(--info-color)' :
                                                       'var(--danger-color)'
                                        }">
                                            <i :class="[
                                                'fas',
                                                log.operation === 'CREATE' ? 'fa-plus' :
                                                log.operation === 'UPDATE' ? 'fa-edit' :
                                                'fa-trash'
                                            ]"></i>
                                        </div>
                                        <div style="flex: 1;">
                                            <div class="activity-title">
                                                <span :class="['badge', log.operation === 'CREATE' ? 'badge-success' : log.operation === 'UPDATE' ? 'badge-info' : 'badge-danger']">
                                                    {{ log.operation }}
                                                </span>
                                                {{ log.featureFlagName }}
                                            </div>
                                            <div class="activity-time">
                                                {{ log.team }} · {{ formatDate(log.timestamp) }}
                                                <span v-if="log.changedBy"> · by {{ log.changedBy }}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </Transition>

                <Transition name="slide-fade">
                    <div v-if="currentTab === 'feature-flags'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Feature Flags</h1>
                                <p>Control feature rollouts and experiments</p>
                            </div>
                            <div class="page-actions">
                                <select v-model="filters.team" class="form-select">
                                    <option value="">All Teams</option>
                                    <option v-for="team in uniqueTeams" :key="team" :value="team">
                                        {{ team }}
                                    </option>
                                </select>
                                <select v-model="filters.region" class="form-select">
                                    <option value="">All Regions</option>
                                    <option v-for="region in uniqueRegions" :key="region" :value="region">
                                        {{ region === 'ALL' ? 'ALL (All Regions)' : region }}
                                    </option>
                                </select>
                                <button class="btn btn-primary" @click="createFeatureFlag">
                                    <i class="fas fa-plus"></i>
                                    Create Feature Flag
                                </button>
                            </div>
                        </div>

                        <div class="content-section">
                            <div class="search-bar">
                                <i class="fas fa-search"></i>
                                <input
                                    type="text"
                                    v-model="searchTerms.featureFlag"
                                    placeholder="Search feature flags..."
                                />
                            </div>

                            <!-- Loading Skeleton for Flags -->
                            <SkeletonLoader v-if="loading.featureFlags" type="flag" :count="5" />

                            <!-- Empty State -->
                            <div v-else-if="filteredFeatureFlags.length === 0" class="empty-state">
                                <div class="empty-state-icon">
                                    <i class="fas fa-flag"></i>
                                </div>
                                <div class="empty-state-title">No Feature Flags Found</div>
                                <div class="empty-state-description">
                                    <span v-if="searchTerms.featureFlag || filters.team || filters.region">
                                        Try adjusting your search or filters to find what you're looking for.
                                    </span>
                                    <span v-else>
                                        Get started by creating your first feature flag to control feature rollouts.
                                    </span>
                                </div>
                                <div class="empty-state-actions">
                                    <button class="btn btn-primary" @click="createFeatureFlag">
                                        <i class="fas fa-plus"></i>
                                        Create Your First Flag
                                    </button>
                                </div>
                            </div>

                            <!-- Enhanced Flag Cards with Animation -->
                            <div class="data-grid stagger-fade-in" v-else>
                                <div v-for="(flag, index) in filteredFeatureFlags" :key="flag.id"
                                     class="flag-card-enhanced"
                                     :class="[flag.rolloutPercentage > 0 ? 'active' : 'inactive']"
                                     :style="{ animationDelay: (index * 0.05) + 's' }">

                                    <div class="flag-card-content">
                                        <!-- Header with Status Indicator -->
                                        <div class="flag-card-header">
                                            <div class="flag-card-title-section">
                                                <div class="flag-card-title">
                                                    <i class="fas fa-flag"></i>
                                                    {{ flag.name }}
                                                    <div class="flag-card-status-indicator"
                                                         :class="flag.rolloutPercentage > 0 ? 'active' : 'inactive'">
                                                    </div>
                                                </div>
                                                <div class="flag-card-description">
                                                    {{ flag.description || 'No description provided' }}
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Metadata Badges -->
                                        <div class="flag-card-meta">
                                            <span class="badge badge-info">
                                                <i class="fas fa-users"></i>
                                                {{ flag.team }}
                                            </span>
                                            <span v-for="region in flag.regions" :key="region"
                                                  :class="['badge', region === 'ALL' ? 'badge-primary' : 'badge-warning']">
                                                <i class="fas fa-globe"></i>
                                                {{ region }}
                                            </span>
                                            <span :class="['badge', flag.rolloutPercentage > 0 ? 'badge-success' : 'badge-secondary']">
                                                <i :class="['fas', flag.rolloutPercentage > 0 ? 'fa-check-circle' : 'fa-times-circle']"></i>
                                                {{ flag.rolloutPercentage > 0 ? 'Active' : 'Inactive' }}
                                            </span>
                                        </div>

                                        <!-- Rollout Progress Bar -->
                                        <div class="flag-card-rollout">
                                            <div class="flag-card-rollout-label">
                                                <span>Rollout Progress</span>
                                                <span class="flag-card-rollout-percentage"
                                                      :class="{
                                                          'high': flag.rolloutPercentage > 75,
                                                          'medium': flag.rolloutPercentage >= 25 && flag.rolloutPercentage <= 75,
                                                          'low': flag.rolloutPercentage < 25
                                                      }">
                                                    {{ flag.rolloutPercentage }}%
                                                </span>
                                            </div>
                                            <div class="flag-card-rollout-bar">
                                                <div class="flag-card-rollout-fill"
                                                     :class="{
                                                         'high': flag.rolloutPercentage > 75,
                                                         'medium': flag.rolloutPercentage >= 25 && flag.rolloutPercentage <= 75,
                                                         'low': flag.rolloutPercentage < 25
                                                     }"
                                                     :style="{ width: flag.rolloutPercentage + '%' }">
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Enabled Workspaces by Region -->
                                        <div v-if="Object.keys(getWorkspaceCountsByRegion(flag.id)).length > 0"
                                             style="margin-top: var(--spacing-4); padding: var(--spacing-3); background: var(--gray-50); border-radius: var(--radius-lg); border: 1px solid var(--gray-200);">
                                            <div style="display: flex; align-items: center; gap: var(--spacing-2); margin-bottom: var(--spacing-2);">
                                                <i class="fas fa-building" style="color: var(--primary-600);"></i>
                                                <span style="font-weight: var(--font-semibold); font-size: var(--text-sm); color: var(--text-primary);">
                                                    Enabled Workspaces by Region
                                                </span>
                                            </div>
                                            <div style="display: flex; flex-wrap: wrap; gap: var(--spacing-2);">
                                                <div v-for="(count, region) in getWorkspaceCountsByRegion(flag.id)"
                                                     :key="region"
                                                     style="display: flex; align-items: center; gap: var(--spacing-2); padding: 6px 12px; background: white; border: 1px solid var(--gray-300); border-radius: var(--radius-md); box-shadow: var(--shadow-xs);">
                                                    <span class="badge badge-warning" style="font-size: var(--text-xs);">
                                                        <i class="fas fa-globe"></i>
                                                        {{ region }}
                                                    </span>
                                                    <span style="font-weight: var(--font-bold); color: var(--primary-600); font-size: var(--text-sm);">
                                                        {{ count }}
                                                    </span>
                                                    <span style="color: var(--text-secondary); font-size: var(--text-xs);">
                                                        {{ count === 1 ? 'workspace' : 'workspaces' }}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Action Buttons -->
                                        <div class="grid-actions" style="margin-top: var(--spacing-4);">
                                            <button class="btn btn-sm btn-secondary hover-lift"
                                                    @click="editFeatureFlag(flag)"
                                                    title="Edit rollout percentage, regions, and other settings">
                                                <i class="fas fa-percentage"></i>
                                                Edit Settings
                                            </button>
                                            <button class="btn btn-sm btn-info hover-lift"
                                                    @click="editWorkspaces(flag)"
                                                    title="Enable/disable this flag for specific workspaces">
                                                <i class="fas fa-building"></i>
                                                Manage Workspaces
                                            </button>
                                            <button class="btn btn-sm btn-success hover-lift"
                                                    @click="viewEnabledWorkspaces(flag)"
                                                    title="View all workspaces where this flag is enabled">
                                                <i class="fas fa-list"></i>
                                                View Enabled
                                            </button>
                                            <button class="btn btn-sm btn-danger hover-lift"
                                                    @click="deleteFeatureFlag(flag)">
                                                <i class="fas fa-trash"></i>
                                                Delete
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </Transition>

                <Transition name="slide-fade">
                    <div v-if="currentTab === 'audit-logs'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Audit Logs</h1>
                                <p>View complete history of feature flag changes</p>
                            </div>
                            <div class="page-actions">
                                <select v-model="filters.auditFlagId" class="form-select">
                                    <option value="">All Flags</option>
                                    <option v-for="flag in featureFlags" :key="flag.id" :value="flag.id">
                                        {{ flag.name }}
                                    </option>
                                </select>
                                <select v-model="filters.auditOperation" class="form-select">
                                    <option value="">All Operations</option>
                                    <option value="CREATE">Create</option>
                                    <option value="UPDATE">Update</option>
                                    <option value="DELETE">Delete</option>
                                </select>
                            </div>
                        </div>

                        <div class="content-section">
                            <div class="search-bar">
                                <i class="fas fa-search"></i>
                                <input
                                    type="text"
                                    v-model="searchTerms.auditLog"
                                    placeholder="Search audit logs..."
                                />
                            </div>

                            <!-- Loading Skeleton for Audit Logs -->
                            <SkeletonLoader v-if="loading.auditLogs" type="table" :count="10" />

                            <!-- Empty State -->
                            <div v-else-if="filteredAuditLogs.length === 0" class="empty-state">
                                <div class="empty-state-icon">
                                    <i class="fas fa-history"></i>
                                </div>
                                <div class="empty-state-title">No Audit Logs Found</div>
                                <div class="empty-state-description">
                                    <span v-if="searchTerms.auditLog || filters.auditFlagId || filters.auditOperation">
                                        Try adjusting your search or filters to find audit logs.
                                    </span>
                                    <span v-else>
                                        Audit logs will appear here when you create, update, or delete feature flags.
                                    </span>
                                </div>
                            </div>

                            <div v-else class="audit-table-container animate-fade-in">
                                <table class="audit-table">
                                    <thead>
                                        <tr>
                                            <th><i class="fas fa-tag"></i> Operation</th>
                                            <th><i class="fas fa-flag"></i> Feature Flag</th>
                                            <th><i class="fas fa-users"></i> Team</th>
                                            <th><i class="fas fa-clock"></i> Timestamp</th>
                                            <th><i class="fas fa-user"></i> Changed By</th>
                                            <th><i class="fas fa-exchange-alt"></i> Changes</th>
                                        </tr>
                                    </thead>
                                    <tbody class="stagger-fade-in">
                                        <tr v-for="(log, index) in filteredAuditLogs" :key="log.id"
                                            :style="{ animationDelay: (index * 0.03) + 's' }">
                                            <td>
                                                <span :class="['badge', log.operation === 'CREATE' ? 'badge-success' : log.operation === 'UPDATE' ? 'badge-info' : 'badge-danger']">
                                                    <i :class="['fas', log.operation === 'CREATE' ? 'fa-plus' : log.operation === 'UPDATE' ? 'fa-edit' : 'fa-trash']"></i>
                                                    {{ log.operation }}
                                                </span>
                                            </td>
                                            <td>
                                                <strong style="color: var(--primary-600);">{{ log.featureFlagName }}</strong>
                                            </td>
                                            <td>
                                                <span class="badge badge-secondary">{{ log.team }}</span>
                                            </td>
                                            <td style="font-size: var(--text-sm); color: var(--text-secondary);">
                                                {{ formatDate(log.timestamp) }}
                                            </td>
                                            <td>
                                                <span v-if="log.changedBy" class="badge badge-info">
                                                    <i class="fas fa-user"></i>
                                                    {{ log.changedBy }}
                                                </span>
                                                <span v-else style="color: var(--text-tertiary);">-</span>
                                            </td>
                                            <td class="changes-cell">
                                                <div v-if="formatJsonDiff(log.oldValues, log.newValues).length === 0"
                                                     style="color: var(--text-tertiary); font-style: italic;">
                                                    No changes
                                                </div>
                                                <div v-else class="changes-list">
                                                    <div v-for="change in formatJsonDiff(log.oldValues, log.newValues)"
                                                         :key="change.field"
                                                         class="change-item">
                                                        <strong style="color: var(--primary-600);">{{ change.field }}:</strong>
                                                        <span class="old-value">{{ change.old !== null ? JSON.stringify(change.old) : '-' }}</span>
                                                        <i class="fas fa-arrow-right" style="color: var(--primary-500);"></i>
                                                        <span class="new-value">{{ change.new !== null ? JSON.stringify(change.new) : '-' }}</span>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </Transition>

                <Transition name="slide-fade">
                    <div v-if="currentTab === 'workspaces'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Workspaces</h1>
                                <p>View all workspaces and their enabled feature flags</p>
                            </div>
                        </div>

                        <div class="content-section" @scroll="handleWorkspaceScroll" style="max-height: calc(100vh - 250px); overflow-y: auto;">
                            <div class="search-bar">
                                <i class="fas fa-search"></i>
                                <input
                                    type="text"
                                    v-model="workspaceSearchTerm"
                                    placeholder="Search workspaces by name or region..."
                                />
                            </div>

                            <!-- Loading Skeleton for Workspaces -->
                            <SkeletonLoader v-if="loading.workspaces" type="workspace" :count="6" />

                            <!-- Empty State -->
                            <div v-else-if="!loading.workspaces && filteredWorkspacesWithFlags.length === 0" class="empty-state">
                                <div class="empty-state-icon">
                                    <i class="fas fa-building"></i>
                                </div>
                                <div class="empty-state-title">No Workspaces Found</div>
                                <div class="empty-state-description">
                                    <span v-if="workspaceSearchTerm">
                                        No workspaces match your search. Try a different search term.
                                    </span>
                                    <span v-else>
                                        No workspaces are available at the moment.
                                    </span>
                                </div>
                            </div>

                            <div v-else class="animate-fade-in">
                                <div class="workspace-info-header" style="padding: 16px; background: linear-gradient(135deg, rgba(0, 42, 134, 0.05), rgba(31, 91, 163, 0.05)); border: 1px solid var(--border-color); border-radius: var(--radius-xl); margin-bottom: 24px; box-shadow: var(--shadow-xs);">
                                    <p style="margin: 0; color: var(--text-secondary); display: flex; align-items: center; gap: var(--spacing-2); font-weight: var(--font-medium);">
                                        <i class="fas fa-info-circle" style="color: var(--primary-500);"></i>
                                        Showing <strong style="color: var(--primary-600); margin: 0 4px;">{{ filteredWorkspacesWithFlags.length }}</strong> of <strong style="color: var(--primary-600); margin: 0 4px;">{{ totalWorkspacesCount }}</strong> workspaces
                                        <span v-if="hasMoreWorkspaces" style="margin-left: 8px; color: var(--success-600);">
                                            <i class="fas fa-arrow-down"></i> Scroll to load more
                                        </span>
                                    </p>
                                </div>
                                <div class="workspace-cards-grid stagger-fade-in">
                                    <div v-for="(item, index) in paginatedWorkspacesWithFlags" :key="item.workspace.id"
                                         class="workspace-card hover-lift"
                                         :style="{ animationDelay: (index * 0.05) + 's' }">
                                        <div class="workspace-card-header">
                                            <div class="workspace-card-title">
                                                <i class="fas fa-building"></i>
                                                <h3>{{ item.workspace.name }}</h3>
                                            </div>
                                            <div class="workspace-card-meta">
                                                <span v-if="item.workspace.type" class="badge badge-secondary">
                                                    {{ item.workspace.type }}
                                                </span>
                                                <span v-if="item.workspace.region" class="badge badge-warning">
                                                    <i class="fas fa-globe"></i>
                                                    {{ item.workspace.region }}
                                                </span>
                                            </div>
                                        </div>

                                        <div class="workspace-card-body">
                                            <div class="workspace-flags-section">
                                                <h4>
                                                    <i class="fas fa-flag"></i>
                                                    Enabled Feature Flags ({{ item.enabledFlags.length }})
                                                </h4>
                                                <div v-if="item.enabledFlags.length === 0" class="no-flags-message">
                                                    <i class="fas fa-info-circle"></i>
                                                    No feature flags enabled
                                                </div>
                                                <div v-else class="flags-list">
                                                    <div v-for="flag in item.enabledFlags" :key="flag.id" class="flag-item">
                                                        <div class="flag-item-header">
                                                            <div class="flag-item-name">
                                                                <i class="fas fa-flag"></i>
                                                                {{ flag.name }}
                                                            </div>
                                                            <span class="badge badge-success">
                                                                <i class="fas fa-check"></i>
                                                                Enabled
                                                            </span>
                                                        </div>
                                                        <div v-if="flag.description" class="flag-item-description">
                                                            {{ flag.description }}
                                                        </div>
                                                        <div class="flag-item-meta">
                                                            <span class="badge badge-info">{{ flag.team }}</span>
                                                            <span class="badge badge-secondary">{{ flag.rolloutPercentage }}% Rollout</span>
                                                            <span v-for="region in flag.regions" :key="region" class="badge badge-warning">
                                                                {{ region }}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="workspace-card-footer">
                                            <button
                                                class="btn btn-sm btn-info"
                                                @click="viewWorkspaceFlags(item.workspace)"
                                                title="View detailed information about enabled flags"
                                            >
                                                <i class="fas fa-eye"></i>
                                                View Details
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                <div v-if="isLoadingMoreWorkspaces" class="loading-more" style="text-align: center; padding: 24px;">
                                    <div class="loading-spinner" style="display: inline-block; width: 30px; height: 30px; border: 3px solid var(--gray-200); border-top: 3px solid var(--primary-500); border-radius: 50%; animation: spin 1s linear infinite;"></div>
                                    <p style="margin-top: 12px; color: var(--text-secondary);">Loading more workspaces...</p>
                                </div>

                                <div v-if="!hasMoreWorkspaces && filteredWorkspacesWithFlags.length > 0" style="text-align: center; padding: 24px; color: var(--text-secondary);">
                                    <i class="fas fa-check-circle"></i> All workspaces loaded
                                </div>
                            </div>
                        </div>
                    </div>
                </Transition>

                <Transition name="slide-fade">
                    <div v-if="currentTab === 'enabled-workspaces'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Enabled Workspaces</h1>
                                <p v-if="selectedFeatureFlagForWorkspaces">
                                    Workspaces where "{{ selectedFeatureFlagForWorkspaces.name }}" is enabled
                                </p>
                            </div>
                            <div class="page-actions">
                                <button class="btn btn-secondary" @click="switchTab('feature-flags')">
                                    <i class="fas fa-arrow-left"></i>
                                    Back to Feature Flags
                                </button>
                            </div>
                        </div>

                        <div class="content-section" @scroll="handleEnabledWorkspacesScroll" style="max-height: calc(100vh - 250px); overflow-y: auto;">
                            <div class="search-bar">
                                <i class="fas fa-search"></i>
                                <input
                                    type="text"
                                    v-model="enabledWorkspacesSearchTerm"
                                    placeholder="Search enabled workspaces by name or region..."
                                />
                            </div>

                            <!-- Loading Skeleton -->
                            <SkeletonLoader v-if="loading.enabledWorkspaces" type="workspace" :count="6" />

                            <!-- Empty State -->
                            <div v-else-if="!loading.enabledWorkspaces && enabledWorkspaces.length === 0" class="empty-state">
                                <div class="empty-state-icon">
                                    <i class="fas fa-building"></i>
                                </div>
                                <div class="empty-state-title">No Enabled Workspaces</div>
                                <div class="empty-state-description">
                                    <span v-if="enabledWorkspacesSearchTerm">
                                        No workspaces found matching "{{ enabledWorkspacesSearchTerm }}"
                                    </span>
                                    <span v-else>
                                        No workspaces have this feature flag enabled yet
                                    </span>
                                </div>
                            </div>

                            <div v-else class="animate-fade-in">
                                <div class="workspace-info-header" style="padding: 16px; background: linear-gradient(135deg, rgba(0, 42, 134, 0.05), rgba(31, 91, 163, 0.05)); border: 1px solid var(--border-color); border-radius: var(--radius-xl); margin-bottom: 24px; box-shadow: var(--shadow-xs);">
                                    <p style="margin: 0; color: var(--text-secondary); display: flex; align-items: center; gap: var(--spacing-2); font-weight: var(--font-medium);">
                                        <i class="fas fa-info-circle" style="color: var(--primary-500);"></i>
                                        Showing <strong style="color: var(--primary-600); margin: 0 4px;">{{ enabledWorkspaces.length }}</strong> of <strong style="color: var(--primary-600); margin: 0 4px;">{{ totalEnabledWorkspacesCount }}</strong> enabled workspaces
                                        <span v-if="enabledWorkspacesSearchTerm" style="margin-left: 8px;">
                                            matching "<strong style="color: var(--primary-600);">{{ enabledWorkspacesSearchTerm }}</strong>"
                                        </span>
                                        <span v-if="hasMoreEnabledWorkspaces" style="margin-left: 8px; color: var(--success-600);">
                                            <i class="fas fa-arrow-down"></i> Scroll to load more
                                        </span>
                                    </p>
                                </div>
                                <div class="data-grid stagger-fade-in">
                                    <div v-for="workspace in enabledWorkspaces" :key="workspace.id" class="grid-item">
                                        <div class="grid-content">
                                            <div class="grid-title">
                                                <i class="fas fa-building"></i>
                                                {{ workspace.name }}
                                            </div>
                                            <div class="grid-meta">
                                                <span v-if="workspace.type" class="badge badge-secondary">{{ workspace.type }}</span>
                                                <span v-if="workspace.region" class="badge badge-warning">
                                                    <i class="fas fa-globe"></i>
                                                    {{ workspace.region }}
                                                </span>
                                            </div>
                                            <div class="grid-meta" style="font-size: var(--text-sm); color: var(--text-secondary);">
                                                Created: {{ formatDate(workspace.createdAt) }}
                                            </div>
                                        </div>
                                        <div class="grid-actions">
                                            <button class="btn btn-sm btn-info" @click="viewWorkspaceFlags(workspace)" title="View all enabled flags for this workspace">
                                                <i class="fas fa-flag"></i>
                                                View Enabled Flags
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                <div v-if="isLoadingMoreEnabledWorkspaces" class="loading-more" style="text-align: center; padding: 24px;">
                                    <div class="loading-spinner" style="display: inline-block; width: 30px; height: 30px; border: 3px solid var(--gray-200); border-top: 3px solid var(--primary-500); border-radius: 50%; animation: spin 1s linear infinite;"></div>
                                    <p style="margin-top: 12px; color: var(--text-secondary);">Loading more workspaces...</p>
                                </div>

                                <div v-if="!hasMoreEnabledWorkspaces && enabledWorkspaces.length > 0" style="text-align: center; padding: 24px; color: var(--text-secondary);">
                                    <i class="fas fa-check-circle"></i> All enabled workspaces loaded
                                </div>
                            </div>
                        </div>
                    </div>
                </Transition>

                <Transition name="slide-fade">
                    <div v-if="currentTab === 'workspace-flags'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Enabled Feature Flags</h1>
                                <p v-if="selectedWorkspaceForFlags">
                                    Feature flags enabled for "{{ selectedWorkspaceForFlags.name }}"
                                </p>
                            </div>
                            <div class="page-actions">
                                <button class="btn btn-secondary" @click="switchTab('workspaces')">
                                    <i class="fas fa-arrow-left"></i>
                                    Back to Workspaces
                                </button>
                            </div>
                        </div>

                        <div class="content-section">
                            <!-- Loading Skeleton -->
                            <SkeletonLoader v-if="loading.workspaceFlags" type="flag" :count="5" />

                            <!-- Empty State -->
                            <div v-else-if="workspaceFlags.length === 0" class="empty-state">
                                <div class="empty-state-icon">
                                    <i class="fas fa-flag"></i>
                                </div>
                                <div class="empty-state-title">No Feature Flags Enabled</div>
                                <div class="empty-state-description">
                                    No feature flags are currently enabled for this workspace
                                </div>
                            </div>

                            <div v-else class="data-grid stagger-fade-in animate-fade-in">
                                <div v-for="flag in workspaceFlags" :key="flag.id" class="grid-item">
                                    <div class="grid-content">
                                        <div class="grid-title">{{ flag.name }}</div>
                                        <div class="grid-subtitle">{{ flag.description || 'No description' }}</div>
                                        <div class="grid-meta">
                                            <span>Team: {{ flag.team }}</span>
                                            <span>Rollout: {{ flag.rolloutPercentage }}%</span>
                                        </div>
                                        <div class="grid-meta">
                                            <span v-for="region in flag.regions" :key="region" :class="['badge', region === 'ALL' ? 'badge-info' : 'badge-warning']" style="margin-right: 4px;">
                                                <i class="fas fa-globe"></i>
                                                {{ region }}
                                            </span>
                                        </div>
                                        <div class="progress-bar">
                                            <div class="progress-fill" :style="{ width: flag.rolloutPercentage + '%' }"></div>
                                        </div>
                                        <div class="grid-meta">
                                            <span class="badge badge-success">
                                                <i class="fas fa-check"></i>
                                                Enabled
                                            </span>
                                        </div>
                                    </div>
                                    <div class="grid-actions">
                                        <button class="btn btn-sm btn-success" @click="viewEnabledWorkspaces(flag)" title="View all workspaces where this flag is enabled">
                                            <i class="fas fa-list"></i>
                                            View All Enabled Workspaces
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </Transition>
            </main>

            <ModalComponent
                :visible="modals.featureFlag"
                :title="editingItems.featureFlag ? 'Edit Feature Flag' : 'Create Feature Flag'"
                @close="modals.featureFlag = false"
            >
                <FeatureFlagFormComponent
                    :feature-flag="editingItems.featureFlag"
                    :is-edit="!!editingItems.featureFlag"
                    :loading="formLoading"
                    @submit="submitFeatureFlag"
                    @cancel="modals.featureFlag = false"
                />
            </ModalComponent>

            <ModalComponent
                :visible="modals.workspaceFeatureFlag"
                title="Edit Workspaces"
                @close="modals.workspaceFeatureFlag = false"
            >
                <WorkspaceFeatureFlagComponent
                    v-if="editingItems.workspaceFeatureFlag"
                    :feature-flag="editingItems.workspaceFeatureFlag"
                    :loading="formLoading"
                    @submit="submitWorkspaceFeatureFlag"
                    @cancel="modals.workspaceFeatureFlag = false"
                />
            </ModalComponent>

            <ModalComponent
                :visible="modals.auditDetail"
                title="Audit Log Details"
                @close="modals.auditDetail = false"
            >
                <div v-if="editingItems.selectedAuditLog" class="audit-detail">
                    <div class="detail-row">
                        <strong>Operation:</strong>
                        <span :class="['badge', editingItems.selectedAuditLog.operation === 'CREATE' ? 'badge-success' : editingItems.selectedAuditLog.operation === 'UPDATE' ? 'badge-info' : 'badge-danger']">
                            {{ editingItems.selectedAuditLog.operation }}
                        </span>
                    </div>
                    <div class="detail-row">
                        <strong>Feature Flag:</strong>
                        <span>{{ editingItems.selectedAuditLog.featureFlagName }}</span>
                    </div>
                    <div class="detail-row">
                        <strong>Team:</strong>
                        <span>{{ editingItems.selectedAuditLog.team }}</span>
                    </div>
                    <div class="detail-row">
                        <strong>Timestamp:</strong>
                        <span>{{ formatDate(editingItems.selectedAuditLog.timestamp) }}</span>
                    </div>
                    <div class="detail-row" v-if="editingItems.selectedAuditLog.changedBy">
                        <strong>Changed By:</strong>
                        <span>{{ editingItems.selectedAuditLog.changedBy }}</span>
                    </div>
                    <div class="detail-section">
                        <h3>Changes</h3>
                        <table class="changes-table">
                            <thead>
                                <tr>
                                    <th>Field</th>
                                    <th>Old Value</th>
                                    <th>New Value</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="change in formatJsonDiff(editingItems.selectedAuditLog.oldValues, editingItems.selectedAuditLog.newValues)" :key="change.field" :class="{ changed: change.changed }">
                                    <td><strong>{{ change.field }}</strong></td>
                                    <td><code>{{ change.old !== null ? JSON.stringify(change.old) : '-' }}</code></td>
                                    <td><code>{{ change.new !== null ? JSON.stringify(change.new) : '-' }}</code></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </ModalComponent>

            <ToastComponent
                :visible="toast.visible"
                :message="toast.message"
                :type="toast.type"
                @close="closeToast"
            />
        </div>
    `
};

document.addEventListener('DOMContentLoaded', function() {
    if (typeof Vue !== 'undefined' && typeof App !== 'undefined') {
        createApp(App).mount('#app');
    }
});
