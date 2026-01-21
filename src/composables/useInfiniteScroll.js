import { ref, onMounted, onUnmounted } from 'vue'

/**
 * Composable for infinite scroll functionality
 * Detects when user scrolls near the bottom and triggers load more
 *
 * @param {Function} loadMore - Callback function to load more data
 * @param {Object} options - Configuration options
 * @param {number} options.threshold - Scroll threshold (0-1), default 0.8 (80%)
 * @param {string} options.target - Target element selector, default window
 * @param {boolean} options.enabled - Whether infinite scroll is enabled
 * @returns {Object} - Infinite scroll utilities
 */
export function useInfiniteScroll(loadMore, options = {}) {
  const {
    threshold = 0.8,
    target = null,
    enabled = true
  } = options

  const isLoading = ref(false)
  const hasMore = ref(true)
  let targetElement = null

  const handleScroll = (event) => {
    if (!enabled || !hasMore.value || isLoading.value) {
      return
    }

    let scrollTop, scrollHeight, clientHeight

    if (targetElement) {
      // Scrolling within a specific element
      scrollTop = targetElement.scrollTop
      scrollHeight = targetElement.scrollHeight
      clientHeight = targetElement.clientHeight
    } else {
      // Scrolling the window
      scrollTop = window.pageYOffset || document.documentElement.scrollTop
      scrollHeight = document.documentElement.scrollHeight
      clientHeight = window.innerHeight
    }

    const scrollPosition = scrollTop + clientHeight
    const triggerPoint = scrollHeight * threshold

    if (scrollPosition >= triggerPoint) {
      isLoading.value = true

      // Call load more function
      Promise.resolve(loadMore()).finally(() => {
        isLoading.value = false
      })
    }
  }

  const setupScrollListener = () => {
    if (target) {
      targetElement = typeof target === 'string'
        ? document.querySelector(target)
        : target

      if (targetElement) {
        targetElement.addEventListener('scroll', handleScroll)
      }
    } else {
      window.addEventListener('scroll', handleScroll)
    }
  }

  const removeScrollListener = () => {
    if (targetElement) {
      targetElement.removeEventListener('scroll', handleScroll)
    } else {
      window.removeEventListener('scroll', handleScroll)
    }
  }

  const reset = () => {
    hasMore.value = true
    isLoading.value = false
  }

  const setHasMore = (value) => {
    hasMore.value = value
  }

  onMounted(() => {
    setupScrollListener()
  })

  onUnmounted(() => {
    removeScrollListener()
  })

  return {
    isLoading,
    hasMore,
    handleScroll,
    reset,
    setHasMore
  }
}
