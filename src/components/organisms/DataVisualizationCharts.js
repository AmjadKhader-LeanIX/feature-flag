/* ============================================
   DATA VISUALIZATION CHARTS COMPONENT
   ApexCharts Integration for Feature Flag Dashboard
   ============================================ */

const DataVisualizationCharts = {
    name: 'DataVisualizationCharts',
    props: {
        featureFlags: {
            type: Array,
            default: () => []
        },
        auditLogs: {
            type: Array,
            default: () => []
        },
        workspaces: {
            type: Array,
            default: () => []
        }
    },
    setup(props) {
        const { ref, computed, watch, onMounted, nextTick } = Vue;

        const rolloutChart = ref(null);
        const teamChart = ref(null);
        const activityChart = ref(null);

        const rolloutChartInstance = ref(null);
        const teamChartInstance = ref(null);
        const activityChartInstance = ref(null);

        // Compute rollout distribution data
        const rolloutDistribution = computed(() => {
            const buckets = {
                'Disabled (0%)': 0,
                'Testing (1-25%)': 0,
                'Partial (26-75%)': 0,
                'Advanced (76-99%)': 0,
                'Full (100%)': 0
            };

            props.featureFlags.forEach(flag => {
                const rollout = flag.rolloutPercentage;
                if (rollout === 0) buckets['Disabled (0%)']++;
                else if (rollout <= 25) buckets['Testing (1-25%)']++;
                else if (rollout <= 75) buckets['Partial (26-75%)']++;
                else if (rollout < 100) buckets['Advanced (76-99%)']++;
                else buckets['Full (100%)']++;
            });

            return buckets;
        });

        // Compute team performance data
        const teamPerformance = computed(() => {
            const teamData = {};

            props.featureFlags.forEach(flag => {
                if (!teamData[flag.team]) {
                    teamData[flag.team] = {
                        total: 0,
                        active: 0
                    };
                }
                teamData[flag.team].total++;
                if (flag.rolloutPercentage > 0) {
                    teamData[flag.team].active++;
                }
            });

            return teamData;
        });

        // Compute activity timeline data (last 30 days)
        const activityTimeline = computed(() => {
            const days = 30;
            const today = new Date();
            const timeline = {};

            // Initialize last 30 days
            for (let i = days - 1; i >= 0; i--) {
                const date = new Date(today);
                date.setDate(date.getDate() - i);
                const dateStr = date.toISOString().split('T')[0];
                timeline[dateStr] = { CREATE: 0, UPDATE: 0, DELETE: 0 };
            }

            // Count operations per day
            props.auditLogs.forEach(log => {
                const logDate = new Date(log.timestamp).toISOString().split('T')[0];
                if (timeline[logDate]) {
                    timeline[logDate][log.operation] = (timeline[logDate][log.operation] || 0) + 1;
                }
            });

            return timeline;
        });

        // Initialize Rollout Distribution Chart (Donut)
        const initRolloutChart = () => {
            if (!rolloutChart.value || typeof ApexCharts === 'undefined') return;

            const distribution = rolloutDistribution.value;
            const series = Object.values(distribution);
            const labels = Object.keys(distribution);

            const options = {
                series: series,
                chart: {
                    type: 'donut',
                    height: 350,
                    fontFamily: 'Inter, sans-serif',
                    toolbar: {
                        show: true
                    }
                },
                labels: labels,
                colors: ['#9ca3af', '#fbbf24', '#3b82f6', '#10b981', '#059669'],
                legend: {
                    position: 'bottom',
                    fontSize: '14px',
                    fontWeight: 500
                },
                dataLabels: {
                    enabled: true,
                    formatter: function(val, opts) {
                        const value = opts.w.globals.series[opts.seriesIndex];
                        return value > 0 ? value : '';
                    },
                    style: {
                        fontSize: '14px',
                        fontWeight: 600
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
                                    fontWeight: 600,
                                    color: '#223548'
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
                                    color: '#64748b',
                                    formatter: function(w) {
                                        return w.globals.seriesTotals.reduce((a, b) => a + b, 0);
                                    }
                                }
                            }
                        }
                    }
                },
                tooltip: {
                    y: {
                        formatter: function(val) {
                            return val + ' flags';
                        }
                    }
                }
            };

            if (rolloutChartInstance.value) {
                rolloutChartInstance.value.destroy();
            }
            rolloutChartInstance.value = new ApexCharts(rolloutChart.value, options);
            rolloutChartInstance.value.render();
        };

        // Initialize Team Performance Chart (Grouped Bar)
        const initTeamChart = () => {
            if (!teamChart.value || typeof ApexCharts === 'undefined') return;

            const performance = teamPerformance.value;
            const teams = Object.keys(performance);
            const totalFlags = teams.map(team => performance[team].total);
            const activeFlags = teams.map(team => performance[team].active);

            const options = {
                series: [
                    {
                        name: 'Total Flags',
                        data: totalFlags
                    },
                    {
                        name: 'Active Flags',
                        data: activeFlags
                    }
                ],
                chart: {
                    type: 'bar',
                    height: 350,
                    fontFamily: 'Inter, sans-serif',
                    toolbar: {
                        show: true
                    }
                },
                plotOptions: {
                    bar: {
                        horizontal: false,
                        columnWidth: '70%',
                        borderRadius: 8,
                        dataLabels: {
                            position: 'top'
                        }
                    }
                },
                dataLabels: {
                    enabled: false
                },
                xaxis: {
                    categories: teams,
                    labels: {
                        style: {
                            fontSize: '12px',
                            fontWeight: 500
                        }
                    }
                },
                yaxis: {
                    title: {
                        text: 'Number of Flags',
                        style: {
                            fontSize: '14px',
                            fontWeight: 600
                        }
                    },
                    labels: {
                        formatter: function(val) {
                            return Math.floor(val);
                        }
                    }
                },
                colors: ['#002a86', '#10b981'],
                legend: {
                    position: 'top',
                    fontSize: '14px',
                    fontWeight: 500
                },
                tooltip: {
                    y: {
                        formatter: function(val) {
                            return val + ' flags';
                        }
                    }
                }
            };

            if (teamChartInstance.value) {
                teamChartInstance.value.destroy();
            }
            teamChartInstance.value = new ApexCharts(teamChart.value, options);
            teamChartInstance.value.render();
        };

        // Initialize Activity Timeline Chart (Area)
        const initActivityChart = () => {
            if (!activityChart.value || typeof ApexCharts === 'undefined') return;

            const timeline = activityTimeline.value;
            const dates = Object.keys(timeline).sort();
            const createData = dates.map(date => timeline[date].CREATE);
            const updateData = dates.map(date => timeline[date].UPDATE);
            const deleteData = dates.map(date => timeline[date].DELETE);

            const options = {
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
                chart: {
                    type: 'area',
                    height: 350,
                    fontFamily: 'Inter, sans-serif',
                    toolbar: {
                        show: true
                    },
                    zoom: {
                        enabled: true
                    }
                },
                dataLabels: {
                    enabled: false
                },
                stroke: {
                    curve: 'smooth',
                    width: 2
                },
                xaxis: {
                    categories: dates.map(date => {
                        const d = new Date(date);
                        return (d.getMonth() + 1) + '/' + d.getDate();
                    }),
                    labels: {
                        style: {
                            fontSize: '11px',
                            fontWeight: 500
                        },
                        rotate: -45
                    }
                },
                yaxis: {
                    title: {
                        text: 'Operations Count',
                        style: {
                            fontSize: '14px',
                            fontWeight: 600
                        }
                    },
                    labels: {
                        formatter: function(val) {
                            return Math.floor(val);
                        }
                    }
                },
                colors: ['#10b981', '#3b82f6', '#ef4444'],
                fill: {
                    type: 'gradient',
                    gradient: {
                        opacityFrom: 0.6,
                        opacityTo: 0.1
                    }
                },
                legend: {
                    position: 'top',
                    fontSize: '14px',
                    fontWeight: 500
                },
                tooltip: {
                    x: {
                        format: 'MMM dd, yyyy'
                    },
                    y: {
                        formatter: function(val) {
                            return val + ' operations';
                        }
                    }
                }
            };

            if (activityChartInstance.value) {
                activityChartInstance.value.destroy();
            }
            activityChartInstance.value = new ApexCharts(activityChart.value, options);
            activityChartInstance.value.render();
        };

        // Initialize all charts
        const initializeCharts = async () => {
            await nextTick();
            initRolloutChart();
            initTeamChart();
            initActivityChart();
        };

        // Watch for data changes
        watch(() => props.featureFlags, () => {
            nextTick(() => {
                initRolloutChart();
                initTeamChart();
            });
        }, { deep: true });

        watch(() => props.auditLogs, () => {
            nextTick(() => {
                initActivityChart();
            });
        }, { deep: true });

        onMounted(() => {
            setTimeout(initializeCharts, 100);
        });

        return {
            rolloutChart,
            teamChart,
            activityChart
        };
    },
    template: `
        <div class="charts-grid">
            <div class="chart-card">
                <div class="chart-header">
                    <h3><i class="fas fa-chart-pie"></i> Rollout Distribution</h3>
                    <p>Distribution of flags by rollout percentage</p>
                </div>
                <div class="chart-container">
                    <div ref="rolloutChart"></div>
                </div>
            </div>

            <div class="chart-card">
                <div class="chart-header">
                    <h3><i class="fas fa-users"></i> Team Performance</h3>
                    <p>Total vs active flags per team</p>
                </div>
                <div class="chart-container">
                    <div ref="teamChart"></div>
                </div>
            </div>

            <div class="chart-card chart-card-wide">
                <div class="chart-header">
                    <h3><i class="fas fa-chart-line"></i> Activity Timeline (Last 30 Days)</h3>
                    <p>Operations over time</p>
                </div>
                <div class="chart-container">
                    <div ref="activityChart"></div>
                </div>
            </div>
        </div>
    `
};

// Export for use in other components
if (typeof window !== 'undefined') {
    window.DataVisualizationCharts = DataVisualizationCharts;
}
