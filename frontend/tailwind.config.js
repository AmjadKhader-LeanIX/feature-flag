/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // LeanIX Navy Blue + SAP Fiori Horizon Color System
      colors: {
        primary: {
          50: '#f3f6fd',
          100: '#e6ecfb',
          200: '#c0d4f6',
          300: '#99bcf1',
          400: '#4d8ce7',
          500: '#4a7bd1',
          600: '#0059c9',
          700: '#002a86', // LeanIX Navy Blue (main)
          800: '#001f61',
          900: '#001232',
        },
        success: {
          50: '#f0fdf4',
          100: '#dcfce7',
          500: '#10b981',
          600: '#059669',
          700: '#047857',
        },
        warning: {
          50: '#fffbeb',
          100: '#fef3c7',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
        },
        danger: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#ef4444',
          600: '#dc2626',
          700: '#b91c1c',
        },
        info: {
          50: '#eff6ff',
          100: '#dbeafe',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
        },
        neutral: {
          50: '#f9fafb',
          100: '#f3f4f6',
          200: '#e5e7eb',
          300: '#d1d5db',
          400: '#9ca3af',
          500: '#6b7280',
          600: '#4b5563',
          700: '#374151',
          800: '#1f2937',
          900: '#111827',
        }
      },

      // SAP Fiori Horizon Border Radius (Generous style)
      borderRadius: {
        'fiori-sm': '6px',
        'fiori-md': '8px',
        'fiori-lg': '12px',
        'fiori-xl': '16px',
        'fiori-2xl': '24px',
        'fiori-3xl': '32px',
      },

      // SAP Fiori Horizon Box Shadows (Soft, subtle)
      boxShadow: {
        'fiori-xs': '0 1px 2px 0 rgba(0, 42, 134, 0.05)',
        'fiori-sm': '0 1px 3px 0 rgba(0, 42, 134, 0.1), 0 1px 2px -1px rgba(0, 42, 134, 0.1)',
        'fiori-md': '0 4px 6px -1px rgba(0, 42, 134, 0.1), 0 2px 4px -2px rgba(0, 42, 134, 0.1)',
        'fiori-lg': '0 10px 15px -3px rgba(0, 42, 134, 0.12), 0 4px 6px -4px rgba(0, 42, 134, 0.1)',
        'fiori-xl': '0 20px 25px -5px rgba(0, 42, 134, 0.15), 0 8px 10px -6px rgba(0, 42, 134, 0.1)',
        'fiori-2xl': '0 25px 50px -12px rgba(0, 42, 134, 0.25)',
        'fiori-inner': 'inset 0 2px 4px 0 rgba(0, 42, 134, 0.06)',
      },

      // Enhanced spacing scale
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
      },

      // Keyframe Animations from animations.css
      keyframes: {
        // Slide Animations
        slideInFromRight: {
          from: { transform: 'translateX(100%)', opacity: '0' },
          to: { transform: 'translateX(0)', opacity: '1' }
        },
        slideInFromLeft: {
          from: { transform: 'translateX(-100%)', opacity: '0' },
          to: { transform: 'translateX(0)', opacity: '1' }
        },
        slideInFromTop: {
          from: { transform: 'translateY(-100%)', opacity: '0' },
          to: { transform: 'translateY(0)', opacity: '1' }
        },
        slideInFromBottom: {
          from: { transform: 'translateY(100%)', opacity: '0' },
          to: { transform: 'translateY(0)', opacity: '1' }
        },

        // Fade Animations
        fadeIn: {
          from: { opacity: '0' },
          to: { opacity: '1' }
        },
        fadeOut: {
          from: { opacity: '1' },
          to: { opacity: '0' }
        },
        fadeInScale: {
          from: { opacity: '0', transform: 'scale(0.9)' },
          to: { opacity: '1', transform: 'scale(1)' }
        },

        // Bounce Animations
        bounceIn: {
          '0%': { opacity: '0', transform: 'scale(0.3)' },
          '50%': { opacity: '1', transform: 'scale(1.05)' },
          '70%': { transform: 'scale(0.9)' },
          '100%': { transform: 'scale(1)' }
        },
        bounceOut: {
          '0%': { transform: 'scale(1)' },
          '25%': { transform: 'scale(0.95)' },
          '50%': { opacity: '1', transform: 'scale(1.1)' },
          '100%': { opacity: '0', transform: 'scale(0.3)' }
        },

        // Shake Animation
        shake: {
          '0%, 100%': { transform: 'translateX(0)' },
          '10%, 30%, 50%, 70%, 90%': { transform: 'translateX(-10px)' },
          '20%, 40%, 60%, 80%': { transform: 'translateX(10px)' }
        },

        // Pulse Animations
        pulse: {
          '0%, 100%': { opacity: '1', transform: 'scale(1)' },
          '50%': { opacity: '0.8', transform: 'scale(1.05)' }
        },
        pulseGlow: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(0, 42, 134, 0.4)' },
          '50%': { boxShadow: '0 0 20px 10px rgba(0, 42, 134, 0)' }
        },

        // Shimmer Effect
        shimmer: {
          '0%': { backgroundPosition: '-1000px 0' },
          '100%': { backgroundPosition: '1000px 0' }
        },
        shimmerWave: {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(100%)' }
        },

        // Rotate Animations
        rotate: {
          from: { transform: 'rotate(0deg)' },
          to: { transform: 'rotate(360deg)' }
        },
        rotateIn: {
          from: { opacity: '0', transform: 'rotate(-200deg) scale(0)' },
          to: { opacity: '1', transform: 'rotate(0deg) scale(1)' }
        },

        // Flip Animations
        flipInX: {
          from: { transform: 'perspective(400px) rotateX(90deg)', opacity: '0' },
          to: { transform: 'perspective(400px) rotateX(0deg)', opacity: '1' }
        },
        flipInY: {
          from: { transform: 'perspective(400px) rotateY(90deg)', opacity: '0' },
          to: { transform: 'perspective(400px) rotateY(0deg)', opacity: '1' }
        },

        // Zoom Animations
        zoomIn: {
          from: { opacity: '0', transform: 'scale(0)' },
          to: { opacity: '1', transform: 'scale(1)' }
        },
        zoomOut: {
          from: { opacity: '1', transform: 'scale(1)' },
          to: { opacity: '0', transform: 'scale(0)' }
        },

        // Progress Bar Animation
        progressBar: {
          '0%': { width: '0%' },
          '100%': { width: '100%' }
        },

        // Skeleton Loader Animation
        skeletonLoading: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' }
        },

        // Ripple Effect
        ripple: {
          '0%': { transform: 'scale(0)', opacity: '1' },
          '100%': { transform: 'scale(4)', opacity: '0' }
        },

        // Typing Indicator
        typingDot: {
          '0%, 60%, 100%': { transform: 'translateY(0)', opacity: '0.7' },
          '30%': { transform: 'translateY(-10px)', opacity: '1' }
        },

        // Heartbeat Animation
        heartbeat: {
          '0%, 100%': { transform: 'scale(1)' },
          '10%, 30%': { transform: 'scale(0.9)' },
          '20%, 40%': { transform: 'scale(1.1)' }
        },

        // Wiggle Animation
        wiggle: {
          '0%, 100%': { transform: 'rotate(0deg)' },
          '25%': { transform: 'rotate(-10deg)' },
          '75%': { transform: 'rotate(10deg)' }
        },

        // Glow Animation
        glow: {
          '0%, 100%': { boxShadow: '0 0 5px rgba(0, 42, 134, 0.5)' },
          '50%': { boxShadow: '0 0 20px rgba(0, 42, 134, 0.8), 0 0 30px rgba(0, 42, 134, 0.6)' }
        },
      },

      // Animation Utility Classes
      animation: {
        // Fade
        'fade-in': 'fadeIn 0.3s ease-out',
        'fade-out': 'fadeOut 0.3s ease-out',
        'fade-in-scale': 'fadeInScale 0.4s ease-out',

        // Slide
        'slide-in-right': 'slideInFromRight 0.5s ease-out',
        'slide-in-left': 'slideInFromLeft 0.5s ease-out',
        'slide-in-top': 'slideInFromTop 0.5s ease-out',
        'slide-in-bottom': 'slideInFromBottom 0.5s ease-out',

        // Bounce
        'bounce-in': 'bounceIn 0.5s ease-out',
        'bounce-out': 'bounceOut 0.5s ease-out',

        // Zoom
        'zoom-in': 'zoomIn 0.3s ease-out',
        'zoom-out': 'zoomOut 0.3s ease-out',

        // Flip
        'flip-in-x': 'flipInX 0.5s ease-out',
        'flip-in-y': 'flipInY 0.5s ease-out',

        // Rotate
        'rotate-in': 'rotateIn 0.5s ease-out',
        'spin': 'rotate 1s linear infinite',
        'spin-slow': 'rotate 3s linear infinite',
        'spin-fast': 'rotate 0.5s linear infinite',

        // Pulse
        'pulse': 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'pulse-glow': 'pulseGlow 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',

        // Others
        'shake': 'shake 0.5s',
        'wiggle': 'wiggle 0.5s ease-in-out',
        'heartbeat': 'heartbeat 1.5s ease-in-out infinite',
        'glow': 'glow 2s ease-in-out infinite',
        'shimmer': 'shimmer 2s linear infinite',
        'shimmer-wave': 'shimmerWave 2s infinite',
        'skeleton': 'skeletonLoading 1.5s ease-in-out infinite',
        'ripple': 'ripple 0.6s ease-out',
        'typing-dot': 'typingDot 1.4s infinite',
        'progress': 'progressBar 1s ease-out',
      },

      // Typography
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },

      // Transition durations
      transitionDuration: {
        '400': '400ms',
      },

      // Z-index scale
      zIndex: {
        '60': '60',
        '70': '70',
        '80': '80',
        '90': '90',
        '100': '100',
      },
    },
  },
  plugins: [
    // Custom plugin for stagger animations
    function({ addUtilities }) {
      const staggerUtilities = {
        '.stagger-fade-in > *': {
          opacity: '0',
          animation: 'fadeIn 0.5s ease-out forwards'
        },
        '.stagger-slide-in > *': {
          opacity: '0',
          animation: 'slideInFromLeft 0.5s ease-out forwards'
        }
      }

      // Generate stagger delays for children
      for (let i = 1; i <= 10; i++) {
        staggerUtilities[`.stagger-fade-in > *:nth-child(${i})`] = {
          'animation-delay': `${i * 0.05}s`
        }
        staggerUtilities[`.stagger-slide-in > *:nth-child(${i})`] = {
          'animation-delay': `${i * 0.05}s`
        }
      }

      addUtilities(staggerUtilities)
    }
  ],
}
