const ToastComponent = {
    template: `
        <Transition name="slide-fade">
            <div v-if="visible" :class="['toast', type, 'show']">
                <div class="toast-content">
                    <span class="toast-message">{{ message }}</span>
                    <button class="toast-close" @click="close">×</button>
                </div>
            </div>
        </Transition>
    `,
    props: ['visible', 'message', 'type'],
    emits: ['close'],
    methods: {
        close() {
            this.$emit('close');
        }
    }
};
