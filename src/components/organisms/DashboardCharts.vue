<template>
  <div class="space-y-6">
    <!-- Top Row: Rollout and Team Activity -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Rollout Distribution Chart -->
      <FioriCard class="p-6">
        <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
          <PieChart :size="20" class="text-primary-600" />
          Rollout Distribution
        </h3>
        <div ref="rolloutChartRef" class="h-64"></div>
      </FioriCard>

      <!-- Team Activity Chart -->
      <FioriCard class="p-6">
        <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
          <BarChart3 :size="20" class="text-primary-600" />
          Team Activity
        </h3>
        <div ref="teamChartRef" class="h-64"></div>
      </FioriCard>
    </div>

    <!-- Bottom Row: Activity Timeline -->
    <FioriCard class="p-6">
      <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
        <Activity :size="20" class="text-primary-600" />
        Activity Timeline (Last 30 Days)
      </h3>
      <div ref="activityChartRef" class="h-64"></div>
    </FioriCard>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import FioriCard from '../atoms/FioriCard.vue'
import { PieChart, BarChart3, Activity } from 'lucide-vue-next'
import VueApexCharts from 'vue3-apexcharts'

const props = defineProps({
  featureFlags: {
    type: Array,
    default: () => []
  },
  auditLogs: {
    type: Array,
    default: () => []
  }
})

const rolloutChartRef = ref(null)
const teamChartRef = ref(null)
const activityChartRef = ref(null)

let rolloutChart = null
let teamChart = null
let activityChart = null

// Compute rollout distribution data
const rolloutDistribution = computed(() => {
  const buckets = {
    'Disabled (0%)': 0,
    'Testing (1-25%)': 0,
    'Partial (26-75%)': 0,
    'Advanced (76-99%)': 0,
    'Full (100%)': 0
  }

  props.featureFlags.forEach(flag => {
    const rollout = flag.rolloutPercentage || 0
    if (rollout === 0) buckets['Disabled (0%)']++
    else if (rollout <= 25) buckets['Testing (1-25%)']++
    else if (rollout <= 75) buckets['Partial (26-75%)']++
    else if (rollout < 100) buckets['Advanced (76-99%)']++
    else buckets['Full (100%)']++
  })

  return buckets
})

// Compute team performance data
const teamPerformance = computed(() => {
  const teamData = {}

  props.featureFlags.forEach(flag => {
    if (!teamData[flag.team]) {
      teamData[flag.team] = {
        total: 0,
        active: 0
      }
    }
    teamData[flag.team].total++
    if (flag.rolloutPercentage > 0) {
      teamData[flag.team].active++
    }
  })

  return teamData
})

// Compute activity timeline data (last 30 days)
const activityTimeline = computed(() => {
  const days = 30
  const today = new Date()
  const timeline = {}

  // Initialize last 30 days
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    timeline[dateStr] = { CREATE: 0, UPDATE: 0, DELETE: 0 }
  }

  // Count operations per day
  props.auditLogs.forEach(log => {
    const logDate = new Date(log.createdAt || log.timestamp).toISOString().split('T')[0]
    if (timeline[logDate]) {
      timeline[logDate][log.operation] = (timeline[logDate][log.operation] || 0) + 1
    }
  })

  return timeline
})

// Initialize Rollout Distribution Chart (Donut)
const initRolloutChart = async () => {
  if (!rolloutChartRef.value) return

  await nextTick()

  const distribution = rolloutDistribution.value
  const labels = Object.keys(distribution)
  const data = Object.values(distribution)

  const options = {
    chart: {
      type: 'donut',
      height: 256,
      fontFamily: 'Inter, sans-serif',
      animations: {
        enabled: true,
        speed: 800,
        animateGradually: {
          enabled: true,
          delay: 150
        }
      }
    },
    series: data,
    labels: labels,
    colors: ['#9ca3af', '#fbbf24', '#3b82f6', '#10b981', '#002a86'],
    legend: {
      position: 'bottom',
      fontSize: '12px',
      fontWeight: 500
    },
    dataLabels: {
      enabled: true,
      formatter: function (val, opts) {
        const value = opts.w.config.series[opts.seriesIndex]
        return value > 0 ? value : ''
      }
    },
    plotOptions: {
      pie: {
        donut: {
          size: '65%',
          labels: {
            show: true,
            name: {
              show: true,
              fontSize: '14px',
              fontWeight: 600
            },
            value: {
              show: true,
              fontSize: '24px',
              fontWeight: 700,
              color: '#002a86'
            },
            total: {
              show: true,
              label: 'Total Flags',
              fontSize: '14px',
              fontWeight: 600,
              color: '#6b7280',
              formatter: function (w) {
                return w.globals.seriesTotals.reduce((a, b) => a + b, 0)
              }
            }
          }
        }
      }
    },
    responsive: [{
      breakpoint: 480,
      options: {
        legend: {
          position: 'bottom',
          fontSize: '10px'
        }
      }
    }]
  }

  // Use ApexCharts directly
  if (window.ApexCharts) {
    rolloutChart = new window.ApexCharts(rolloutChartRef.value, options)
    rolloutChart.render()
  }
}

// Initialize Team Activity Chart (Bar)
const initTeamChart = async () => {
  if (!teamChartRef.value) return

  await nextTick()

  const teamData = teamPerformance.value
  const teams = Object.keys(teamData).sort()
  const totalData = teams.map(team => teamData[team].total)
  const activeData = teams.map(team => teamData[team].active)

  const options = {
    chart: {
      type: 'bar',
      height: 256,
      fontFamily: 'Inter, sans-serif',
      toolbar: {
        show: false
      }
    },
    series: [
      {
        name: 'Total Flags',
        data: totalData
      },
      {
        name: 'Active Flags',
        data: activeData
      }
    ],
    xaxis: {
      categories: teams,
      labels: {
        style: {
          fontSize: '12px'
        }
      }
    },
    yaxis: {
      title: {
        text: 'Number of Flags',
        style: {
          fontSize: '12px',
          fontWeight: 500
        }
      },
      labels: {
        style: {
          fontSize: '12px'
        }
      }
    },
    colors: ['#3b82f6', '#002a86'],
    plotOptions: {
      bar: {
        horizontal: false,
        columnWidth: '60%',
        borderRadius: 4
      }
    },
    dataLabels: {
      enabled: false
    },
    legend: {
      position: 'top',
      fontSize: '12px',
      fontWeight: 500,
      horizontalAlign: 'right'
    },
    grid: {
      borderColor: '#e5e7eb',
      strokeDashArray: 4
    }
  }

  // Use ApexCharts directly
  if (window.ApexCharts) {
    teamChart = new window.ApexCharts(teamChartRef.value, options)
    teamChart.render()
  }
}

// Initialize Activity Timeline Chart (Area)
const initActivityChart = async () => {
  if (!activityChartRef.value) return

  await nextTick()

  const timeline = activityTimeline.value
  const dates = Object.keys(timeline).sort()
  const createData = dates.map(date => timeline[date].CREATE)
  const updateData = dates.map(date => timeline[date].UPDATE)
  const deleteData = dates.map(date => timeline[date].DELETE)

  const options = {
    chart: {
      type: 'area',
      height: 256,
      fontFamily: 'Inter, sans-serif',
      toolbar: {
        show: false
      },
      zoom: {
        enabled: false
      }
    },
    series: [
      {
        name: 'Created',
        data: createData
      },
      {
        name: 'Updated',
        data: updateData
      },
      {
        name: 'Deleted',
        data: deleteData
      }
    ],
    xaxis: {
      categories: dates.map(date => {
        const d = new Date(date)
        return (d.getMonth() + 1) + '/' + d.getDate()
      }),
      labels: {
        style: {
          fontSize: '11px'
        },
        rotate: -45
      }
    },
    yaxis: {
      title: {
        text: 'Operations Count',
        style: {
          fontSize: '12px',
          fontWeight: 500
        }
      },
      labels: {
        style: {
          fontSize: '12px'
        },
        formatter: function(val) {
          return Math.floor(val)
        }
      }
    },
    colors: ['#10b981', '#3b82f6', '#ef4444'],
    dataLabels: {
      enabled: false
    },
    stroke: {
      curve: 'smooth',
      width: 2
    },
    fill: {
      type: 'gradient',
      gradient: {
        opacityFrom: 0.6,
        opacityTo: 0.1
      }
    },
    legend: {
      position: 'top',
      fontSize: '12px',
      fontWeight: 500,
      horizontalAlign: 'right'
    },
    grid: {
      borderColor: '#e5e7eb',
      strokeDashArray: 4
    },
    tooltip: {
      y: {
        formatter: function(val) {
          return val + ' operations'
        }
      }
    }
  }

  // Use ApexCharts directly
  if (window.ApexCharts) {
    activityChart = new window.ApexCharts(activityChartRef.value, options)
    activityChart.render()
  }
}

// Update charts when data changes
watch(() => props.featureFlags, () => {
  if (rolloutChart) {
    const distribution = rolloutDistribution.value
    rolloutChart.updateSeries(Object.values(distribution))
  }
  if (teamChart) {
    const teamData = teamPerformance.value
    const teams = Object.keys(teamData).sort()
    const totalData = teams.map(team => teamData[team].total)
    const activeData = teams.map(team => teamData[team].active)
    teamChart.updateOptions({
      xaxis: {
        categories: teams
      }
    })
    teamChart.updateSeries([
      { name: 'Total Flags', data: totalData },
      { name: 'Active Flags', data: activeData }
    ])
  }
}, { deep: true })

// Update activity chart when audit logs change
watch(() => props.auditLogs, () => {
  if (activityChart) {
    const timeline = activityTimeline.value
    const dates = Object.keys(timeline).sort()
    const createData = dates.map(date => timeline[date].CREATE)
    const updateData = dates.map(date => timeline[date].UPDATE)
    const deleteData = dates.map(date => timeline[date].DELETE)

    activityChart.updateOptions({
      xaxis: {
        categories: dates.map(date => {
          const d = new Date(date)
          return (d.getMonth() + 1) + '/' + d.getDate()
        })
      }
    })
    activityChart.updateSeries([
      { name: 'Created', data: createData },
      { name: 'Updated', data: updateData },
      { name: 'Deleted', data: deleteData }
    ])
  }
}, { deep: true })

onMounted(() => {
  // Load ApexCharts from CDN if not already loaded
  if (!window.ApexCharts) {
    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/apexcharts@3.45.2/dist/apexcharts.min.js'
    script.onload = () => {
      initRolloutChart()
      initTeamChart()
      initActivityChart()
    }
    document.head.appendChild(script)
  } else {
    initRolloutChart()
    initTeamChart()
    initActivityChart()
  }
})
</script>

<style scoped>
/* Chart container styles */
</style>
