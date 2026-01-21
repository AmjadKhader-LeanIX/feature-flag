/* ============================================
   SKELETON LOADER COMPONENT
   Professional Loading Placeholders
   ============================================ */

const SkeletonLoader = {
    name: 'SkeletonLoader',
    props: {
        type: {
            type: String,
            default: 'card'
        },
        count: {
            type: Number,
            default: 1
        },
        height: {
            type: String,
            default: null
        }
    },
    setup(props) {
        const items = Array.from({ length: props.count }, (_, i) => i);

        return {
            items
        };
    },
    template: `
        <div class="skeleton-loader">
            <!-- Stat Card Skeleton -->
            <div v-if="type === 'stat'" class="skeleton-stat-grid">
                <div v-for="item in items" :key="item" class="skeleton-stat-card">
                    <div class="skeleton-stat-icon skeleton"></div>
                    <div class="skeleton-stat-content">
                        <div class="skeleton skeleton-title" style="width: 60%;"></div>
                        <div class="skeleton skeleton-text" style="width: 40%;"></div>
                    </div>
                </div>
            </div>

            <!-- Flag Card Skeleton -->
            <div v-else-if="type === 'flag'" class="skeleton-flag-list">
                <div v-for="item in items" :key="item" class="skeleton-flag-card">
                    <div class="skeleton-flag-header">
                        <div class="skeleton skeleton-title" style="width: 50%;"></div>
                        <div class="skeleton skeleton-avatar"></div>
                    </div>
                    <div class="skeleton skeleton-paragraph" style="width: 90%;"></div>
                    <div class="skeleton skeleton-paragraph" style="width: 70%;"></div>
                    <div class="skeleton-flag-meta">
                        <div class="skeleton skeleton-button" style="width: 80px;"></div>
                        <div class="skeleton skeleton-button" style="width: 60px;"></div>
                        <div class="skeleton skeleton-button" style="width: 100px;"></div>
                    </div>
                    <div class="skeleton skeleton-paragraph" style="width: 100%; height: 10px; margin-top: 12px;"></div>
                </div>
            </div>

            <!-- Workspace Card Skeleton -->
            <div v-else-if="type === 'workspace'" class="skeleton-workspace-grid">
                <div v-for="item in items" :key="item" class="skeleton-workspace-card">
                    <div class="skeleton-workspace-header">
                        <div class="skeleton skeleton-title" style="width: 60%;"></div>
                        <div class="skeleton skeleton-button" style="width: 80px;"></div>
                    </div>
                    <div class="skeleton-workspace-body">
                        <div class="skeleton skeleton-paragraph" style="width: 40%; margin-bottom: 16px;"></div>
                        <div v-for="i in 3" :key="i" class="skeleton-workspace-flag-item">
                            <div class="skeleton skeleton-paragraph" style="width: 70%;"></div>
                            <div class="skeleton skeleton-text" style="width: 50%;"></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Table Skeleton -->
            <div v-else-if="type === 'table'" class="skeleton-table">
                <div class="skeleton-table-header">
                    <div v-for="i in 5" :key="i" class="skeleton skeleton-text"></div>
                </div>
                <div class="skeleton-table-body">
                    <div v-for="item in items" :key="item" class="skeleton-table-row">
                        <div v-for="i in 5" :key="i" class="skeleton skeleton-paragraph"></div>
                    </div>
                </div>
            </div>

            <!-- List Skeleton -->
            <div v-else-if="type === 'list'" class="skeleton-list">
                <div v-for="item in items" :key="item" class="skeleton-list-item">
                    <div class="skeleton skeleton-avatar"></div>
                    <div class="skeleton-list-content">
                        <div class="skeleton skeleton-title" style="width: 60%;"></div>
                        <div class="skeleton skeleton-paragraph" style="width: 80%;"></div>
                    </div>
                </div>
            </div>

            <!-- Chart Skeleton -->
            <div v-else-if="type === 'chart'" class="skeleton-chart-container">
                <div v-for="item in items" :key="item" class="skeleton-chart">
                    <div class="skeleton skeleton-title" style="width: 40%; margin-bottom: 24px;"></div>
                    <div class="skeleton-chart-bars">
                        <div v-for="i in 6" :key="i" class="skeleton-chart-bar" :style="{ height: (Math.random() * 60 + 40) + '%' }"></div>
                    </div>
                </div>
            </div>

            <!-- Generic Card Skeleton -->
            <div v-else class="skeleton-cards">
                <div v-for="item in items" :key="item" class="skeleton-card" :style="height ? { height } : {}">
                    <div class="skeleton skeleton-title" style="width: 60%;"></div>
                    <div class="skeleton skeleton-paragraph" style="width: 100%;"></div>
                    <div class="skeleton skeleton-paragraph" style="width: 80%;"></div>
                </div>
            </div>
        </div>
    `
};

// Export for use in other components
if (typeof window !== 'undefined') {
    window.SkeletonLoader = SkeletonLoader;
}
