const { createApp, ref, computed, reactive, onMounted, nextTick } = Vue;

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

    // Workspace API
    getWorkspaces() {
        return this.request('/workspaces');
    },

    createWorkspace(data) {
        return this.request('/workspaces', {
            method: 'POST',
            data
        });
    },

    updateWorkspace(id, data) {
        return this.request(`/workspaces/${id}`, {
            method: 'PUT',
            data
        });
    },

    deleteWorkspace(id) {
        return this.request(`/workspaces/${id}`, {
            method: 'DELETE'
        });
    },

    // Feature Flag API
    getFeatureFlags() {
        return this.request('/feature-flags');
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

    evaluateFeatureFlag(id, customerId) {
        return this.request(`/feature-flags/${id}/check?customerId=${customerId}`);
    },

    // User API
    getUsers() {
        return this.request('/users');
    },

    createUser(data) {
        return this.request('/users', {
            method: 'POST',
            data
        });
    },

    updateUser(id, data) {
        return this.request(`/users/${id}`, {
            method: 'PUT',
            data
        });
    },

    deleteUser(id) {
        return this.request(`/users/${id}`, {
            method: 'DELETE'
        });
    },

    // Customer Feature Flag API
    getCustomerFeatureFlags() {
        return this.request('/customer-feature-flags');
    },

    createCustomerFeatureFlag(data) {
        return this.request('/customer-feature-flags', {
            method: 'POST',
            data
        });
    },

    updateCustomerFeatureFlag(id, data) {
        return this.request(`/customer-feature-flags/${id}`, {
            method: 'PUT',
            data
        });
    },

    deleteCustomerFeatureFlag(id) {
        return this.request(`/customer-feature-flags/${id}`, {
            method: 'DELETE'
        });
    },

    getWorkspaceCustomers() {
        return this.request('/customer-feature-flags/workspace-customers');
    }
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
                    <div v-if="$slots.actions" class="modal-actions">
                        <slot name="actions"></slot>
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

// Workspace Form Component
const WorkspaceFormComponent = {
    template: `
        <form @submit.prevent="submit">
            <div class="form-group">
                <label for="workspace-name">Name *</label>
                <input
                    id="workspace-name"
                    v-model="form.name"
                    type="text"
                    required
                    placeholder="Enter workspace name"
                />
            </div>
            <div class="form-group">
                <label for="workspace-type">Type</label>
                <input
                    id="workspace-type"
                    v-model="form.type"
                    type="text"
                    placeholder="Enter workspace type (optional)"
                />
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" @click="cancel">Cancel</button>
                <button type="submit" class="btn btn-primary">
                    {{ isEdit ? 'Update' : 'Create' }}
                </button>
            </div>
        </form>
    `,
    props: ['workspace', 'isEdit'],
    emits: ['submit', 'cancel'],
    data() {
        return {
            form: {
                name: '',
                type: ''
            }
        };
    },
    watch: {
        workspace: {
            immediate: true,
            handler(newWorkspace) {
                if (newWorkspace) {
                    this.form.name = newWorkspace.name || '';
                    this.form.type = newWorkspace.type || '';
                } else {
                    this.form.name = '';
                    this.form.type = '';
                }
            }
        }
    },
    methods: {
        submit() {
            const data = {
                name: this.form.name,
                type: this.form.type || null
            };
            this.$emit('submit', data);
        },
        cancel() {
            this.$emit('cancel');
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
                />
            </div>
            <div class="form-group">
                <label for="flag-description">Description</label>
                <textarea
                    id="flag-description"
                    v-model="form.description"
                    placeholder="Enter description (optional)"
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
                />
            </div>
            <div class="form-group">
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
                <button type="submit" class="btn btn-primary">
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
                    this.form.rolloutPercentage = newFlag.rolloutPercentage || 0;
                } else {
                    this.form.name = '';
                    this.form.description = '';
                    this.form.team = '';
                    this.form.rolloutPercentage = 0;
                }
            }
        }
    },
    methods: {
        submit() {
            const data = {
                name: this.form.name,
                description: this.form.description || null,
                team: this.form.team,
                rolloutPercentage: this.form.rolloutPercentage
            };
            this.$emit('submit', data);
        },
        cancel() {
            this.$emit('cancel');
        }
    }
};

// User Form Component
const UserFormComponent = {
    template: `
        <form @submit.prevent="submit">
            <div class="form-group">
                <label for="user-workspace">Workspace *</label>
                <select id="user-workspace" v-model="form.workspaceId" required>
                    <option value="">Select workspace</option>
                    <option v-for="workspace in workspaces" :key="workspace.id" :value="workspace.id">
                        {{ workspace.name }}
                    </option>
                </select>
            </div>
            <div class="form-group">
                <label for="user-email">Email</label>
                <input
                    id="user-email"
                    v-model="form.email"
                    type="email"
                    placeholder="Enter email address (optional)"
                />
            </div>
            <div class="form-group">
                <label class="checkbox-label">
                    <input
                        v-model="form.isActive"
                        type="checkbox"
                    />
                    Active User
                </label>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" @click="cancel">Cancel</button>
                <button type="submit" class="btn btn-primary">
                    {{ isEdit ? 'Update' : 'Create' }}
                </button>
            </div>
        </form>
    `,
    props: ['user', 'isEdit', 'workspaces'],
    emits: ['submit', 'cancel'],
    data() {
        return {
            form: {
                workspaceId: '',
                email: '',
                isActive: true
            }
        };
    },
    watch: {
        user: {
            immediate: true,
            handler(newUser) {
                if (newUser) {
                    this.form.workspaceId = newUser.workspaceId || '';
                    this.form.email = newUser.email || '';
                    this.form.isActive = newUser.isActive !== false;
                } else {
                    this.form.workspaceId = '';
                    this.form.email = '';
                    this.form.isActive = true;
                }
            }
        }
    },
    methods: {
        submit() {
            const data = {
                workspaceId: this.form.workspaceId,
                email: this.form.email || null,
                isActive: this.form.isActive
            };
            this.$emit('submit', data);
        },
        cancel() {
            this.$emit('cancel');
        }
    }
};

// Customer Feature Flag Form Component
const CustomerFeatureFlagFormComponent = {
    template: `
        <form @submit.prevent="submit">
            <div class="form-group">
                <label for="customer-flag-workspace">Workspace</label>
                <select id="customer-flag-workspace" v-model="form.workspaceId">
                    <option value="">Select workspace</option>
                    <option v-for="workspace in workspaces" :key="workspace.id" :value="workspace.id">
                        {{ workspace.name }}
                    </option>
                </select>
            </div>
            <div class="form-group">
                <label for="customer-flag-customer">Customer ID *</label>
                <input
                    id="customer-flag-customer"
                    v-model="form.customerId"
                    type="text"
                    required
                    placeholder="Enter customer UUID"
                    :disabled="isEdit"
                />
            </div>
            <div class="form-group">
                <label for="customer-flag-feature">Feature Flag *</label>
                <select id="customer-flag-feature" v-model="form.featureFlagId" :disabled="isEdit" required>
                    <option value="">Select feature flag</option>
                    <option v-for="flag in featureFlags" :key="flag.id" :value="flag.id">
                        {{ flag.name }}
                    </option>
                </select>
            </div>
            <div class="form-group">
                <label class="checkbox-label">
                    <input
                        v-model="form.isEnabled"
                        type="checkbox"
                    />
                    Enabled for Customer
                </label>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" @click="cancel">Cancel</button>
                <button type="submit" class="btn btn-primary">
                    {{ isEdit ? 'Update' : 'Create' }}
                </button>
            </div>
        </form>
    `,
    props: ['customerFlag', 'isEdit', 'workspaces', 'featureFlags'],
    emits: ['submit', 'cancel'],
    data() {
        return {
            form: {
                workspaceId: '',
                customerId: '',
                featureFlagId: '',
                isEnabled: false
            }
        };
    },
    watch: {
        customerFlag: {
            immediate: true,
            handler(newFlag) {
                if (newFlag) {
                    this.form.workspaceId = newFlag.workspaceId || '';
                    this.form.customerId = newFlag.customerId || '';
                    this.form.featureFlagId = newFlag.featureFlagId || '';
                    this.form.isEnabled = newFlag.isEnabled || false;
                } else {
                    this.form.workspaceId = '';
                    this.form.customerId = '';
                    this.form.featureFlagId = '';
                    this.form.isEnabled = false;
                }
            }
        }
    },
    methods: {
        submit() {
            const data = {
                workspaceId: this.form.workspaceId || null,
                customerId: this.form.customerId,
                featureFlagId: this.form.featureFlagId,
                isEnabled: this.form.isEnabled
            };
            this.$emit('submit', data);
        },
        cancel() {
            this.$emit('cancel');
        }
    }
};

// Evaluation Modal Component
const EvaluationModalComponent = {
    template: `
        <div class="evaluation-content">
            <div class="form-group">
                <label for="evaluation-customer">Customer ID *</label>
                <input
                    id="evaluation-customer"
                    v-model="customerId"
                    type="text"
                    placeholder="Enter customer UUID"
                    required
                />
            </div>
            <button class="btn btn-primary" @click="evaluate" :disabled="!customerId.trim()">
                Evaluate Flag
            </button>
            <div v-if="result" class="evaluation-result">
                <div :class="['result-status', result.enabled ? 'enabled' : 'disabled']">
                    {{ result.enabled ? 'ENABLED' : 'DISABLED' }}
                </div>
                <div class="result-reason">{{ result.reason }}</div>
            </div>
        </div>
    `,
    props: ['flagId'],
    emits: ['close'],
    data() {
        return {
            customerId: '',
            result: null
        };
    },
    methods: {
        async evaluate() {
            if (!this.customerId.trim()) {
                return;
            }

            try {
                this.result = await apiService.evaluateFeatureFlag(this.flagId, this.customerId);
            } catch (error) {
                this.$emit('error', error.message);
            }
        }
    }
};

// Main App
const App = {
    components: {
        ToastComponent,
        ModalComponent,
        WorkspaceFormComponent,
        FeatureFlagFormComponent,
        UserFormComponent,
        CustomerFeatureFlagFormComponent,
        EvaluationModalComponent
    },

    setup() {
        // Reactive data
        const currentTab = ref('dashboard');
        const loading = reactive({
            workspaces: false,
            featureFlags: false,
            users: false,
            customerFeatureFlags: false
        });

        // Data stores
        const workspaces = ref([]);
        const featureFlags = ref([]);
        const users = ref([]);
        const customerFeatureFlags = ref([]);
        const workspaceCustomers = ref([]);

        // Search/filter states
        const searchTerms = reactive({
            workspace: '',
            featureFlag: '',
            customer: ''
        });

        const filters = reactive({
            workspaceId: '',
            userWorkspaceId: '',
            userIsActive: '',
            customerWorkspaceId: ''
        });

        // Modal states
        const modals = reactive({
            workspace: false,
            featureFlag: false,
            user: false,
            customerFeatureFlag: false,
            evaluation: false
        });

        // Form states
        const editingItems = reactive({
            workspace: null,
            featureFlag: null,
            user: null,
            customerFeatureFlag: null,
            evaluationFlagId: null
        });

        // Toast state
        const toast = reactive({
            visible: false,
            message: '',
            type: 'info'
        });

        // Computed properties
        const dashboardStats = computed(() => ({
            workspaceCount: workspaces.value.length,
            featureFlagCount: featureFlags.value.length,
            userCount: users.value.length,
            activeFlagsCount: featureFlags.value.filter(flag => flag.rolloutPercentage > 0).length
        }));

        const recentActivity = computed(() => [
            {
                icon: 'fas fa-building',
                title: `${dashboardStats.value.workspaceCount} workspaces configured`,
                time: 'Current status'
            },
            {
                icon: 'fas fa-flag',
                title: `${dashboardStats.value.featureFlagCount} feature flags created`,
                time: 'Current status'
            },
            {
                icon: 'fas fa-users',
                title: `${dashboardStats.value.userCount} users registered`,
                time: 'Current status'
            }
        ]);

        const filteredWorkspaces = computed(() => {
            return workspaces.value.filter(workspace => {
                const matchesSearch = !searchTerms.workspace ||
                    workspace.name.toLowerCase().includes(searchTerms.workspace.toLowerCase()) ||
                    (workspace.type && workspace.type.toLowerCase().includes(searchTerms.workspace.toLowerCase()));
                return matchesSearch;
            });
        });

        const filteredFeatureFlags = computed(() => {
            return featureFlags.value.filter(flag => {
                const matchesSearch = !searchTerms.featureFlag ||
                    flag.name.toLowerCase().includes(searchTerms.featureFlag.toLowerCase()) ||
                    (flag.description && flag.description.toLowerCase().includes(searchTerms.featureFlag.toLowerCase())) ||
                    flag.team.toLowerCase().includes(searchTerms.featureFlag.toLowerCase());

                const matchesWorkspace = !filters.workspaceId; // For now, show all as workspace association isn't in DTO

                return matchesSearch && matchesWorkspace;
            });
        });

        const filteredUsers = computed(() => {
            return users.value.filter(user => {
                const matchesWorkspace = !filters.userWorkspaceId || user.workspaceId === filters.userWorkspaceId;
                const matchesActive = filters.userIsActive === '' || user.isActive === (filters.userIsActive === 'true');
                return matchesWorkspace && matchesActive;
            });
        });

        const filteredCustomerFeatureFlags = computed(() => {
            return customerFeatureFlags.value.filter(flag => {
                const matchesWorkspace = !filters.customerWorkspaceId || flag.workspaceId === filters.customerWorkspaceId;
                const matchesSearch = !searchTerms.customer ||
                    flag.customerId.toLowerCase().includes(searchTerms.customer.toLowerCase());
                return matchesWorkspace && matchesSearch;
            });
        });

        // Methods
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

            // Load data for the active tab
            switch(tabId) {
                case 'dashboard':
                    loadDashboardData();
                    break;
                case 'workspaces':
                    loadWorkspaces();
                    break;
                case 'feature-flags':
                    loadFeatureFlags();
                    break;
                case 'users':
                    loadUsers();
                    break;
                case 'customers':
                    loadCustomerFeatureFlags();
                    break;
            }
        };

        // Data loading methods
        const loadWorkspaces = async () => {
            try {
                loading.workspaces = true;
                workspaces.value = await apiService.getWorkspaces();
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.workspaces = false;
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

        const loadUsers = async () => {
            try {
                loading.users = true;
                users.value = await apiService.getUsers();
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.users = false;
            }
        };

        const loadCustomerFeatureFlags = async () => {
            try {
                loading.customerFeatureFlags = true;
                customerFeatureFlags.value = await apiService.getCustomerFeatureFlags();
            } catch (error) {
                showToast(error.message, 'error');
            } finally {
                loading.customerFeatureFlags = false;
            }
        };

        const loadWorkspaceCustomers = async () => {
            try {
                workspaceCustomers.value = await apiService.getWorkspaceCustomers();
            } catch (error) {
                console.error('Failed to load workspace customers:', error);
            }
        };

        const loadDashboardData = async () => {
            await Promise.all([
                loadWorkspaces(),
                loadFeatureFlags(),
                loadUsers(),
                loadCustomerFeatureFlags()
            ]);
        };

        // CRUD methods for workspaces
        const createWorkspace = () => {
            editingItems.workspace = null;
            modals.workspace = true;
        };

        const editWorkspace = (workspace) => {
            editingItems.workspace = workspace;
            modals.workspace = true;
        };

        const submitWorkspace = async (data) => {
            try {
                if (editingItems.workspace) {
                    await apiService.updateWorkspace(editingItems.workspace.id, data);
                    showToast('Workspace updated successfully', 'success');
                } else {
                    await apiService.createWorkspace(data);
                    showToast('Workspace created successfully', 'success');
                }
                modals.workspace = false;
                await loadWorkspaces();
                if (currentTab.value === 'dashboard') await loadDashboardData();
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        const deleteWorkspace = async (workspace) => {
            if (!confirm(`Are you sure you want to delete workspace "${workspace.name}"?`)) {
                return;
            }

            try {
                await apiService.deleteWorkspace(workspace.id);
                showToast('Workspace deleted successfully', 'success');
                await loadWorkspaces();
                if (currentTab.value === 'dashboard') await loadDashboardData();
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        // CRUD methods for feature flags
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
                if (currentTab.value === 'dashboard') await loadDashboardData();
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
                if (currentTab.value === 'dashboard') await loadDashboardData();
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        const showEvaluationModal = (flagId) => {
            editingItems.evaluationFlagId = flagId;
            modals.evaluation = true;
        };

        // CRUD methods for users
        const createUser = () => {
            editingItems.user = null;
            modals.user = true;
        };

        const editUser = (user) => {
            editingItems.user = user;
            modals.user = true;
        };

        const submitUser = async (data) => {
            try {
                if (editingItems.user) {
                    await apiService.updateUser(editingItems.user.id, data);
                    showToast('User updated successfully', 'success');
                } else {
                    await apiService.createUser(data);
                    showToast('User created successfully', 'success');
                }
                modals.user = false;
                await loadUsers();
                if (currentTab.value === 'dashboard') await loadDashboardData();
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        const deleteUser = async (user) => {
            const identifier = user.email || 'User';
            if (!confirm(`Are you sure you want to delete user "${identifier}"?`)) {
                return;
            }

            try {
                await apiService.deleteUser(user.id);
                showToast('User deleted successfully', 'success');
                await loadUsers();
                if (currentTab.value === 'dashboard') await loadDashboardData();
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        // CRUD methods for customer feature flags
        const createCustomerFeatureFlag = () => {
            editingItems.customerFeatureFlag = null;
            modals.customerFeatureFlag = true;
        };

        const editCustomerFeatureFlag = (flag) => {
            editingItems.customerFeatureFlag = flag;
            modals.customerFeatureFlag = true;
        };

        const submitCustomerFeatureFlag = async (data) => {
            try {
                if (editingItems.customerFeatureFlag) {
                    await apiService.updateCustomerFeatureFlag(editingItems.customerFeatureFlag.id, data);
                    showToast('Customer feature flag updated successfully', 'success');
                } else {
                    await apiService.createCustomerFeatureFlag(data);
                    showToast('Customer feature flag created successfully', 'success');
                }
                modals.customerFeatureFlag = false;
                await loadCustomerFeatureFlags();
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        const deleteCustomerFeatureFlag = async (flag) => {
            if (!confirm(`Are you sure you want to delete the feature flag for customer "${flag.customerId}"?`)) {
                return;
            }

            try {
                await apiService.deleteCustomerFeatureFlag(flag.id);
                showToast('Customer feature flag deleted successfully', 'success');
                await loadCustomerFeatureFlags();
            } catch (error) {
                showToast(error.message, 'error');
            }
        };

        // Utility methods
        const formatDate = (dateString) => {
            return new Date(dateString).toLocaleDateString();
        };

        const getWorkspaceName = (workspaceId) => {
            const workspace = workspaces.value.find(w => w.id === workspaceId);
            return workspace ? workspace.name : 'Unknown';
        };

        const getFeatureFlagName = (flagId) => {
            const flag = featureFlags.value.find(f => f.id === flagId);
            return flag ? flag.name : 'Unknown Feature';
        };

        // Lifecycle
        onMounted(() => {
            loadDashboardData();
            loadWorkspaceCustomers();
        });

        return {
            // State
            currentTab,
            loading,
            workspaces,
            featureFlags,
            users,
            customerFeatureFlags,
            workspaceCustomers,
            searchTerms,
            filters,
            modals,
            editingItems,
            toast,

            // Computed
            dashboardStats,
            recentActivity,
            filteredWorkspaces,
            filteredFeatureFlags,
            filteredUsers,
            filteredCustomerFeatureFlags,

            // Methods
            showToast,
            closeToast,
            switchTab,

            // Workspace methods
            createWorkspace,
            editWorkspace,
            submitWorkspace,
            deleteWorkspace,

            // Feature flag methods
            createFeatureFlag,
            editFeatureFlag,
            submitFeatureFlag,
            deleteFeatureFlag,
            showEvaluationModal,

            // User methods
            createUser,
            editUser,
            submitUser,
            deleteUser,

            // Customer feature flag methods
            createCustomerFeatureFlag,
            editCustomerFeatureFlag,
            submitCustomerFeatureFlag,
            deleteCustomerFeatureFlag,

            // Utility methods
            formatDate,
            getWorkspaceName,
            getFeatureFlagName
        };
    },

    template: `
        <div>
            <!-- Navigation -->
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
                            <i class="fas fa-chart-dashboard"></i>
                            <span>Dashboard</span>
                        </button>
                        <button
                            class="nav-item"
                            :class="{ active: currentTab === 'feature-flags' }"
                            @click="switchTab('feature-flags')"
                        >
                            <i class="fas fa-flag"></i>
                            <span>Feature Flags</span>

                    </div>
                </div>
            </nav>

            <!-- Main Content -->
            <main class="main-content">
                <!-- Dashboard Tab -->
                <Transition name="slide-fade">
                    <div v-if="currentTab === 'dashboard'">
                        <div class="page-header">
                            <div class="page-title">
                                <h1>Dashboard</h1>
                                <p>Overview of your feature flag management system</p>
                            </div>
                        </div>

                        <div class="stats-grid">
                            <div class="stat-card">
                                <div class="stat-icon">
                                    <i class="fas fa-building"></i>
                                </div>
                                <div class="stat-content">
                                    <div class="stat-number">{{ dashboardStats.workspaceCount }}</div>
                                    <div class="stat-label">Workspaces</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-icon">
                                    <i class="fas fa-flag"></i>
                                </div>
                                <div class="stat-content">
                                    <div class="stat-number">{{ dashboardStats.featureFlagCount }}</div>
                                    <div class="stat-label">Feature Flags</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-icon">
                                    <i class="fas fa-users"></i>
                                </div>
                                <div class="stat-content">
                                    <div class="stat-number">{{ dashboardStats.userCount }}</div>
                                    <div class="stat-label">Users</div>
                                </div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-icon">
                                    <i class="fas fa-toggle-on"></i>
                                </div>
                                <div class="stat-content">
                                    <div class="stat-number">{{ dashboardStats.activeFlagsCount }}</div>
                                    <div class="stat-label">Active Flags</div>
                                </div>
                            </div>
                        </div>

                        <div class="dashboard-sections">
                            <div class="recent-activity">
                                <h2>Recent Activity</h2>
                                <div class="activity-list">
                                    <div v-for="activity in recentActivity" :key="activity.title" class="activity-item">
                                        <div class="activity-icon">
                                            <i :class="activity.icon"></i>
                                        </div>
                                        <div class="activity-content">
                                            <div class="activity-title">{{ activity.title }}</div>
                                            <div class="activity-time">{{ activity.time }}</div>
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
                                <select v-model="filters.workspaceId" class="form-select">
                                    <option value="">All Workspaces</option>
                                    <option v-for="workspace in workspaces" :key="workspace.id" :value="workspace.id">
                                        {{ workspace.name }}
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
                                            <button class="btn btn-sm btn-info" @click="showEvaluationModal(flag.id)">
                                                <i class="fas fa-check-circle"></i>
                                                Evaluate
                                            </button>
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



            <!-- Modals -->
            <ModalComponent
                :visible="modals.workspace"
                :title="editingItems.workspace ? 'Edit Workspace' : 'Create Workspace'"
                @close="modals.workspace = false"
            >
                <WorkspaceFormComponent
                    :workspace="editingItems.workspace"
                    :is-edit="!!editingItems.workspace"
                    @submit="submitWorkspace"
                    @cancel="modals.workspace = false"
                />
            </ModalComponent>

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
                :visible="modals.user"
                :title="editingItems.user ? 'Edit User' : 'Create User'"
                @close="modals.user = false"
            >
                <UserFormComponent
                    :user="editingItems.user"
                    :is-edit="!!editingItems.user"
                    :workspaces="workspaces"
                    @submit="submitUser"
                    @cancel="modals.user = false"
                />
            </ModalComponent>

            <ModalComponent
                :visible="modals.customerFeatureFlag"
                :title="editingItems.customerFeatureFlag ? 'Edit Customer Feature Flag' : 'Create Customer Feature Flag'"
                @close="modals.customerFeatureFlag = false"
            >
                <CustomerFeatureFlagFormComponent
                    :customer-flag="editingItems.customerFeatureFlag"
                    :is-edit="!!editingItems.customerFeatureFlag"
                    :workspaces="workspaces"
                    :feature-flags="featureFlags"
                    @submit="submitCustomerFeatureFlag"
                    @cancel="modals.customerFeatureFlag = false"
                />
            </ModalComponent>

            <ModalComponent
                :visible="modals.evaluation"
                title="Evaluate Feature Flag"
                @close="modals.evaluation = false"
            >
                <EvaluationModalComponent
                    :flag-id="editingItems.evaluationFlagId"
                    @error="showToast"
                    @close="modals.evaluation = false"
                />
            </ModalComponent>

            <!-- Toast Notification -->
            <ToastComponent
                :visible="toast.visible"
                :message="toast.message"
                :type="toast.type"
                @close="closeToast"
            />
        </div>
    `
};

// Create and mount the app
