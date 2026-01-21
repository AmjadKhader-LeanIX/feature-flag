import { ref, watch } from 'vue'

/**
 * Composable for debounced search
 * Delays the execution of search function until user stops typing
 *
 * @param {Ref} searchValue - Reactive search value ref
 * @param {Function} searchFunction - Function to execute after debounce
 * @param {number} delay - Debounce delay in milliseconds (default: 300)
 * @returns {Object} - Debounced search utilities
 */
export function useDebouncedSearch(searchValue, searchFunction, delay = 300) {
  const debouncedValue = ref(searchValue.value)
  const isDebouncing = ref(false)
  let timeoutId = null

  // Watch the search value and debounce it
  watch(searchValue, (newValue) => {
    isDebouncing.value = true

    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    timeoutId = setTimeout(() => {
      debouncedValue.value = newValue
      isDebouncing.value = false

      if (searchFunction && typeof searchFunction === 'function') {
        searchFunction(newValue)
      }
    }, delay)
  })

  const cancel = () => {
    if (timeoutId) {
      clearTimeout(timeoutId)
      isDebouncing.value = false
    }
  }

  const flush = () => {
    cancel()
    debouncedValue.value = searchValue.value
    if (searchFunction && typeof searchFunction === 'function') {
      searchFunction(searchValue.value)
    }
  }

  return {
    debouncedValue,
    isDebouncing,
    cancel,
    flush
  }
}

/**
 * Alternative composable that returns only the debounced value
 *
 * @param {Ref} value - Reactive value ref
 * @param {number} delay - Debounce delay in milliseconds (default: 300)
 * @returns {Ref} - Debounced value ref
 */
export function useDebounce(value, delay = 300) {
  const debouncedValue = ref(value.value)
  let timeoutId = null

  watch(value, (newValue) => {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    timeoutId = setTimeout(() => {
      debouncedValue.value = newValue
    }, delay)
  })

  return debouncedValue
}
