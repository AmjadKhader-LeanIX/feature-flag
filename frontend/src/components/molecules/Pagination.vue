<template>
  <div class="pagination-wrapper">
    <!-- Page info -->
    <div class="pagination-info">
      <span class="text-sm text-neutral-600">
        Showing <span class="font-medium text-neutral-900">{{ startItem }}</span> to
        <span class="font-medium text-neutral-900">{{ endItem }}</span> of
        <span class="font-medium text-neutral-900">{{ total }}</span> items
      </span>
    </div>

    <!-- Pagination controls -->
    <div class="pagination-controls">
      <!-- First page button -->
      <button
        v-if="showFirstLast"
        type="button"
        :disabled="isFirstPage"
        :class="buttonClasses"
        @click="goToPage(1)"
        aria-label="First page"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m11 17-5-5 5-5"></path>
          <path d="m18 17-5-5 5-5"></path>
        </svg>
      </button>

      <!-- Previous button -->
      <button
        type="button"
        :disabled="isFirstPage"
        :class="buttonClasses"
        @click="goToPrevious"
        aria-label="Previous page"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"></path>
        </svg>
      </button>

      <!-- Page numbers -->
      <div class="pagination-pages">
        <button
          v-for="page in visiblePages"
          :key="page"
          type="button"
          :class="pageButtonClasses(page)"
          @click="goToPage(page)"
          :aria-label="`Go to page ${page}`"
          :aria-current="page === current ? 'page' : undefined"
        >
          {{ page }}
        </button>
      </div>

      <!-- Next button -->
      <button
        type="button"
        :disabled="isLastPage"
        :class="buttonClasses"
        @click="goToNext"
        aria-label="Next page"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m9 18 6-6-6-6"></path>
        </svg>
      </button>

      <!-- Last page button -->
      <button
        v-if="showFirstLast"
        type="button"
        :disabled="isLastPage"
        :class="buttonClasses"
        @click="goToPage(totalPages)"
        aria-label="Last page"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m6 17 5-5-5-5"></path>
          <path d="m13 17 5-5-5-5"></path>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  current: {
    type: Number,
    required: true,
    validator: (value) => value >= 1
  },
  total: {
    type: Number,
    required: true,
    validator: (value) => value >= 0
  },
  perPage: {
    type: Number,
    default: 10,
    validator: (value) => value > 0
  },
  maxVisible: {
    type: Number,
    default: 7,
    validator: (value) => value >= 5
  },
  showFirstLast: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['page-change'])

// Computed properties
const totalPages = computed(() => Math.ceil(props.total / props.perPage))

const startItem = computed(() => {
  if (props.total === 0) return 0
  return (props.current - 1) * props.perPage + 1
})

const endItem = computed(() => {
  const end = props.current * props.perPage
  return end > props.total ? props.total : end
})

const isFirstPage = computed(() => props.current === 1)

const isLastPage = computed(() => props.current >= totalPages.value)

// Calculate visible page numbers
const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = props.current
  const maxVisible = props.maxVisible

  if (total <= maxVisible) {
    // Show all pages
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // Show subset with current page in center
    const half = Math.floor(maxVisible / 2)
    let start = current - half
    let end = current + half

    // Adjust if at start
    if (start < 1) {
      start = 1
      end = maxVisible
    }

    // Adjust if at end
    if (end > total) {
      end = total
      start = total - maxVisible + 1
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
  }

  return pages
})

// Methods
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== props.current) {
    emit('page-change', page)
  }
}

const goToPrevious = () => {
  if (!isFirstPage.value) {
    goToPage(props.current - 1)
  }
}

const goToNext = () => {
  if (!isLastPage.value) {
    goToPage(props.current + 1)
  }
}

// Style classes
const buttonClasses = computed(() => [
  'inline-flex items-center justify-center',
  'w-10 h-10',
  'rounded-fiori-lg',
  'border border-neutral-300',
  'bg-white',
  'text-neutral-700',
  'transition-all duration-200',
  'hover:bg-neutral-50 hover:border-neutral-400',
  'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:border-neutral-300',
  'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-1'
])

const pageButtonClasses = (page) => [
  'inline-flex items-center justify-center',
  'min-w-10 h-10 px-3',
  'rounded-fiori-lg',
  'border',
  'text-sm font-medium',
  'transition-all duration-200',
  'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-1',
  page === props.current
    ? 'bg-primary-600 text-white border-primary-600 shadow-fiori-md cursor-default'
    : 'bg-white text-neutral-700 border-neutral-300 hover:bg-neutral-50 hover:border-neutral-400 cursor-pointer'
]
</script>

<style scoped>
.pagination-wrapper {
  @apply flex flex-col sm:flex-row items-center justify-between gap-4;
}

.pagination-info {
  @apply order-2 sm:order-1;
}

.pagination-controls {
  @apply flex items-center gap-2 order-1 sm:order-2;
}

.pagination-pages {
  @apply flex items-center gap-1;
}
</style>
