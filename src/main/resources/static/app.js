const {createApp, ref, computed, reactive, onMounted, nextTick} = Vue;

// API Service
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

    // Feature Flag API
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

    // Audit Log API
    getAuditLogs(params = {}) {
        const queryString = new URLSearchParams(params).toString();
        return this.request(`/audit-logs${queryString ? '?' + queryString : ''}`);
    },

    getAuditLogsByFeatureFlagId(featureFlagId) {
        return this.request(`/audit-logs/feature-flag/${featureFlagId}`);
    },
};

// Toast Component
const ToastComponent = {
    template: `
        <Transition name="slide-fade">
            <div v-if="visible" :class="['toast', type, 'show']">
                <div class="toast-content">
                    <span class="toast-message">{{ message }}</span>
                    <button class="toast-close" @click="close">×</button>
                </div>
            </div>
        </Transition>
    `,
    props: ['visible', 'message', 'type'],
    emits: ['close'],
    methods: {
        close() {
            this.$emit('close');
        }
    }
};

// Modal Component
const ModalComponent = {
    template: `
        <Transition name="fade">
            <div v-if="visible" class="modal-overlay" @click="closeModal">
                <div class="modal" @click.stop>
                    <div class="modal-header">
                        <h2>{{ title }}</h2>
                        <button class="modal-close" @click="closeModal">×</button>
                    </div>
                    <div class="modal-content">
                        <slot></slot>
                    </div>
                </div>
            </div>
        </Transition>
    `,
    props: ['visible', 'title'],
    emits: ['close'],
    methods: {
        closeModal() {
            this.$emit('close');
        }
    }
};

// Feature Flag Form Component
const FeatureFlagFormComponent = {
    template: `
        <form @submit.prevent="submit">
            <div class="form-group">
                <label for="flag-name">Name *</label>
                <input
                    id="flag-name"
                    v-model="form.name"
                    type="text"
                    required
                    placeholder="Enter feature flag name"
                    :disabled="isEdit"
                />
            </div>
            <div class="form-group">
                <label for="flag-description">Description</label>
                <textarea
                    id="flag-description"
                    v-model="form.description"
                    placeholder="Enter description (optional)"
                    :disabled="isEdit"
                ></textarea>
            </div>
            <div class="form-group">
                <label for="flag-team">Team *</label>
                <input
                    id="flag-team"
                    v-model="form.team"
                    type="text"
                    required
                    placeholder="Enter team name"
                    :disabled="isEdit"
                />
            </div>
            <div v-if="!isEdit" class="form-group">
                <label>Regions *</label>
                <div style="max-height: 200px; overflow-y: auto; border: 1px solid #ddd; padding: 10px; border-radius: 4px;">
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="ALL" v-model="form.regions" />
                        ALL (All Regions)
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="WESTEUROPE" v-model="form.regions" :disabled="form.regions.includes('ALL')" />
                        West Europe
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="EASTUS" v-model="form.regions" :disabled="form.regions.includes('ALL')" />
                        East US
                    </label>
                </div>
                <small v-if="form.regions.length === 0" style="color: red;">Please select at least one region</small>
            </div>
            <div v-if="isEdit" class="form-group">
                <label for="flag-rollout">Rollout Percentage</label>
                <div class="range-input">
                    <input
                        id="flag-rollout"
                        v-model.number="form.rolloutPercentage"
                        type="range"
                        min="0"
                        max="100"
                    />
                    <span class="rollout-display">{{ form.rolloutPercentage }}%</span>
                </div>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" @click="cancel">Cancel</button>
                <button type="submit" class="btn btn-primary" :disabled="!isEdit && form.regions.length === 0">
                    {{ isEdit ? 'Update' : 'Create' }}
                </button>
            </div>
        </form>
    `,
    props: ['featureFlag', 'isEdit'],
    emits: ['submit', 'cancel'],
    data() {
        return {
            form: {
                name: '',
                description: '',
                team: '',
                regions: ['ALL'],
                rolloutPercentage: 0
            }
        };
    },
    watch: {
        featureFlag: {
            immediate: true,
            handler(newFlag) {
                if (newFlag) {
                    this.form.name = newFlag.name || '';
                    this.form.description = newFlag.description || '';
                    this.form.team = newFlag.team || '';
                    this.form.regions = newFlag.regions || ['ALL'];
                    this.form.rolloutPercentage = newFlag.rolloutPercentage || 0;
                } else {
                    this.form.name = '';
                    this.form.description = '';
                    this.form.team = '';
                    this.form.regions = ['ALL'];
                    this.form.rolloutPercentage = 0;
                }
            }
        },
        'form.regions': {
            handler(newRegions) {
                if (newRegions.includes('ALL') && newRegions.length > 1) {
                    this.form.regions = ['ALL'];
                }
            }
        }
    },
    methods: {
        submit() {
            if (!this.isEdit && this.form.regions.length === 0) {
                return;
            }
            const data = {
                name: this.form.name,
                description: this.form.description || null,
                team: this.form.team,
                regions: this.form.regions,
                rolloutPercentage: this.isEdit ? this.form.rolloutPercentage : 0
            };
            this.$emit('submit', data);
        },
        cancel() {
            this.$emit('cancel');
        }
    }
};

// Main App
const App = {
    components: {
        ToastComponent,
        ModalComponent,
        FeatureFlagFormComponent
    },

    setup() {
        const currentTab = ref('dashboard');
        const loading = reactive({
            featureFlags: false,
            auditLogs: false,
        });

        const featureFlags = ref([]);
        const auditLogs = ref([]);

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
            auditDetail: false,
        });

        const editingItems = reactive({
            featureFlag: null,
            selectedAuditLog: null,
        });

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
                auditLogs.value = await apiService.getAuditLogs(params);
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.auditLogs = false;
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

        const submitFeatureFlag = async (data) => {
            try {
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
                // CREATE operation - show all new values
                Object.entries(newValues).forEach(([key, value]) => {
                    changes.push({ field: key, old: null, new: value, changed: true });
                });
            } else if (oldValues && !newValues) {
                // DELETE operation - show all old values
                Object.entries(oldValues).forEach(([key, value]) => {
                    changes.push({ field: key, old: value, new: null, changed: true });
                });
            } else if (oldValues && newValues) {
                // UPDATE operation - only show changed fields
                const allKeys = new Set([...Object.keys(oldValues), ...Object.keys(newValues)]);
                allKeys.forEach(key => {
                    const oldVal = oldValues[key];
                    const newVal = newValues[key];
                    // Only include if values are different
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

        const { watch } = Vue;

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

        onMounted(() => {
            loadFeatureFlags();
            loadAuditLogs();
        });

        return {
            currentTab,
            loading,
            featureFlags,
            auditLogs,
            searchTerms,
            filters,
            modals,
            editingItems,
            toast,
            filteredFeatureFlags,
            filteredAuditLogs,
            uniqueTeams,
            uniqueRegions,
            showToast,
            closeToast,
            switchTab,
            createFeatureFlag,
            editFeatureFlag,
            submitFeatureFlag,
            deleteFeatureFlag,
            viewAuditDetails,
            formatDate,
            formatJsonDiff
        };
    },

    template: `
        <div>
            <nav class="navbar">
                <div class="nav-container">
                    <div class="nav-brand">
                        <i class="fas fa-flag"></i>
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
                <Transition name="slide-fade">
                    <div v-if="currentTab === 'dashboard'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Dashboard</h1>
                                <p>Overview of your feature flags</p>
                            </div>
                        </div>

                        <div class="stats-grid">
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
                                <div class="stat-icon" style="background: linear-gradient(135deg, var(--warning-color), #d97706);">
                                    <i class="fas fa-history"></i>
                                </div>
                                <div>
                                    <div class="stat-number">{{ auditLogs.length }}</div>
                                    <div class="stat-label">Audit Logs</div>
                                </div>
                            </div>
                        </div>

                        <div class="dashboard-sections">
                            <div class="recent-activity">
                                <h2><i class="fas fa-clock"></i> Recent Activity</h2>
                                <div v-if="auditLogs.length === 0" class="loading">No recent activity</div>
                                <div v-else class="activity-list">
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

                            <div class="data-grid">
                                <div v-if="loading.featureFlags" class="loading">Loading feature flags...</div>
                                <div v-else-if="filteredFeatureFlags.length === 0" class="loading">No feature flags found</div>
                                <div v-else>
                                    <div v-for="flag in filteredFeatureFlags" :key="flag.id" class="grid-item">
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
                                                <span :class="['badge', flag.rolloutPercentage > 0 ? 'badge-success' : 'badge-secondary']">
                                                    {{ flag.rolloutPercentage > 0 ? 'Active' : 'Inactive' }}
                                                </span>
                                            </div>
                                        </div>
                                        <div class="grid-actions">
                                            <button class="btn btn-sm btn-secondary" @click="editFeatureFlag(flag)">
                                                <i class="fas fa-edit"></i>
                                                Edit
                                            </button>
                                            <button class="btn btn-sm btn-danger" @click="deleteFeatureFlag(flag)">
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

                            <div class="audit-table-container">
                                <div v-if="loading.auditLogs" class="loading">Loading audit logs...</div>
                                <div v-else-if="filteredAuditLogs.length === 0" class="loading">No audit logs found</div>
                                <table v-else class="audit-table">
                                    <thead>
                                        <tr>
                                            <th>Operation</th>
                                            <th>Feature Flag</th>
                                            <th>Team</th>
                                            <th>Timestamp</th>
                                            <th>Changed By</th>
                                            <th>Changes</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="log in filteredAuditLogs" :key="log.id">
                                            <td>
                                                <span :class="['badge', log.operation === 'CREATE' ? 'badge-success' : log.operation === 'UPDATE' ? 'badge-info' : 'badge-danger']">
                                                    {{ log.operation }}
                                                </span>
                                            </td>
                                            <td><strong>{{ log.featureFlagName }}</strong></td>
                                            <td>{{ log.team }}</td>
                                            <td>{{ formatDate(log.timestamp) }}</td>
                                            <td>{{ log.changedBy || '-' }}</td>
                                            <td class="changes-cell">
                                                <div v-if="formatJsonDiff(log.oldValues, log.newValues).length === 0">
                                                    No changes
                                                </div>
                                                <div v-else class="changes-list">
                                                    <div v-for="change in formatJsonDiff(log.oldValues, log.newValues)" :key="change.field" class="change-item">
                                                        <strong>{{ change.field }}:</strong>
                                                        <span class="old-value">{{ change.old !== null ? JSON.stringify(change.old) : '-' }}</span>
                                                        <i class="fas fa-arrow-right"></i>
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
            </main>

            <ModalComponent
                :visible="modals.featureFlag"
                :title="editingItems.featureFlag ? 'Edit Feature Flag' : 'Create Feature Flag'"
                @close="modals.featureFlag = false"
            >
                <FeatureFlagFormComponent
                    :feature-flag="editingItems.featureFlag"
                    :is-edit="!!editingItems.featureFlag"
                    @submit="submitFeatureFlag"
                    @cancel="modals.featureFlag = false"
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
