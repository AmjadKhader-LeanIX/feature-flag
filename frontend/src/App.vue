<template>
  <FioriShellLayout
    ref="shellRef"
    app-title="Feature Flag Manager"
    :nav-items="navItems"
    :active-nav="currentTab"
    @nav-click="handleNavClick"
  >
    <!-- Main Content -->
    <PageLayout
      :title="pageTitle"
      :subtitle="pageSubtitle"
      :loading="isPageLoading"
    >
      <template #actions>
        <FioriButton
          v-if="currentTab === 'feature-flags'"
          variant="primary"
          @click="createFlag"
        >
          Create Feature Flag
        </FioriButton>
      </template>

      <!-- Dashboard Tab -->
      <div v-if="currentTab === 'dashboard'" class="animate-fade-in">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <StatCard
            :icon="Flag"
            :value="featureFlags.length"
            label="Total Flags"
            accent-color="#0059c9"
          />
          <StatCard
            :icon="CheckCircle"
            :value="activeFlags"
            label="Active Flags"
            accent-color="#10b981"
          />
          <StatCard
            :icon="Users"
            :value="uniqueTeams.length"
            label="Teams"
            accent-color="#3b82f6"
          />
        </div>

        <!-- Charts -->
        <DashboardCharts
          :feature-flags="featureFlags"
          :audit-logs="auditLogs"
        />
      </div>

      <!-- Feature Flags Tab -->
      <div v-else-if="currentTab === 'feature-flags'" class="animate-fade-in">
        <SearchBar
          v-model="searchTerms.featureFlag"
          placeholder="Search feature flags..."
          class="mb-6"
        />

        <div v-if="filteredFeatureFlags.length === 0" class="text-center py-12">
          <p class="text-neutral-600">No feature flags found</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <FioriCard
            v-for="flag in filteredFeatureFlags"
            :key="flag.id"
            hover
            class="p-6"
          >
            <!-- Icon + Title -->
            <div class="flex items-start gap-3 mb-3">
              <div class="flex-shrink-0 w-10 h-10 rounded-fiori-lg bg-primary-50 flex items-center justify-center">
                <Flag :size="20" class="text-primary-600" />
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="text-lg font-semibold truncate">{{ flag.name }}</h3>
              </div>
            </div>

            <p class="text-sm text-neutral-600 mb-4">{{ flag.description || 'No description' }}</p>

            <div class="flex gap-2 mb-4">
              <FioriBadge variant="info" :text="flag.team" />
              <FioriBadge
                :variant="flag.rolloutPercentage > 0 ? 'success' : 'neutral'"
                :text="flag.rolloutPercentage > 0 ? 'Active' : 'Inactive'"
              />
            </div>

            <div class="mb-4">
              <div class="flex justify-between text-sm mb-1">
                <span>Rollout</span>
                <span class="font-medium">{{ flag.rolloutPercentage }}%</span>
              </div>
              <div class="h-2 bg-neutral-200 rounded-full overflow-hidden">
                <div
                  class="h-full bg-primary-600 transition-all duration-300"
                  :style="{ width: flag.rolloutPercentage + '%' }"
                ></div>
              </div>
            </div>

            <div class="flex flex-col gap-2">
              <FioriButton size="small" variant="secondary" @click="viewEnabledWorkspaces(flag)" full-width>
                <Eye :size="16" class="mr-1" />
                View Enabled Workspaces
              </FioriButton>
              <div class="flex gap-2">
                <FioriButton size="small" variant="tertiary" @click="manageWorkspaces(flag)" full-width>
                  <Edit3 :size="16" class="mr-1" />
                  Edit
                </FioriButton>
                <FioriButton size="small" variant="critical" @click="deleteFlag(flag)" full-width>
                  Delete
                </FioriButton>
              </div>
            </div>
          </FioriCard>
        </div>
      </div>

      <!-- Workspaces Tab -->
      <div v-else-if="currentTab === 'workspaces'" class="animate-fade-in">
        <SearchBar
          v-model="workspaceSearch"
          placeholder="Search workspaces..."
          class="mb-6"
        />

        <div class="flex justify-between items-center text-sm text-neutral-600 mb-4">
          <p class="flex items-center gap-2">
            <Building2 :size="16" class="text-neutral-500" />
            <span v-if="workspaceSearch">
              Showing {{ workspaces.length }} of {{ totalWorkspaces }} workspaces matching "{{ workspaceSearch }}"
            </span>
            <span v-else>
              Showing {{ workspaces.length }} of {{ totalWorkspaces }} total workspaces
            </span>
          </p>
        </div>

        <div v-if="workspaces.length === 0 && !loading.workspaces" class="text-center py-12">
          <Building2 :size="48" class="mx-auto text-neutral-400 mb-3" />
          <p class="text-neutral-600" v-if="workspaceSearch">No workspaces match your search</p>
          <p class="text-neutral-600" v-else>No workspaces found</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <FioriCard
            v-for="workspace in workspaces"
            :key="workspace.id"
            hover
            class="p-6 cursor-pointer"
            @click="viewWorkspaceFlags(workspace)"
          >
            <div class="flex items-start gap-3 mb-3">
              <div class="flex-shrink-0 w-10 h-10 rounded-fiori-lg bg-primary-50 flex items-center justify-center">
                <Building2 :size="20" class="text-primary-600" />
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="text-lg font-semibold truncate">{{ workspace.name }}</h3>
              </div>
            </div>
            <div class="flex flex-wrap gap-2 mb-3">
              <div v-if="workspace.type" class="flex items-center gap-1">
                <Tag :size="14" class="text-neutral-500" />
                <FioriBadge variant="neutral" :text="workspace.type" size="small" />
              </div>
              <div v-if="workspace.region" class="flex items-center gap-1">
                <MapPin :size="14" class="text-neutral-500" />
                <FioriBadge variant="warning" :text="workspace.region" size="small" />
              </div>
            </div>
            <div class="mt-4 pt-4 border-t border-neutral-200">
              <FioriButton size="small" variant="tertiary" full-width @click="viewWorkspaceFlags(workspace)">
                View Feature Flags
              </FioriButton>
            </div>
          </FioriCard>
        </div>

        <!-- Loading More Indicator -->
        <div v-if="isLoadingMoreWorkspaces" class="text-center py-6">
          <div class="inline-block animate-spin rounded-full h-8 w-8 border-2 border-primary-700 border-t-transparent"></div>
          <p class="mt-2 text-neutral-600 text-sm">Loading more workspaces...</p>
        </div>

        <!-- End of List Indicator -->
        <div v-else-if="!hasMoreWorkspaces && workspaces.length > 0" class="text-center py-6">
          <p class="text-neutral-500 text-sm">All workspaces loaded ({{ totalWorkspaces }} total)</p>
        </div>
      </div>

      <!-- Audit Logs Tab -->
      <div v-else-if="currentTab === 'audit-logs'" class="animate-fade-in">
        <SearchBar
          v-model="searchTerms.auditLog"
          placeholder="Search audit logs..."
          class="mb-6"
        />

        <!-- Results Count -->
        <div class="flex justify-between items-center text-sm text-neutral-600 mb-4">
          <p v-if="searchTerms.auditLog">
            Showing {{ auditLogs.length }} of {{ totalAuditLogs }} audit logs matching "{{ searchTerms.auditLog }}"
          </p>
          <p v-else>
            Showing {{ auditLogs.length }} of {{ totalAuditLogs }} total audit logs
          </p>
        </div>

        <!-- Audit Log Cards (Better for showing changes) -->
        <div class="space-y-4">
          <FioriCard
            v-for="log in filteredAuditLogs"
            :key="log.id"
            class="p-6"
          >
            <div class="flex items-start justify-between mb-4">
              <div class="flex items-start gap-3 flex-1">
                <div class="flex-shrink-0">
                  <FioriBadge
                    :variant="log.operation === 'CREATE' ? 'success' : log.operation === 'UPDATE' ? 'info' : 'danger'"
                    :text="log.operation"
                  />
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="text-base font-semibold text-neutral-900 mb-1">
                    {{ log.featureFlagName }}
                  </h4>
                  <div class="flex items-center gap-2 text-sm text-neutral-600">
                    <Users :size="14" />
                    <span>{{ log.team }}</span>
                    <span class="text-neutral-400">•</span>
                    <span>{{ formatDate(log.timestamp) }}</span>
                    <span v-if="log.changedBy" class="text-neutral-400">•</span>
                    <span v-if="log.changedBy">by {{ log.changedBy }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Change Details -->
            <div v-if="log.operation === 'UPDATE' && (log.oldValues || log.newValues)" class="mt-4 pt-4 border-t border-neutral-200">
              <h5 class="text-sm font-semibold text-neutral-700 mb-3">Changes:</h5>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <!-- Before -->
                <div class="bg-danger-50 rounded-fiori-lg p-3">
                  <div class="flex items-center gap-2 mb-2">
                    <div class="w-1 h-4 bg-danger-500 rounded"></div>
                    <span class="text-xs font-semibold text-danger-700 uppercase">Before</span>
                  </div>
                  <pre class="text-xs text-danger-900 overflow-x-auto">{{ formatJson(log.oldValues) }}</pre>
                </div>
                <!-- After -->
                <div class="bg-success-50 rounded-fiori-lg p-3">
                  <div class="flex items-center gap-2 mb-2">
                    <div class="w-1 h-4 bg-success-500 rounded"></div>
                    <span class="text-xs font-semibold text-success-700 uppercase">After</span>
                  </div>
                  <pre class="text-xs text-success-900 overflow-x-auto">{{ formatJson(log.newValues) }}</pre>
                </div>
              </div>
            </div>

            <!-- CREATE details -->
            <div v-else-if="log.operation === 'CREATE' && log.newValues" class="mt-4 pt-4 border-t border-neutral-200">
              <h5 class="text-sm font-semibold text-neutral-700 mb-3">Created with:</h5>
              <div class="bg-success-50 rounded-fiori-lg p-3">
                <pre class="text-xs text-success-900 overflow-x-auto">{{ formatJson(log.newValues) }}</pre>
              </div>
            </div>

            <!-- DELETE details -->
            <div v-else-if="log.operation === 'DELETE' && log.oldValues" class="mt-4 pt-4 border-t border-neutral-200">
              <h5 class="text-sm font-semibold text-neutral-700 mb-3">Deleted flag:</h5>
              <div class="bg-danger-50 rounded-fiori-lg p-3">
                <pre class="text-xs text-danger-900 overflow-x-auto">{{ formatJson(log.oldValues) }}</pre>
              </div>
            </div>
          </FioriCard>
        </div>

        <!-- Loading More Indicator -->
        <div v-if="isLoadingMoreAuditLogs" class="text-center py-6">
          <div class="inline-block animate-spin rounded-full h-8 w-8 border-2 border-primary-700 border-t-transparent"></div>
          <p class="mt-2 text-neutral-600 text-sm">Loading more audit logs...</p>
        </div>

        <!-- End of List Indicator -->
        <div v-else-if="!hasMoreAuditLogs && auditLogs.length > 0" class="text-center py-6">
          <p class="text-neutral-500 text-sm">All audit logs loaded ({{ totalAuditLogs }} total)</p>
        </div>

        <!-- Empty state -->
        <div v-if="filteredAuditLogs.length === 0 && !loading.auditLogs" class="text-center py-12">
          <p class="text-neutral-600">No audit logs found</p>
        </div>
      </div>
    </PageLayout>

    <!-- Modals -->
    <FioriModal
      :visible="modals.featureFlag"
      :title="editingItems.featureFlag ? 'Edit Feature Flag' : 'Create Feature Flag'"
      @close="closeFeatureFlagModal"
    >
      <FeatureFlagForm
        :feature-flag="editingItems.featureFlag"
        :is-edit="!!editingItems.featureFlag"
        :loading="loading.featureFlags"
        @submit="submitFeatureFlag"
        @cancel="closeFeatureFlagModal"
      />
    </FioriModal>

    <FioriModal
      :visible="modals.workspaceFeatureFlag"
      title="Manage Workspace Rollout"
      size="lg"
      @close="closeWorkspaceModal"
    >
      <WorkspaceFeatureFlagForm
        :feature-flag="editingItems.selectedFlagForWorkspaces"
        :loading="loading.featureFlags"
        @submit="submitWorkspaceFeatureFlag"
        @cancel="closeWorkspaceModal"
      />
    </FioriModal>

    <!-- Workspace Feature Flags Modal -->
    <FioriModal
      :visible="modals.workspaceFlags"
      :title="`Feature Flags for ${editingItems.selectedWorkspaceForFlags?.name || ''}`"
      size="lg"
      @close="closeWorkspaceFlagsModal"
    >
      <div v-if="loadingWorkspaceFlags" class="text-center py-8">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-2 border-primary-700 border-t-transparent"></div>
        <p class="mt-2 text-neutral-600">Loading feature flags...</p>
      </div>

      <div v-else class="space-y-4">
        <!-- Search Bar -->
        <SearchBar
          v-model="workspaceFlagsSearch"
          placeholder="Search by name, description, or team..."
        />

        <!-- Results Count -->
        <div v-if="workspaceFlags.length > 0" class="flex items-center text-sm text-neutral-600">
          <p v-if="workspaceFlagsSearch">
            Showing {{ workspaceFlags.length }} of {{ totalWorkspaceFlags }} feature flags matching "{{ workspaceFlagsSearch }}"
          </p>
          <p v-else>
            Showing {{ workspaceFlags.length }} of {{ totalWorkspaceFlags }} feature flags
          </p>
        </div>

        <!-- Empty State - No flags at all -->
        <div v-if="workspaceFlags.length === 0 && !workspaceFlagsSearch" class="text-center py-8">
          <Flag :size="48" class="mx-auto text-neutral-400 mb-3" />
          <p class="text-neutral-600">No feature flags are enabled for this workspace</p>
        </div>

        <!-- Empty State - No search results -->
        <div v-else-if="workspaceFlags.length === 0 && workspaceFlagsSearch" class="text-center py-8">
          <Flag :size="48" class="mx-auto text-neutral-400 mb-3" />
          <p class="text-neutral-600">No feature flags match your search</p>
        </div>

        <!-- Feature Flags List with Scroll -->
        <div v-else class="grid grid-cols-1 gap-3 max-h-96 overflow-y-auto" @scroll="handleWorkspaceFlagsScroll">
          <div
            v-for="flag in workspaceFlags"
            :key="flag.id"
            class="flex items-start gap-3 p-3 bg-white border border-neutral-200 rounded-fiori-lg hover:shadow-fiori-md transition-all"
          >
            <div class="flex-shrink-0 w-10 h-10 rounded-fiori-lg bg-primary-50 flex items-center justify-center">
              <Flag :size="20" class="text-primary-600" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-semibold text-neutral-900 truncate">{{ flag.name }}</h4>
              <p v-if="flag.description" class="text-sm text-neutral-600 mt-1">{{ flag.description }}</p>
              <div class="flex flex-wrap gap-2 mt-2">
                <FioriBadge variant="info" :text="flag.team" size="small" />
                <FioriBadge
                  :variant="flag.rolloutPercentage > 0 ? 'success' : 'neutral'"
                  :text="flag.rolloutPercentage > 0 ? 'Active' : 'Inactive'"
                  size="small"
                />
                <FioriBadge
                  variant="warning"
                  :text="`${flag.rolloutPercentage}% rollout`"
                  size="small"
                />
              </div>
            </div>
          </div>

          <!-- Loading More Indicator -->
          <div v-if="isLoadingMoreWorkspaceFlags" class="text-center py-4">
            <div class="inline-block animate-spin rounded-full h-6 w-6 border-2 border-primary-700 border-t-transparent"></div>
            <p class="mt-2 text-neutral-600 text-xs">Loading more...</p>
          </div>

          <!-- End of List Indicator -->
          <div v-else-if="!hasMoreWorkspaceFlags && workspaceFlags.length > 0" class="text-center py-4">
            <p class="text-neutral-500 text-xs">All feature flags loaded ({{ totalWorkspaceFlags }} total)</p>
          </div>
        </div>
      </div>
    </FioriModal>

    <!-- Enabled Workspaces Modal -->
    <FioriModal
      :visible="modals.enabledWorkspaces"
      :title="`Enabled Workspaces for ${editingItems.selectedFlagForViewing?.name || ''}`"
      size="lg"
      @close="closeEnabledWorkspacesModal"
    >
      <div v-if="loadingEnabledWorkspaces" class="text-center py-8">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-2 border-primary-700 border-t-transparent"></div>
        <p class="mt-2 text-neutral-600">Loading workspaces...</p>
      </div>

      <div v-else-if="enabledWorkspaces.length === 0 && !enabledWorkspacesSearch" class="text-center py-8">
        <Building2 :size="48" class="mx-auto text-neutral-400 mb-3" />
        <p class="text-neutral-600">No workspaces have this feature flag enabled</p>
      </div>

      <div v-else class="space-y-4">
        <!-- Search Bar -->
        <SearchBar
          v-model="enabledWorkspacesSearch"
          placeholder="Search by name, type, or region..."
        />

        <!-- Results Count -->
        <div class="flex items-center text-sm text-neutral-600">
          <p v-if="enabledWorkspacesSearch">
            Showing {{ enabledWorkspaces.length }} of {{ totalEnabledWorkspaces }} workspaces matching "{{ enabledWorkspacesSearch }}"
          </p>
          <p v-else>
            Showing {{ enabledWorkspaces.length }} of {{ totalEnabledWorkspaces }} workspaces
          </p>
        </div>

        <!-- Region Breakdown -->
        <div v-if="regionCounts.length > 0 && !enabledWorkspacesSearch" class="bg-neutral-50 border border-neutral-200 rounded-fiori-lg p-4">
          <h4 class="text-sm font-semibold text-neutral-700 mb-3 flex items-center gap-2">
            <MapPin :size="16" />
            Enabled by Region
          </h4>
          <div class="flex flex-wrap gap-3">
            <div
              v-for="regionCount in regionCounts"
              :key="regionCount.region"
              class="flex items-center gap-2 px-3 py-2 bg-white border border-neutral-200 rounded-fiori-md"
            >
              <FioriBadge variant="warning" :text="regionCount.region" size="small" />
              <span class="text-sm font-semibold text-neutral-900">
                {{ regionCount.enabledCount }} out of {{ regionCount.totalCount }}
              </span>
            </div>
          </div>
        </div>

        <!-- No Results -->
        <div v-if="enabledWorkspaces.length === 0" class="text-center py-8">
          <Building2 :size="48" class="mx-auto text-neutral-400 mb-3" />
          <p class="text-neutral-600">No workspaces match your search</p>
        </div>

        <!-- Workspace List with Scroll -->
        <div v-else class="grid grid-cols-1 gap-3 max-h-96 overflow-y-auto" @scroll="handleEnabledWorkspacesScroll">
          <div
            v-for="workspace in enabledWorkspaces"
            :key="workspace.id"
            class="flex items-start gap-3 p-3 bg-white border border-neutral-200 rounded-fiori-lg hover:shadow-fiori-md transition-all"
          >
            <div class="flex-shrink-0 w-10 h-10 rounded-fiori-lg bg-primary-50 flex items-center justify-center">
              <Building2 :size="20" class="text-primary-600" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-semibold text-neutral-900 truncate">{{ workspace.name }}</h4>
              <div class="flex flex-wrap gap-2 mt-2">
                <div v-if="workspace.type" class="flex items-center gap-1">
                  <Tag :size="12" class="text-neutral-500" />
                  <FioriBadge variant="neutral" :text="workspace.type" size="small" />
                </div>
                <div v-if="workspace.region" class="flex items-center gap-1">
                  <MapPin :size="12" class="text-neutral-500" />
                  <FioriBadge variant="warning" :text="workspace.region" size="small" />
                </div>
                <div v-if="workspace.rolloutPercentage !== undefined">
                  <FioriBadge variant="info" :text="`${workspace.rolloutPercentage}% rollout`" size="small" />
                </div>
              </div>
            </div>
          </div>

          <!-- Loading More Indicator -->
          <div v-if="isLoadingMoreEnabledWorkspaces" class="text-center py-4">
            <div class="inline-block animate-spin rounded-full h-6 w-6 border-2 border-primary-700 border-t-transparent"></div>
            <p class="mt-2 text-neutral-600 text-xs">Loading more...</p>
          </div>

          <!-- End of List Indicator -->
          <div v-else-if="!hasMoreEnabledWorkspaces && enabledWorkspaces.length > 0" class="text-center py-4">
            <p class="text-neutral-500 text-xs">All workspaces loaded ({{ totalEnabledWorkspaces }} total)</p>
          </div>
        </div>
      </div>
    </FioriModal>
  </FioriShellLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { FioriShellLayout, PageLayout } from './components/layouts'
import { FioriButton, FioriCard, FioriBadge } from './components/atoms'
import { SearchBar, StatCard } from './components/molecules'
import { FioriTable, FioriModal, DashboardCharts } from './components/organisms'
import FeatureFlagForm from './components/organisms/FeatureFlagForm.vue'
import WorkspaceFeatureFlagForm from './components/organisms/WorkspaceFeatureFlagForm.vue'
import apiService from './services/api-service'
import { Flag, CheckCircle, Users, Building2, MapPin, Tag, Eye, Edit3 } from 'lucide-vue-next'

// State
const currentTab = ref('dashboard')
const shellRef = ref(null)

const loading = reactive({
  featureFlags: false,
  auditLogs: false,
  workspaces: false
})

const featureFlags = ref([])
const auditLogs = ref([])
const auditLogsPage = ref(0)
const totalAuditLogs = ref(0)
const isLoadingMoreAuditLogs = ref(false)
const hasMoreAuditLogs = ref(true)

const workspaces = ref([])

const searchTerms = reactive({
  featureFlag: '',
  auditLog: ''
})

const workspaceSearch = ref('')

const modals = reactive({
  featureFlag: false,
  workspaceFeatureFlag: false,
  enabledWorkspaces: false,
  workspaceFlags: false
})

const editingItems = reactive({
  featureFlag: null,
  selectedFlagForWorkspaces: null,
  selectedFlagForViewing: null,
  selectedWorkspaceForFlags: null
})

const enabledWorkspaces = ref([])
const loadingEnabledWorkspaces = ref(false)
const enabledWorkspacesSearch = ref('')
const enabledWorkspacesPage = ref(0)
const totalEnabledWorkspaces = ref(0)
const isLoadingMoreEnabledWorkspaces = ref(false)
const hasMoreEnabledWorkspaces = ref(true)
const regionCounts = ref([])

const workspaceFlags = ref([])
const loadingWorkspaceFlags = ref(false)
const workspaceFlagsSearch = ref('')
const workspaceFlagsPage = ref(0)
const totalWorkspaceFlags = ref(0)
const isLoadingMoreWorkspaceFlags = ref(false)
const hasMoreWorkspaceFlags = ref(true)

const workspacePage = ref(0)
const totalWorkspaces = ref(0)
const isLoadingMoreWorkspaces = ref(false)
const hasMoreWorkspaces = ref(true)

// Navigation items
const navItems = computed(() => [
  { id: 'dashboard', label: 'Dashboard', active: currentTab.value === 'dashboard' },
  { id: 'feature-flags', label: 'Feature Flags', active: currentTab.value === 'feature-flags' },
  { id: 'workspaces', label: 'Workspaces', active: currentTab.value === 'workspaces' },
  { id: 'audit-logs', label: 'Audit Logs', active: currentTab.value === 'audit-logs' }
])

// Computed
const pageTitle = computed(() => {
  const titles = {
    'dashboard': 'Dashboard',
    'feature-flags': 'Feature Flags',
    'workspaces': 'Workspaces',
    'audit-logs': 'Audit Logs'
  }
  return titles[currentTab.value] || ''
})

const pageSubtitle = computed(() => {
  const subtitles = {
    'dashboard': 'Overview of your feature flags',
    'feature-flags': 'Control feature rollouts and experiments',
    'workspaces': 'View all workspaces and their enabled feature flags',
    'audit-logs': 'View complete history of feature flag changes'
  }
  return subtitles[currentTab.value] || ''
})

const isPageLoading = computed(() => {
  return loading.featureFlags || loading.auditLogs || loading.workspaces
})

const filteredFeatureFlags = computed(() => {
  return featureFlags.value.filter(flag => {
    if (!searchTerms.featureFlag) return true
    const search = searchTerms.featureFlag.toLowerCase()
    return flag.name.toLowerCase().includes(search) ||
           flag.description?.toLowerCase().includes(search) ||
           flag.team.toLowerCase().includes(search)
  })
})

const filteredAuditLogs = computed(() => {
  // Server-side search is now handled in loadAuditLogs
  return auditLogs.value
})

const activeFlags = computed(() => {
  return featureFlags.value.filter(f => f.rolloutPercentage > 0).length
})

const uniqueTeams = computed(() => {
  return [...new Set(featureFlags.value.map(f => f.team))].sort()
})


// Audit table columns
const auditColumns = [
  { key: 'operation', label: 'Operation', sortable: true },
  { key: 'featureFlagName', label: 'Feature Flag', sortable: true },
  { key: 'team', label: 'Team', sortable: true },
  { key: 'timestamp', label: 'Timestamp', sortable: true },
  { key: 'changedBy', label: 'Changed By', sortable: false }
]

// Methods
const handleNavClick = (item) => {
  currentTab.value = item.id

  if (item.id === 'dashboard') {
    loadFeatureFlags()
    loadAuditLogs()
  } else if (item.id === 'feature-flags') {
    loadFeatureFlags()
  } else if (item.id === 'workspaces') {
    loadWorkspaces()
  } else if (item.id === 'audit-logs') {
    loadAuditLogs()
  }
}

const loadFeatureFlags = async () => {
  try {
    loading.featureFlags = true
    featureFlags.value = await apiService.getFeatureFlags()
  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: error.message
    })
  } finally {
    loading.featureFlags = false
  }
}

const loadAuditLogs = async (reset = true) => {
  try {
    if (reset) {
      loading.auditLogs = true
      auditLogsPage.value = 0
      auditLogs.value = []
    }

    const response = await apiService.getAuditLogs(
      auditLogsPage.value,
      100,
      searchTerms.auditLog
    )

    if (reset) {
      auditLogs.value = response.content || []
    } else {
      auditLogs.value = [...auditLogs.value, ...(response.content || [])]
    }

    totalAuditLogs.value = response.totalElements || 0
    hasMoreAuditLogs.value = !response.last

  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: error.message
    })
  } finally {
    loading.auditLogs = false
    isLoadingMoreAuditLogs.value = false
  }
}

const loadMoreAuditLogs = async () => {
  if (isLoadingMoreAuditLogs.value || !hasMoreAuditLogs.value) return

  isLoadingMoreAuditLogs.value = true
  auditLogsPage.value++
  await loadAuditLogs(false)
}

const handleAuditLogsScroll = () => {
  const scrollPosition = window.innerHeight + window.scrollY
  const threshold = document.documentElement.scrollHeight * 0.8

  if (scrollPosition >= threshold && currentTab.value === 'audit-logs') {
    loadMoreAuditLogs()
  }
}

const loadWorkspaces = async (reset = true) => {
  try {
    if (reset) {
      loading.workspaces = true
      workspacePage.value = 0
      workspaces.value = []
    }

    const response = await apiService.getWorkspaces(workspacePage.value, 100, workspaceSearch.value)

    if (reset) {
      workspaces.value = response.content || []
    } else {
      workspaces.value = [...workspaces.value, ...(response.content || [])]
    }

    totalWorkspaces.value = response.totalElements || 0
    hasMoreWorkspaces.value = !response.last

  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: error.message
    })
  } finally {
    loading.workspaces = false
    isLoadingMoreWorkspaces.value = false
  }
}

const loadMoreWorkspaces = async () => {
  if (isLoadingMoreWorkspaces.value || !hasMoreWorkspaces.value) return

  isLoadingMoreWorkspaces.value = true
  workspacePage.value++
  await loadWorkspaces(false)
}

const handleWorkspaceScroll = () => {
  const scrollPosition = window.innerHeight + window.scrollY
  const threshold = document.documentElement.scrollHeight * 0.8

  if (scrollPosition >= threshold && currentTab.value === 'workspaces') {
    loadMoreWorkspaces()
  }
}

const createFlag = () => {
  editingItems.featureFlag = null
  modals.featureFlag = true
}

const editFlag = (flag) => {
  editingItems.featureFlag = flag
  modals.featureFlag = true
}

const deleteFlag = async (flag) => {
  if (!confirm(`Delete "${flag.name}"?`)) return

  try {
    await apiService.deleteFeatureFlag(flag.id)
    shellRef.value?.showNotification({
      type: 'success',
      message: 'Feature flag deleted'
    })
    loadFeatureFlags()
  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: error.message
    })
  }
}

const loadEnabledWorkspaces = async (flag, reset = true) => {
  try {
    if (reset) {
      loadingEnabledWorkspaces.value = true
      enabledWorkspacesPage.value = 0
      enabledWorkspaces.value = []

      // Load region counts when first opening the modal
      try {
        const counts = await apiService.getWorkspaceCountsByRegion(flag.id)
        regionCounts.value = counts || []
      } catch (err) {
        console.error('Failed to load region counts:', err)
        regionCounts.value = []
      }
    }

    const response = await apiService.getEnabledWorkspacesForFeatureFlag(
      flag.id,
      enabledWorkspacesPage.value,
      100,
      enabledWorkspacesSearch.value
    )

    if (reset) {
      enabledWorkspaces.value = response.content || []
    } else {
      enabledWorkspaces.value = [...enabledWorkspaces.value, ...(response.content || [])]
    }

    totalEnabledWorkspaces.value = response.totalElements || 0
    hasMoreEnabledWorkspaces.value = !response.last

  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: 'Failed to load enabled workspaces: ' + error.message
    })
  } finally {
    loadingEnabledWorkspaces.value = false
    isLoadingMoreEnabledWorkspaces.value = false
  }
}

const viewEnabledWorkspaces = async (flag) => {
  editingItems.selectedFlagForViewing = flag
  modals.enabledWorkspaces = true
  await loadEnabledWorkspaces(flag, true)
}

const loadMoreEnabledWorkspaces = async () => {
  if (isLoadingMoreEnabledWorkspaces.value || !hasMoreEnabledWorkspaces.value) return
  if (!editingItems.selectedFlagForViewing) return

  isLoadingMoreEnabledWorkspaces.value = true
  enabledWorkspacesPage.value++
  await loadEnabledWorkspaces(editingItems.selectedFlagForViewing, false)
}

const handleEnabledWorkspacesScroll = (event) => {
  const element = event.target
  const scrollPosition = element.scrollTop + element.clientHeight
  const threshold = element.scrollHeight * 0.8

  if (scrollPosition >= threshold && modals.enabledWorkspaces) {
    loadMoreEnabledWorkspaces()
  }
}

const manageWorkspaces = (flag) => {
  editingItems.selectedFlagForWorkspaces = flag
  modals.workspaceFeatureFlag = true
}

const submitFeatureFlag = async (data) => {
  try {
    loading.featureFlags = true
    if (editingItems.featureFlag) {
      await apiService.updateFeatureFlag(editingItems.featureFlag.id, data)
      shellRef.value?.showNotification({
        type: 'success',
        message: 'Feature flag updated'
      })
    } else {
      await apiService.createFeatureFlag(data)
      shellRef.value?.showNotification({
        type: 'success',
        message: 'Feature flag created'
      })
    }
    closeFeatureFlagModal()
    loadFeatureFlags()
  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: error.message
    })
  } finally {
    loading.featureFlags = false
  }
}

const submitWorkspaceFeatureFlag = async (data) => {
  try {
    loading.featureFlags = true
    await apiService.updateWorkspaceFeatureFlags(
      editingItems.selectedFlagForWorkspaces.id,
      {
        workspaceIds: data.workspaceIds,
        excludedWorkspaceIds: data.excludedWorkspaceIds,
        enabled: data.enabled,
        rolloutPercentage: data.rolloutPercentage,
        targetRegion: data.targetRegion
      }
    )
    shellRef.value?.showNotification({
      type: 'success',
      message: 'Workspace rollout updated'
    })
    closeWorkspaceModal()
    loadFeatureFlags()
  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: error.message
    })
  } finally {
    loading.featureFlags = false
  }
}

const closeFeatureFlagModal = () => {
  modals.featureFlag = false
  editingItems.featureFlag = null
}

const closeWorkspaceModal = () => {
  modals.workspaceFeatureFlag = false
  editingItems.selectedFlagForWorkspaces = null
}

const closeEnabledWorkspacesModal = () => {
  modals.enabledWorkspaces = false
  editingItems.selectedFlagForViewing = null
  enabledWorkspacesSearch.value = ''
  enabledWorkspacesPage.value = 0
  enabledWorkspaces.value = []
}

const loadWorkspaceFlags = async (workspace, reset = true) => {
  try {
    if (reset) {
      loadingWorkspaceFlags.value = true
      workspaceFlagsPage.value = 0
      workspaceFlags.value = []
    }

    const response = await apiService.getEnabledFeatureFlagsForWorkspace(
      workspace.id,
      workspaceFlagsPage.value,
      100,
      workspaceFlagsSearch.value
    )

    if (reset) {
      workspaceFlags.value = response.content || []
    } else {
      workspaceFlags.value = [...workspaceFlags.value, ...(response.content || [])]
    }

    totalWorkspaceFlags.value = response.totalElements || 0
    hasMoreWorkspaceFlags.value = !response.last

  } catch (error) {
    shellRef.value?.showNotification({
      type: 'error',
      message: 'Failed to load feature flags: ' + error.message
    })
  } finally {
    loadingWorkspaceFlags.value = false
    isLoadingMoreWorkspaceFlags.value = false
  }
}

const viewWorkspaceFlags = async (workspace) => {
  editingItems.selectedWorkspaceForFlags = workspace
  modals.workspaceFlags = true
  await loadWorkspaceFlags(workspace, true)
}

const loadMoreWorkspaceFlags = async () => {
  if (isLoadingMoreWorkspaceFlags.value || !hasMoreWorkspaceFlags.value) return
  if (!editingItems.selectedWorkspaceForFlags) return

  isLoadingMoreWorkspaceFlags.value = true
  workspaceFlagsPage.value++
  await loadWorkspaceFlags(editingItems.selectedWorkspaceForFlags, false)
}

const handleWorkspaceFlagsScroll = (event) => {
  const element = event.target
  const scrollPosition = element.scrollTop + element.clientHeight
  const threshold = element.scrollHeight * 0.8

  if (scrollPosition >= threshold && modals.workspaceFlags) {
    loadMoreWorkspaceFlags()
  }
}

const closeWorkspaceFlagsModal = () => {
  modals.workspaceFlags = false
  editingItems.selectedWorkspaceForFlags = null
  workspaceFlagsSearch.value = ''
  workspaceFlagsPage.value = 0
  workspaceFlags.value = []
}

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleString()
}

const formatJson = (value) => {
  if (!value) return 'N/A'

  try {
    // If it's a string, try to parse it
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    // If parsing fails, return as-is
    return typeof value === 'string' ? value : JSON.stringify(value, null, 2)
  }
}

// Watchers
let workspaceSearchTimeout = null
watch(workspaceSearch, (newValue) => {
  // Debounce search to avoid too many API calls
  clearTimeout(workspaceSearchTimeout)
  workspaceSearchTimeout = setTimeout(() => {
    loadWorkspaces(true)
  }, 300)
})

let workspaceFlagsSearchTimeout = null
watch(workspaceFlagsSearch, (newValue) => {
  if (editingItems.selectedWorkspaceForFlags) {
    clearTimeout(workspaceFlagsSearchTimeout)
    workspaceFlagsSearchTimeout = setTimeout(() => {
      loadWorkspaceFlags(editingItems.selectedWorkspaceForFlags, true)
    }, 300)
  }
})

let enabledWorkspacesSearchTimeout = null
watch(enabledWorkspacesSearch, (newValue) => {
  if (editingItems.selectedFlagForViewing) {
    clearTimeout(enabledWorkspacesSearchTimeout)
    enabledWorkspacesSearchTimeout = setTimeout(() => {
      loadEnabledWorkspaces(editingItems.selectedFlagForViewing, true)
    }, 300)
  }
})

let auditLogSearchTimeout = null
watch(() => searchTerms.auditLog, (newValue) => {
  clearTimeout(auditLogSearchTimeout)
  auditLogSearchTimeout = setTimeout(() => {
    loadAuditLogs(true)
  }, 300)
})

// Lifecycle
onMounted(() => {
  loadFeatureFlags()
  loadAuditLogs()
  window.addEventListener('scroll', handleWorkspaceScroll)
  window.addEventListener('scroll', handleAuditLogsScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleWorkspaceScroll)
  window.removeEventListener('scroll', handleAuditLogsScroll)
  clearTimeout(workspaceSearchTimeout)
  clearTimeout(workspaceFlagsSearchTimeout)
  clearTimeout(enabledWorkspacesSearchTimeout)
  clearTimeout(auditLogSearchTimeout)
})
</script>

<style scoped>
/* Additional page-specific styles */
</style>
