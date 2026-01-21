import { createApp } from 'vue'
import App from './App.vue'
import './assets/styles/tailwind.css'

// Create Vue app
const app = createApp(App)

// Mount app
app.mount('#app')

// Hide loading screen after app is mounted
window.addEventListener('load', () => {
  setTimeout(() => {
    const loadingScreen = document.getElementById('loading-screen')
    if (loadingScreen) {
      loadingScreen.classList.add('opacity-0', 'pointer-events-none')
      setTimeout(() => {
        loadingScreen.style.display = 'none'
      }, 300)
    }
  }, 500)
})
