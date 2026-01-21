import { ref, computed } from 'vue'

/**
 * Composable for wrapping API calls with loading and error states
 *
 * @param {Function} apiFunction - The API function to call
 * @param {Object} options - Configuration options
 * @returns {Object} - API call utilities
 */
export function useApiCall(apiFunction, options = {}) {
  const {
    immediate = false,
    initialData = null,
    onSuccess = null,
    onError = null,
    resetOnExecute = true
  } = options

  const data = ref(initialData)
  const error = ref(null)
  const loading = ref(false)
  const executed = ref(false)

  /**
   * Execute the API call
   */
  const execute = async (...args) => {
    loading.value = true
    error.value = null

    if (resetOnExecute) {
      data.value = initialData
    }

    try {
      const result = await apiFunction(...args)
      data.value = result
      executed.value = true

      if (onSuccess && typeof onSuccess === 'function') {
        onSuccess(result)
      }

      return result
    } catch (err) {
      error.value = err
      executed.value = true

      if (onError && typeof onError === 'function') {
        onError(err)
      }

      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Reset the state
   */
  const reset = () => {
    data.value = initialData
    error.value = null
    loading.value = false
    executed.value = false
  }

  /**
   * Retry the API call with the last arguments
   */
  let lastArgs = []
  const retry = () => {
    return execute(...lastArgs)
  }

  // Override execute to save arguments
  const executeWithRetry = (...args) => {
    lastArgs = args
    return execute(...args)
  }

  // Computed properties
  const isReady = computed(() => !loading.value && executed.value && !error.value)
  const isError = computed(() => !!error.value)
  const isSuccess = computed(() => executed.value && !error.value)

  // Execute immediately if requested
  if (immediate) {
    execute()
  }

  return {
    // State
    data,
    error,
    loading,
    executed,

    // Computed
    isReady,
    isError,
    isSuccess,

    // Methods
    execute: executeWithRetry,
    reset,
    retry
  }
}

/**
 * Composable for managing multiple API calls
 * Useful for parallel API calls or coordinating multiple endpoints
 */
export function useMultipleApiCalls(apiConfigs) {
  const results = {}
  const allLoading = ref(false)
  const hasErrors = ref(false)

  // Create API call composables for each config
  Object.entries(apiConfigs).forEach(([key, config]) => {
    results[key] = useApiCall(config.apiFunction, {
      ...config.options,
      immediate: false
    })
  })

  /**
   * Execute all API calls
   */
  const executeAll = async (sequential = false) => {
    allLoading.value = true
    hasErrors.value = false

    try {
      if (sequential) {
        // Execute sequentially
        for (const key of Object.keys(results)) {
          await results[key].execute()
        }
      } else {
        // Execute in parallel
        await Promise.all(
          Object.values(results).map(result => result.execute())
        )
      }
    } catch (err) {
      hasErrors.value = true
      throw err
    } finally {
      allLoading.value = false
    }
  }

  /**
   * Reset all API calls
   */
  const resetAll = () => {
    Object.values(results).forEach(result => result.reset())
    allLoading.value = false
    hasErrors.value = false
  }

  /**
   * Retry all failed API calls
   */
  const retryFailed = async () => {
    const failedCalls = Object.values(results).filter(result => result.isError.value)

    if (failedCalls.length === 0) return

    allLoading.value = true
    hasErrors.value = false

    try {
      await Promise.all(
        failedCalls.map(result => result.retry())
      )
    } catch (err) {
      hasErrors.value = true
      throw err
    } finally {
      allLoading.value = false
    }
  }

  // Computed
  const allReady = computed(() => {
    return Object.values(results).every(result => result.isReady.value)
  })

  const allSuccess = computed(() => {
    return Object.values(results).every(result => result.isSuccess.value)
  })

  return {
    results,
    allLoading,
    hasErrors,
    allReady,
    allSuccess,
    executeAll,
    resetAll,
    retryFailed
  }
}
