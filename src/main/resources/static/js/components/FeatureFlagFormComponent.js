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
                    :disabled="isEdit"
                />
            </div>
            <div class="form-group">
                <label for="flag-description">Description</label>
                <textarea
                    id="flag-description"
                    v-model="form.description"
                    placeholder="Enter description (optional)"
                    :disabled="isEdit"
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
                    :disabled="isEdit"
                />
            </div>
            <div v-if="!isEdit" class="form-group">
                <label>Regions *</label>
                <div style="max-height: 200px; overflow-y: auto; border: 1px solid #ddd; padding: 10px; border-radius: 4px;">
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="ALL" v-model="form.regions" />
                        ALL (All Regions)
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="WESTEUROPE" v-model="form.regions" :disabled="form.regions.includes('ALL')" />
                        West Europe
                    </label>
                    <label class="checkbox-label" style="display: block; margin-bottom: 8px;">
                        <input type="checkbox" value="EASTUS" v-model="form.regions" :disabled="form.regions.includes('ALL')" />
                        East US
                    </label>
                </div>
                <small v-if="form.regions.length === 0" style="color: red;">Please select at least one region</small>
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
                    />
                    <span class="rollout-display">{{ form.rolloutPercentage }}%</span>
                </div>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" @click="cancel">Cancel</button>
                <button type="submit" class="btn btn-primary" :disabled="!isEdit && form.regions.length === 0">
                    {{ isEdit ? 'Update' : 'Create' }}
                </button>
            </div>
        </form>
    `,
    props: ['featureFlag', 'isEdit'],
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
