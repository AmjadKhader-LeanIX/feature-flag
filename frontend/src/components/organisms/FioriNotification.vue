<template>
  <!-- Notification container (fixed position) -->
  <Teleport to="body">
    <div class="notification-container">
      <TransitionGroup name="notification-list">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          :class="getNotificationClasses(notification)"
          role="alert"
          :aria-live="notification.type === 'error' ? 'assertive' : 'polite'"
        >
          <!-- Icon -->
          <div class="notification-icon">
            <component :is="getIcon(notification.type)" />
          </div>

          <!-- Content -->
          <div class="notification-content">
            <p v-if="notification.title" class="notification-title">
              {{ notification.title }}
            </p>
            <p class="notification-message">
              {{ notification.message }}
            </p>
          </div>

          <!-- Close button -->
          <button
            v-if="notification.closable !== false"
            type="button"
            class="notification-close"
            @click="close(notification.id)"
            aria-label="Close notification"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18"></path>
              <path d="m6 6 12 12"></path>
            </svg>
          </button>

          <!-- Progress bar (for duration) -->
          <div
            v-if="notification.duration && notification.duration > 0"
            class="notification-progress"
            :style="{ animationDuration: `${notification.duration}ms` }"
          ></div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, h } from 'vue'

// Notification queue
const notifications = ref([])

// Notification counter for unique IDs
let notificationId = 0

// Icon components
const getIcon = (type) => {
  const icons = {
    success: () => h('svg', {
      xmlns: 'http://www.w3.org/2000/svg',
      width: 20,
      height: 20,
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': 2,
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    }, [
      h('path', { d: 'M22 11.08V12a10 10 0 1 1-5.93-9.14' }),
      h('path', { d: 'm9 11 3 3L22 4' })
    ]),
    error: () => h('svg', {
      xmlns: 'http://www.w3.org/2000/svg',
      width: 20,
      height: 20,
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': 2,
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    }, [
      h('circle', { cx: 12, cy: 12, r: 10 }),
      h('path', { d: 'm15 9-6 6' }),
      h('path', { d: 'm9 9 6 6' })
    ]),
    warning: () => h('svg', {
      xmlns: 'http://www.w3.org/2000/svg',
      width: 20,
      height: 20,
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': 2,
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    }, [
      h('path', { d: 'm21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3' }),
      h('path', { d: 'M12 9v4' }),
      h('path', { d: 'M12 17h.01' })
    ]),
    info: () => h('svg', {
      xmlns: 'http://www.w3.org/2000/svg',
      width: 20,
      height: 20,
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': 2,
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    }, [
      h('circle', { cx: 12, cy: 12, r: 10 }),
      h('path', { d: 'M12 16v-4' }),
      h('path', { d: 'M12 8h.01' })
    ])
  }
  return icons[type] || icons.info
}

// Get notification classes based on type
const getNotificationClasses = (notification) => {
  const baseClasses = [
    'notification-item',
    'animate-slide-in-right'
  ]

  const typeClasses = {
    success: 'notification-success',
    error: 'notification-error',
    warning: 'notification-warning',
    info: 'notification-info'
  }

  return [...baseClasses, typeClasses[notification.type] || typeClasses.info]
}

// Show notification
const show = (options) => {
  const id = ++notificationId
  const notification = {
    id,
    message: options.message || '',
    title: options.title || '',
    type: options.type || 'info', // success, error, warning, info
    duration: options.duration !== undefined ? options.duration : 5000,
    closable: options.closable !== false
  }

  notifications.value.push(notification)

  // Auto-close after duration
  if (notification.duration > 0) {
    setTimeout(() => {
      close(id)
    }, notification.duration)
  }

  return id
}

// Close notification
const close = (id) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index !== -1) {
    notifications.value.splice(index, 1)
  }
}

// Close all notifications
const closeAll = () => {
  notifications.value = []
}

// Expose methods
defineExpose({
  show,
  close,
  closeAll
})
</script>

<style scoped>
.notification-container {
  @apply fixed top-4 right-4 z-[100];
  @apply flex flex-col gap-3;
  @apply pointer-events-none;
  max-width: 420px;
  width: calc(100% - 2rem);
}

.notification-item {
  @apply relative;
  @apply flex items-start gap-3;
  @apply p-4;
  @apply bg-white;
  @apply rounded-fiori-lg;
  @apply shadow-fiori-xl;
  @apply border-l-4;
  @apply pointer-events-auto;
  @apply overflow-hidden;
}

.notification-success {
  @apply border-success-500;
}

.notification-success .notification-icon {
  @apply text-success-600;
}

.notification-error {
  @apply border-danger-500;
}

.notification-error .notification-icon {
  @apply text-danger-600;
}

.notification-warning {
  @apply border-warning-500;
}

.notification-warning .notification-icon {
  @apply text-warning-600;
}

.notification-info {
  @apply border-info-500;
}

.notification-info .notification-icon {
  @apply text-info-600;
}

.notification-icon {
  @apply flex-shrink-0 mt-0.5;
}

.notification-content {
  @apply flex-1 min-w-0;
}

.notification-title {
  @apply text-sm font-semibold text-neutral-900 mb-1;
}

.notification-message {
  @apply text-sm text-neutral-700;
  @apply break-words;
}

.notification-close {
  @apply flex-shrink-0;
  @apply p-1;
  @apply rounded-fiori-md;
  @apply text-neutral-400 hover:text-neutral-600 hover:bg-neutral-100;
  @apply transition-colors duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-1;
}

.notification-progress {
  @apply absolute bottom-0 left-0;
  @apply h-1;
  @apply bg-current;
  @apply opacity-30;
  animation: progress-shrink linear forwards;
}

@keyframes progress-shrink {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
}

/* Transition animations */
.notification-list-move,
.notification-list-enter-active {
  @apply transition-all duration-300;
}

.notification-list-leave-active {
  @apply transition-all duration-200;
}

.notification-list-enter-from {
  @apply opacity-0 translate-x-full;
}

.notification-list-leave-to {
  @apply opacity-0 scale-95;
}

.notification-list-leave-active {
  @apply absolute;
}

/* Responsive */
@media (max-width: 640px) {
  .notification-container {
    @apply top-2 right-2 left-2;
    max-width: none;
    width: auto;
  }
}
</style>
