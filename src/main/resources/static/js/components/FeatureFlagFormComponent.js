const FeatureFlagFormComponent = {
    template: `
        <form @submit.prevent="submit">
            <div class="form-group">
                <label for="flag-name">Name *</label>
                <input
                    id="flag-name"
                    v-model="form.name"
                    type="text"
                    required
                    placeholder="Enter feature flag name"
                    :disabled="isEdit || loading"
                />
            </div>
            <div class="form-group">
                <label for="flag-description">Description</label>
                <textarea
                    id="flag-description"
                    v-model="form.description"
                    placeholder="Enter description (optional)"
                    :disabled="isEdit || loading"
                ></textarea>
            </div>
            <div class="form-group">
                <label for="flag-team">Team *</label>
                <input
                    id="flag-team"
                    v-model="form.team"
                    type="text"
                    required
                    placeholder="Enter team name"
                    :disabled="isEdit || loading"
                />
            </div>
            <div v-if="!isEdit" class="form-group">
                <label>Regions *</label>
                <div style="max-height: 200px; overflow-y: auto; border: 1px solid var(--border-color); padding: var(--spacing-3); border-radius: var(--radius-md); background: var(--bg-secondary);">
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="ALL" v-model="form.regions" :disabled="loading" />
                        ALL (All Regions)
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="WESTEUROPE" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        West Europe
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="EASTUS" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        East US
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="CANADACENTRAL" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        Canada Central
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="AUSTRALIAEAST" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        Australia East
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="GERMANYWESTCENTRAL" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        Germany West Central
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="SWITZERLANDNORTH" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        Switzerland North
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="UAENORTH" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        UAE North
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="UKSOUTH" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        UK South
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="BRAZILSOUTH" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        Brazil South
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="SOUTHEASTASIA" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        Southeast Asia
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="JAPANEAST" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        Japan East
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="NORTHEUROPE" v-model="form.regions" :disabled="form.regions.includes('ALL') || loading" />
                        North Europe
                    </label>
                </div>
                <small v-if="form.regions.length === 0" style="color: var(--danger-color); font-size: var(--text-xs); margin-top: var(--spacing-2); display: block;">Please select at least one region</small>
            </div>
            <div v-if="isEdit" class="form-group">
                <label for="flag-rollout">Rollout Percentage</label>
                <div class="range-input">
                    <input
                        id="flag-rollout"
                        v-model.number="form.rolloutPercentage"
                        type="range"
                        min="0"
                        max="100"
                        :disabled="loading"
                    />
                    <span class="rollout-display">{{ form.rolloutPercentage }}%</span>
                </div>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" @click="cancel" :disabled="loading">Cancel</button>
                <button type="submit" class="btn btn-primary" :disabled="(!isEdit && form.regions.length === 0) || loading">
                    <i v-if="loading" class="fas fa-spinner fa-spin" style="margin-right: 8px;"></i>
                    {{ loading ? 'Processing...' : (isEdit ? 'Update' : 'Create') }}
                </button>
            </div>
        </form>
    `,
    props: ['featureFlag', 'isEdit', 'loading'],
    emits: ['submit', 'cancel'],
    data() {
        return {
            form: {
                name: '',
                description: '',
                team: '',
                regions: ['ALL'],
                rolloutPercentage: 0
            }
        };
    },
    watch: {
        featureFlag: {
            immediate: true,
            handler(newFlag) {
                if (newFlag) {
                    this.form.name = newFlag.name || '';
                    this.form.description = newFlag.description || '';
                    this.form.team = newFlag.team || '';
                    this.form.regions = newFlag.regions || ['ALL'];
                    this.form.rolloutPercentage = newFlag.rolloutPercentage || 0;
                } else {
                    this.form.name = '';
                    this.form.description = '';
                    this.form.team = '';
                    this.form.regions = ['ALL'];
                    this.form.rolloutPercentage = 0;
                }
            }
        },
        'form.regions': {
            handler(newRegions) {
                if (newRegions.includes('ALL') && newRegions.length > 1) {
                    this.form.regions = ['ALL'];
                }
            }
        }
    },
    methods: {
        submit() {
            if (!this.isEdit && this.form.regions.length === 0) {
                return;
            }
            const data = {
                name: this.form.name,
                description: this.form.description || null,
                team: this.form.team,
                regions: this.form.regions,
                rolloutPercentage: this.isEdit ? this.form.rolloutPercentage : 0
            };
            this.$emit('submit', data);
        },
        cancel() {
            this.$emit('cancel');
        }
    }
};
