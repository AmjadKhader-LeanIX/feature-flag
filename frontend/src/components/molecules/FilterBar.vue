<template>
  <div class="filter-bar">
    <!-- Filter label -->
    <div v-if="label" class="filter-label">
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
      </svg>
      <span class="text-sm font-medium text-neutral-700">{{ label }}</span>
    </div>

    <!-- Filter controls -->
    <div class="filter-controls">
      <!-- Dynamic filter slots -->
      <div
        v-for="(filter, key) in filters"
        :key="key"
        class="filter-item"
      >
        <label :for="`filter-${key}`" class="filter-item-label">
          {{ filter.label }}
        </label>
        <select
          :id="`filter-${key}`"
          :value="filter.value"
          :class="selectClasses"
          @change="handleFilterChange(key, $event.target.value)"
        >
          <option value="">{{ filter.placeholder || 'All' }}</option>
          <option
            v-for="option in filter.options"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </div>

      <!-- Custom slot for additional filters -->
      <slot></slot>
    </div>

    <!-- Active filters and clear button -->
    <div v-if="showActiveFilters && activeFilterCount > 0" class="active-filters">
      <div class="active-filters-list">
        <span
          v-for="(filter, key) in activeFilters"
          :key="key"
          class="active-filter-badge"
        >
          {{ filter.label }}: {{ filter.selectedLabel }}
          <button
            type="button"
            class="active-filter-remove"
            @click="clearFilter(key)"
            :aria-label="`Remove ${filter.label} filter`"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18"></path>
              <path d="m6 6 12 12"></path>
            </svg>
          </button>
        </span>
      </div>

      <button
        type="button"
        class="clear-all-button"
        @click="clearAllFilters"
      >
        Clear all
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  filters: {
    type: Object,
    required: true
    // Example:
    // {
    //   team: {
    //     label: 'Team',
    //     value: '',
    //     placeholder: 'All Teams',
    //     options: [{ value: 'team1', label: 'Team 1' }, ...]
    //   }
    // }
  },
  label: {
    type: String,
    default: 'Filters'
  },
  showActiveFilters: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:filters', 'filter-change', 'clear-all'])

// Computed properties
const activeFilterCount = computed(() => {
  return Object.values(props.filters).filter(f => f.value).length
})

const activeFilters = computed(() => {
  const active = {}
  Object.entries(props.filters).forEach(([key, filter]) => {
    if (filter.value) {
      const selectedOption = filter.options.find(opt => opt.value === filter.value)
      active[key] = {
        label: filter.label,
        selectedLabel: selectedOption?.label || filter.value
      }
    }
  })
  return active
})

// Methods
const handleFilterChange = (key, value) => {
  const updatedFilters = {
    ...props.filters,
    [key]: {
      ...props.filters[key],
      value
    }
  }
  emit('update:filters', updatedFilters)
  emit('filter-change', { key, value })
}

const clearFilter = (key) => {
  handleFilterChange(key, '')
}

const clearAllFilters = () => {
  const clearedFilters = {}
  Object.entries(props.filters).forEach(([key, filter]) => {
    clearedFilters[key] = {
      ...filter,
      value: ''
    }
  })
  emit('update:filters', clearedFilters)
  emit('clear-all')
}

// Style classes
const selectClasses = computed(() => [
  'block w-full',
  'px-3 py-2',
  'text-sm',
  'border border-neutral-300',
  'rounded-fiori-lg',
  'bg-white',
  'text-neutral-700',
  'transition-all duration-200',
  'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500',
  'hover:border-neutral-400',
  'cursor-pointer'
])
</script>

<style scoped>
.filter-bar {
  @apply bg-white rounded-fiori-lg border border-neutral-200 p-4;
}

.filter-label {
  @apply flex items-center gap-2 mb-3 pb-3 border-b border-neutral-200;
}

.filter-controls {
  @apply grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4;
}

.filter-item {
  @apply flex flex-col gap-1.5;
}

.filter-item-label {
  @apply text-xs font-medium text-neutral-600 uppercase tracking-wide;
}

.active-filters {
  @apply flex flex-wrap items-center justify-between gap-3 mt-4 pt-4 border-t border-neutral-200;
}

.active-filters-list {
  @apply flex flex-wrap items-center gap-2;
}

.active-filter-badge {
  @apply inline-flex items-center gap-1.5;
  @apply px-3 py-1.5;
  @apply text-xs font-medium;
  @apply bg-primary-50 text-primary-700 border border-primary-200;
  @apply rounded-fiori-md;
  @apply transition-all duration-200;
}

.active-filter-remove {
  @apply inline-flex items-center justify-center;
  @apply w-4 h-4;
  @apply text-primary-600 hover:text-primary-800;
  @apply transition-colors duration-150;
  @apply rounded;
  @apply focus:outline-none focus:ring-2 focus:ring-primary-500;
}

.clear-all-button {
  @apply px-3 py-1.5;
  @apply text-xs font-medium;
  @apply text-danger-600 hover:text-danger-700;
  @apply border border-danger-300 hover:border-danger-400;
  @apply bg-white hover:bg-danger-50;
  @apply rounded-fiori-md;
  @apply transition-all duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-danger-500 focus:ring-offset-1;
}
</style>
