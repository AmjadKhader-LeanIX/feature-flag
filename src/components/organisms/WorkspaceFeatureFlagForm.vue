<template>
  <div class="workspace-feature-flag-form">
    <!-- Rollout Percentage -->
    <FormGroup
      label="Rollout Percentage"
      :helper-text="`Adjust the rollout percentage: ${rolloutPercentage}%`"
    >
      <div class="rollout-slider">
        <input
          v-model.number="rolloutPercentage"
          type="range"
          min="0"
          max="100"
          :disabled="loading"
          class="slider"
        />
        <div class="rollout-display">
          <span class="rollout-value">{{ rolloutPercentage }}%</span>
          <div class="rollout-bar">
            <div
              class="rollout-fill"
              :style="{ width: rolloutPercentage + '%' }"
            ></div>
          </div>
        </div>
      </div>
    </FormGroup>

    <!-- Target Region -->
    <FormGroup
      label="Target Region"
      helper-text="Optional: Target a specific region for this rollout"
    >
      <select
        v-model="selectedRegion"
        :disabled="loading"
        class="fiori-select"
      >
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
    </FormGroup>

    <!-- Pick Workspaces -->
    <FormGroup
      label="Pick Workspaces to Prioritize"
      helper-text="Search and select specific workspaces to enable this feature flag"
    >
      <SearchBar
        v-model="searchTerm"
        placeholder="Search for workspaces to pick..."
        :loading="loadingSearch"
        @search="searchWorkspaces"
      />

      <!-- Search Results Dropdown -->
      <div v-if="showDropdown && searchResults.length > 0" class="search-dropdown">
        <div
          v-for="workspace in searchResults"
          :key="workspace.id"
          class="search-result-item"
          @click="pickWorkspace(workspace)"
        >
          <div class="flex-1">
            <div class="font-medium">{{ workspace.name }}</div>
            <div class="flex gap-2 mt-1">
              <FioriBadge v-if="workspace.type" variant="neutral" :text="workspace.type" size="small" />
              <FioriBadge v-if="workspace.region" variant="warning" :text="workspace.region" size="small" />
            </div>
          </div>
        </div>
      </div>

      <!-- Picked Workspaces List -->
      <div v-if="pickedWorkspaces.length > 0" class="mt-4">
        <p class="text-sm font-medium text-neutral-700 mb-2">
          Picked Workspaces ({{ pickedCount }})
        </p>
        <div class="space-y-2">
          <div
            v-for="workspace in pickedWorkspaces"
            :key="workspace.id"
            class="picked-workspace-item"
          >
            <div class="flex-1">
              <div class="font-medium">{{ workspace.name }}</div>
              <div class="flex gap-2 mt-1">
                <FioriBadge v-if="workspace.type" variant="neutral" :text="workspace.type" size="small" />
                <FioriBadge v-if="workspace.region" variant="warning" :text="workspace.region" size="small" />
              </div>
            </div>
            <button
              type="button"
              class="remove-button"
              @click="removePickedWorkspace(workspace.id)"
              :disabled="loading"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 6 6 18"></path>
                <path d="m6 6 12 12"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </FormGroup>

    <!-- Exclude Workspaces -->
    <FormGroup
      label="Exclude Workspaces"
      helper-text="Search and select workspaces to always keep this feature flag disabled"
    >
      <SearchBar
        v-model="excludeSearchTerm"
        placeholder="Search for workspaces to exclude..."
        :loading="loadingExcludeSearch"
        @search="searchExcludeWorkspaces"
      />

      <!-- Exclude Search Results Dropdown -->
      <div v-if="showExcludeDropdown && excludeSearchResults.length > 0" class="search-dropdown">
        <div
          v-for="workspace in excludeSearchResults"
          :key="workspace.id"
          class="search-result-item"
          @click="excludeWorkspace(workspace)"
        >
          <div class="flex-1">
            <div class="font-medium">{{ workspace.name }}</div>
            <div class="flex gap-2 mt-1">
              <FioriBadge v-if="workspace.type" variant="neutral" :text="workspace.type" size="small" />
              <FioriBadge v-if="workspace.region" variant="warning" :text="workspace.region" size="small" />
            </div>
          </div>
        </div>
      </div>

      <!-- Excluded Workspaces List -->
      <div v-if="excludedWorkspaces.length > 0" class="mt-4">
        <p class="text-sm font-medium text-neutral-700 mb-2">
          Excluded Workspaces ({{ excludedCount }})
        </p>
        <div class="space-y-2">
          <div
            v-for="workspace in excludedWorkspaces"
            :key="workspace.id"
            class="excluded-workspace-item"
          >
            <div class="flex-1">
              <div class="font-medium">{{ workspace.name }}</div>
              <div class="flex gap-2 mt-1">
                <FioriBadge v-if="workspace.type" variant="neutral" :text="workspace.type" size="small" />
                <FioriBadge v-if="workspace.region" variant="warning" :text="workspace.region" size="small" />
              </div>
            </div>
            <button
              type="button"
              class="remove-button"
              @click="removeExcludedWorkspace(workspace.id)"
              :disabled="loading"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 6 6 18"></path>
                <path d="m6 6 12 12"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </FormGroup>

    <!-- Form Actions -->
    <div class="form-actions">
      <FioriButton
        type="button"
        variant="secondary"
        @click="handleCancel"
        :disabled="loading"
      >
        Cancel
      </FioriButton>
      <FioriButton
        type="button"
        variant="primary"
        @click="handleSubmit"
        :loading="loading"
        :disabled="!hasChanges"
      >
        Update Workspaces
      </FioriButton>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { FioriButton, FioriCard, FioriBadge } from '../atoms'
import { FormGroup, SearchBar } from '../molecules'
import { useDebouncedSearch } from '@/composables'
import apiService from '@/services/api-service'

const props = defineProps({
  featureFlag: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit', 'cancel'])

// State
const searchTerm = ref('')
const searchResults = ref([])
const loadingSearch = ref(false)
const showDropdown = ref(false)

const excludeSearchTerm = ref('')
const excludeSearchResults = ref([])
const loadingExcludeSearch = ref(false)
const showExcludeDropdown = ref(false)

const pickedWorkspaces = ref([])
const excludedWorkspaces = ref([])
const rolloutPercentage = ref(0)
const initialRolloutPercentage = ref(0)
const selectedRegion = ref(null)

// Computed
const hasRolloutChanged = computed(() => rolloutPercentage.value !== initialRolloutPercentage.value)
const pickedCount = computed(() => pickedWorkspaces.value.length)
const excludedCount = computed(() => excludedWorkspaces.value.length)
const hasChanges = computed(() =>
  hasRolloutChanged.value || pickedWorkspaces.value.length > 0 || excludedWorkspaces.value.length > 0
)

// Methods
const searchWorkspaces = async () => {
  if (!searchTerm.value || searchTerm.value.length < 2) {
    searchResults.value = []
    showDropdown.value = false
    return
  }

  try {
    loadingSearch.value = true
    const response = await apiService.getWorkspaces(0, 20, searchTerm.value)
    searchResults.value = response.content || []
    showDropdown.value = searchResults.value.length > 0
  } catch (error) {
    console.error('Failed to search workspaces:', error)
    searchResults.value = []
    showDropdown.value = false
  } finally {
    loadingSearch.value = false
  }
}

const pickWorkspace = (workspace) => {
  const alreadyPicked = pickedWorkspaces.value.some(w => w.id === workspace.id)
  if (!alreadyPicked) {
    pickedWorkspaces.value.push(workspace)
  }
  searchTerm.value = ''
  searchResults.value = []
  showDropdown.value = false
}

const removePickedWorkspace = (workspaceId) => {
  pickedWorkspaces.value = pickedWorkspaces.value.filter(w => w.id !== workspaceId)
}

const searchExcludeWorkspaces = async () => {
  if (!excludeSearchTerm.value || excludeSearchTerm.value.length < 2) {
    excludeSearchResults.value = []
    showExcludeDropdown.value = false
    return
  }

  try {
    loadingExcludeSearch.value = true
    const response = await apiService.getWorkspaces(0, 20, excludeSearchTerm.value)
    excludeSearchResults.value = response.content || []
    showExcludeDropdown.value = excludeSearchResults.value.length > 0
  } catch (error) {
    console.error('Failed to search workspaces:', error)
    excludeSearchResults.value = []
    showExcludeDropdown.value = false
  } finally {
    loadingExcludeSearch.value = false
  }
}

const excludeWorkspace = (workspace) => {
  const alreadyExcluded = excludedWorkspaces.value.some(w => w.id === workspace.id)
  if (!alreadyExcluded) {
    excludedWorkspaces.value.push(workspace)
  }
  excludeSearchTerm.value = ''
  excludeSearchResults.value = []
  showExcludeDropdown.value = false
}

const removeExcludedWorkspace = (workspaceId) => {
  excludedWorkspaces.value = excludedWorkspaces.value.filter(w => w.id !== workspaceId)
}

const handleSubmit = () => {
  if (!hasChanges.value) {
    alert('Please change the rollout percentage, pick workspaces to prioritize, or exclude workspaces')
    return
  }

  emit('submit', {
    workspaceIds: pickedWorkspaces.value.map(w => w.id),
    excludedWorkspaceIds: excludedWorkspaces.value.map(w => w.id),
    enabled: true,
    rolloutPercentage: rolloutPercentage.value,
    targetRegion: selectedRegion.value
  })
}

const handleCancel = () => {
  emit('cancel')
}

// Debounced search
useDebouncedSearch(searchTerm, searchWorkspaces, 300)
useDebouncedSearch(excludeSearchTerm, searchExcludeWorkspaces, 300)

// Initialize
onMounted(() => {
  rolloutPercentage.value = props.featureFlag.rolloutPercentage || 0
  initialRolloutPercentage.value = props.featureFlag.rolloutPercentage || 0
})

// Watch for feature flag changes
watch(() => props.featureFlag, (newFlag) => {
  if (newFlag) {
    rolloutPercentage.value = newFlag.rolloutPercentage || 0
    initialRolloutPercentage.value = newFlag.rolloutPercentage || 0
  }
})
</script>

<style scoped>
.workspace-feature-flag-form {
  @apply space-y-6;
}

.fiori-select {
  @apply w-full px-4 py-2 text-base;
  @apply border border-neutral-300 rounded-fiori-lg;
  @apply transition-all duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500;
  @apply disabled:bg-neutral-100 disabled:cursor-not-allowed;
}

.rollout-slider {
  @apply space-y-3;
}

.slider {
  @apply w-full h-2 rounded-full;
  @apply appearance-none cursor-pointer;
  background: linear-gradient(to right, #e5e7eb 0%, #002a86 100%);
}

.slider::-webkit-slider-thumb {
  @apply appearance-none w-5 h-5 rounded-full bg-primary-700;
  @apply border-2 border-white shadow-fiori-md;
  @apply cursor-pointer;
  @apply hover:scale-110 transition-transform;
}

.slider::-moz-range-thumb {
  @apply w-5 h-5 rounded-full bg-primary-700;
  @apply border-2 border-white shadow-fiori-md;
  @apply cursor-pointer;
}

.slider:disabled {
  @apply opacity-50 cursor-not-allowed;
}

.rollout-display {
  @apply space-y-2;
}

.rollout-value {
  @apply text-2xl font-bold text-primary-700;
}

.rollout-bar {
  @apply h-3 bg-neutral-200 rounded-full overflow-hidden;
}

.rollout-fill {
  @apply h-full bg-primary-600 transition-all duration-300;
}

.search-dropdown {
  @apply absolute top-full left-0 right-0 mt-2 z-50;
  @apply bg-white border border-neutral-200 rounded-fiori-lg shadow-fiori-lg;
  @apply max-h-64 overflow-y-auto;
}

.search-result-item {
  @apply px-4 py-3;
  @apply cursor-pointer;
  @apply transition-colors duration-150;
  @apply hover:bg-primary-50;
  @apply border-b border-neutral-100 last:border-0;
  @apply flex items-start gap-3;
}

.picked-workspace-item,
.excluded-workspace-item {
  @apply flex items-start gap-3;
  @apply p-3;
  @apply bg-white border border-neutral-200 rounded-fiori-lg;
  @apply transition-all duration-200;
  @apply hover:shadow-fiori-md;
}

.remove-button {
  @apply p-2;
  @apply text-neutral-400 hover:text-danger-600 hover:bg-danger-50;
  @apply rounded-fiori-md;
  @apply transition-all duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-danger-500;
  @apply disabled:opacity-50 disabled:cursor-not-allowed;
}

.form-actions {
  @apply flex items-center justify-end gap-3;
  @apply pt-6 border-t border-neutral-200;
}
</style>
