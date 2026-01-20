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
                <button type="submit" class="btn btn-primary" :disabled="loading">
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
                    this.form.rolloutPercentage = newFlag.rolloutPercentage || 0;
                } else {
                    this.form.name = '';
                    this.form.description = '';
                    this.form.team = '';
                    this.form.rolloutPercentage = 0;
                }
            }
        }
    },
    methods: {
        submit() {
            const data = {
                name: this.form.name,
                description: this.form.description || null,
                team: this.form.team,
                rolloutPercentage: this.isEdit ? this.form.rolloutPercentage : 0
            };
            this.$emit('submit', data);
        },
        cancel() {
            this.$emit('cancel');
        }
    }
};
