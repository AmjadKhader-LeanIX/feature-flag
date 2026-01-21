<template>
  <div class="fiori-table-wrapper">
    <!-- Table container -->
    <div :class="tableContainerClasses">
      <table :class="tableClasses">
        <!-- Table head -->
        <thead>
          <tr>
            <th
              v-for="(column, index) in columns"
              :key="index"
              :class="getHeaderClasses(column)"
              :style="{ width: column.width || 'auto' }"
              @click="column.sortable ? handleSort(column.key) : null"
            >
              <div class="th-content">
                <span>{{ column.label }}</span>
                <span v-if="column.sortable" class="sort-icon">
                  <svg
                    v-if="sortKey === column.key && sortOrder === 'asc'"
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="m18 15-6-6-6 6"></path>
                  </svg>
                  <svg
                    v-else-if="sortKey === column.key && sortOrder === 'desc'"
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="m6 9 6 6 6-6"></path>
                  </svg>
                  <svg
                    v-else
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    opacity="0.3"
                  >
                    <path d="m7 15 5 5 5-5"></path>
                    <path d="m7 9 5-5 5 5"></path>
                  </svg>
                </span>
              </div>
            </th>
          </tr>
        </thead>

        <!-- Table body -->
        <tbody>
          <!-- Loading skeleton -->
          <tr v-if="loading">
            <td :colspan="columns.length">
              <div class="loading-container">
                <div class="spinner"></div>
                <p class="text-sm text-neutral-600 mt-2">Loading data...</p>
              </div>
            </td>
          </tr>

          <!-- Empty state -->
          <tr v-else-if="!loading && sortedData.length === 0">
            <td :colspan="columns.length">
              <div class="empty-state-table">
                <slot name="empty">
                  <div class="text-center py-8">
                    <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mx-auto text-neutral-400 mb-3">
                      <rect width="7" height="9" x="3" y="3" rx="1"></rect>
                      <rect width="7" height="5" x="14" y="3" rx="1"></rect>
                      <rect width="7" height="9" x="14" y="12" rx="1"></rect>
                      <rect width="7" height="5" x="3" y="16" rx="1"></rect>
                    </svg>
                    <p class="text-neutral-600 font-medium">{{ emptyMessage }}</p>
                  </div>
                </slot>
              </div>
            </td>
          </tr>

          <!-- Data rows -->
          <tr
            v-else
            v-for="(row, rowIndex) in sortedData"
            :key="rowIndex"
            :class="getRowClasses(row, rowIndex)"
            @click="handleRowClick(row, rowIndex)"
          >
            <td
              v-for="(column, colIndex) in columns"
              :key="colIndex"
              :class="getCellClasses(column)"
            >
              <!-- Slot for custom cell rendering -->
              <slot
                :name="`cell-${column.key}`"
                :row="row"
                :value="getValue(row, column.key)"
                :index="rowIndex"
              >
                <!-- Default cell content -->
                <span>{{ formatValue(getValue(row, column.key), column) }}</span>
              </slot>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination slot -->
    <div v-if="$slots.pagination" class="table-pagination">
      <slot name="pagination"></slot>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  columns: {
    type: Array,
    required: true
    // Example: [{ key: 'name', label: 'Name', sortable: true, width: '200px' }]
  },
  data: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  hoverable: {
    type: Boolean,
    default: true
  },
  striped: {
    type: Boolean,
    default: false
  },
  bordered: {
    type: Boolean,
    default: true
  },
  compact: {
    type: Boolean,
    default: false
  },
  clickable: {
    type: Boolean,
    default: false
  },
  emptyMessage: {
    type: String,
    default: 'No data available'
  },
  sortable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['row-click', 'sort'])

// Sorting state
const sortKey = ref('')
const sortOrder = ref('asc') // 'asc' or 'desc'

// Sorted data
const sortedData = computed(() => {
  if (!sortKey.value || !props.sortable) {
    return props.data
  }

  const sorted = [...props.data].sort((a, b) => {
    const aVal = getValue(a, sortKey.value)
    const bVal = getValue(b, sortKey.value)

    if (aVal === bVal) return 0

    const comparison = aVal > bVal ? 1 : -1
    return sortOrder.value === 'asc' ? comparison : -comparison
  })

  return sorted
})

// Methods
const getValue = (row, key) => {
  // Support nested keys like 'user.name'
  return key.split('.').reduce((obj, k) => obj?.[k], row)
}

const formatValue = (value, column) => {
  if (column.formatter && typeof column.formatter === 'function') {
    return column.formatter(value)
  }
  if (value === null || value === undefined) {
    return '-'
  }
  return value
}

const handleSort = (key) => {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortOrder.value = 'asc'
  }
  emit('sort', { key: sortKey.value, order: sortOrder.value })
}

const handleRowClick = (row, index) => {
  if (props.clickable) {
    emit('row-click', { row, index })
  }
}

// Style classes
const tableContainerClasses = computed(() => [
  'table-container',
  'overflow-x-auto',
  'rounded-fiori-lg',
  props.bordered ? 'border border-neutral-200' : ''
])

const tableClasses = computed(() => [
  'fiori-table',
  'w-full',
  props.compact ? 'table-compact' : ''
])

const getHeaderClasses = (column) => [
  'table-header',
  column.sortable ? 'sortable cursor-pointer' : '',
  column.align ? `text-${column.align}` : 'text-left'
]

const getRowClasses = (row, index) => [
  'table-row',
  props.hoverable ? 'hover:bg-neutral-50' : '',
  props.striped && index % 2 === 1 ? 'bg-neutral-50/50' : '',
  props.clickable ? 'cursor-pointer' : '',
  'transition-colors duration-150'
]

const getCellClasses = (column) => [
  'table-cell',
  column.align ? `text-${column.align}` : 'text-left'
]
</script>

<style scoped>
.fiori-table-wrapper {
  @apply w-full;
}

.table-container {
  @apply bg-white shadow-fiori-sm;
}

.fiori-table {
  @apply border-collapse;
}

.table-header {
  @apply px-4 py-3;
  @apply text-sm font-semibold text-neutral-700;
  @apply bg-neutral-50;
  @apply border-b border-neutral-200;
  @apply whitespace-nowrap;
}

.table-header.sortable:hover {
  @apply bg-neutral-100;
}

.th-content {
  @apply flex items-center gap-2;
}

.sort-icon {
  @apply text-neutral-500;
}

.table-row {
  @apply border-b border-neutral-200 last:border-0;
}

.table-cell {
  @apply px-4 py-3;
  @apply text-sm text-neutral-900;
}

.table-compact .table-header,
.table-compact .table-cell {
  @apply px-3 py-2;
}

.loading-container {
  @apply flex flex-col items-center justify-center py-12;
}

.empty-state-table {
  @apply py-8;
}

.table-pagination {
  @apply mt-4;
}
</style>
