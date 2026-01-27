import {attachmentsApi} from './attachments-api.js';
import {handleError} from '../http-utils.js';

class AttachmentFormManager {
    constructor() {
        this.form = document.getElementById('attachmentForm');
        this.hasUnsavedChanges = false; // Per saber si ha canviat alguna cosa en el formulari
        this.elements = this.getFormElements(); // Elements del formulari
    }

    getFormElements() {
        return {
            code: document.querySelector('input[name="code"]'),
            name: document.querySelector('input[name="name"]'),
            fileInput: document.getElementById('fileInput'),
            id: document.querySelector('input[name="id"]')
        };
    }

    // Detecta canvis en els camps del formulari
    setupChangeDetection() {
        const fields = [
            {element: this.elements.code, event: 'input'},
            {element: this.elements.name, event: 'input'},
            {element: this.elements.fileInput, event: 'change'}
        ];

        fields.forEach(({element, event}) => {
            element?.addEventListener(event, () => {
                this.hasUnsavedChanges = true;  // Canviar a true per indicar que un camp ha canviat
            });
        });
    }

    // Avís de canvis no guardats
    setupBeforeUnload() {
        window.addEventListener('beforeunload', (e) => {
            if (this.hasUnsavedChanges) e.preventDefault();
        });
    }

    // Actualitzar label de l'arxiu
    setupFileInputLabel() {
        const {fileInput} = this.elements;
        if (!fileInput) return;

        fileInput.addEventListener('change', function () {
            const fileName = this.files[0]?.name || 'Cap arxiu seleccionat';
            const label = this.nextElementSibling?.querySelector('span');
            if (label) label.textContent = fileName;
        });
    }

    validateForm() {
        const {fileInput, id} = this.elements;
        const isEdit = id && id.value;
        const hasFile = fileInput.files[0];

        if (!hasFile && !isEdit) {
            alert('Cal seleccionar un arxiu');
            return false;
        }
        return true;
    }

    getFormData() {
        const {code, name, fileInput, id} = this.elements;
        const formData = new FormData();
        formData.append('code', code.value.trim());
        formData.append('name', name.value.trim());

        if (fileInput.files[0]) formData.append('file', fileInput.files[0]);
        if (id?.value) formData.append('id', id.value);

        return {formData, isEdit: !!id?.value};
    }

    async handleSubmit(event) {
        event.preventDefault();
        if (!this.validateForm()) return;   // Validar formulari

        const {formData, isEdit} = this.getFormData();

        try {
            const result = await attachmentsApi.saveAttachment(formData, isEdit);
            if (result.item?.id) {
                this.hasUnsavedChanges = false;
                alert('Èxit: L\'adjunt s\'ha guardat correctament');
                window.location.replace(`/plantilles-web/attachments/edit/${result.item.id}`);
            }
        } catch (error) {
            handleError('Error al guardar l\'adjunt', error);
        }
    }

    init() {
        if (!this.form) return; // Si no hi ha formulari, no fem res
        this.setupChangeDetection(); // Detectar canvis en els camps del formulari
        this.setupBeforeUnload();   // Avís si hi ha canvis no guardats al sortir
        this.setupFileInputLabel(); // Actualitzar label de l'arxiu
        this.form.addEventListener('submit', (e) => this.handleSubmit(e));  // Afegir event listener de submit al formulari
    }
}

// Inicialització
document.addEventListener('DOMContentLoaded', () => {
    new AttachmentFormManager().init();
});