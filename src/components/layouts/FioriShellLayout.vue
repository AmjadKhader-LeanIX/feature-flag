<template>
  <div class="fiori-shell-layout">
    <!-- Top Navigation Bar -->
    <nav class="fiori-shell-bar">
      <div class="shell-bar-content">
        <!-- Logo and Brand -->
        <div class="shell-bar-brand">
          <img
            src="https://www.leanix.net/hubfs/2024-Website/branding/logo/favicon/256x256.svg"
            alt="LeanIX Logo"
            class="brand-logo"
          />
          <span class="brand-name">{{ appTitle }}</span>
        </div>

        <!-- Navigation Items -->
        <div class="shell-bar-nav">
          <button
            v-for="item in navItems"
            :key="item.id"
            :class="getNavItemClasses(item)"
            @click="handleNavClick(item)"
          >
            <component v-if="item.icon" :is="item.icon" :size="20" />
            <span>{{ item.label }}</span>
          </button>
        </div>

        <!-- Actions (right side) -->
        <div class="shell-bar-actions">
          <slot name="actions"></slot>
        </div>
      </div>
    </nav>

    <!-- Main Content Area -->
    <main class="fiori-main-content">
      <slot></slot>
    </main>

    <!-- Notification Area -->
    <FioriNotification ref="notificationRef" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import FioriNotification from '../organisms/FioriNotification.vue'

const props = defineProps({
  appTitle: {
    type: String,
    default: 'Feature Flag Manager'
  },
  navItems: {
    type: Array,
    default: () => []
    // Example: [{ id: 'dashboard', label: 'Dashboard', icon: HomeIcon, active: true }]
  },
  activeNav: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['nav-click'])

const notificationRef = ref(null)

// Methods
const handleNavClick = (item) => {
  emit('nav-click', item)
}

const getNavItemClasses = (item) => [
  'nav-item',
  item.id === props.activeNav || item.active ? 'active' : ''
]

// Expose notification methods
const showNotification = (options) => {
  return notificationRef.value?.show(options)
}

const closeNotification = (id) => {
  notificationRef.value?.close(id)
}

const closeAllNotifications = () => {
  notificationRef.value?.closeAll()
}

defineExpose({
  showNotification,
  closeNotification,
  closeAllNotifications
})
</script>

<style scoped>
.fiori-shell-layout {
  @apply min-h-screen bg-neutral-50;
  @apply flex flex-col;
}

.fiori-shell-bar {
  @apply fixed top-0 left-0 right-0 z-40;
  @apply h-16;
  @apply bg-gradient-to-r from-primary-700 to-primary-600;
  @apply shadow-fiori-md;
}

.shell-bar-content {
  @apply h-full;
  @apply flex items-center justify-between;
  @apply px-6;
  @apply gap-6;
}

.shell-bar-brand {
  @apply flex items-center gap-3;
  @apply flex-shrink-0;
}

.brand-logo {
  @apply h-10 w-10;
  @apply object-contain;
}

.brand-name {
  @apply text-xl font-semibold text-white;
  @apply hidden sm:block;
}

.shell-bar-nav {
  @apply flex items-center gap-2;
  @apply flex-1;
  @apply overflow-x-auto;
}

.nav-item {
  @apply flex items-center gap-2;
  @apply px-4 py-2;
  @apply text-sm font-medium;
  @apply text-white/80;
  @apply rounded-fiori-lg;
  @apply transition-all duration-200;
  @apply whitespace-nowrap;
  @apply hover:bg-white/10 hover:text-white;
  @apply focus:outline-none focus:ring-2 focus:ring-white/30 focus:ring-offset-2 focus:ring-offset-primary-700;
}

.nav-item.active {
  @apply bg-white/20 text-white;
  @apply shadow-fiori-sm;
}

.shell-bar-actions {
  @apply flex items-center gap-3;
  @apply flex-shrink-0;
}

.fiori-main-content {
  @apply flex-1;
  @apply mt-16;
}

/* Responsive */
@media (max-width: 640px) {
  .shell-bar-content {
    @apply px-4;
  }

  .nav-item span {
    @apply hidden;
  }

  .nav-item {
    @apply px-3;
  }
}
</style>
