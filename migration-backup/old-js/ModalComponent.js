const ModalComponent = {
    template: `
        <Transition name="fade">
            <div v-if="visible" class="modal-overlay" @click="closeModal">
                <div class="modal" @click.stop>
                    <div class="modal-header">
                        <h2>{{ title }}</h2>
                        <button class="modal-close" @click="closeModal">×</button>
                    </div>
                    <div class="modal-content">
                        <slot></slot>
                    </div>
                </div>
            </div>
        </Transition>
    `,
    props: ['visible', 'title'],
    emits: ['close'],
    methods: {
        closeModal() {
            this.$emit('close');
        }
    }
};
