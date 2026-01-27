import {stylesApi} from './styles-api.js';
import {handleError} from '../http-utils.js';

class StyleFormManager {
    constructor() {
        this.form = document.getElementById('styleForm');
        this.hasUnsavedChanges = false; // Per saber si ha canviat alguna cosa en el formulari
        this.elements = this.getFormElements(); // Elements del formulari
    }

    getFormElements() {
        return {
            code: document.querySelector('input[name="code"]'),
            name: document.querySelector('input[name="name"]'),
            rules: document.querySelector('textarea[name="rules"]'),
            ensLocal: document.querySelector('select[name="ensLocal"]'),
            id: document.querySelector('input[name="id"]')
        };
    }

    // Carregar els ens locals al select de l'estil
    async loadEnsLocals() {
        try {
            const ensLocals = await stylesApi.loadEnsLocals();
            const select = this.elements.ensLocal;

            if (!select) return;

            ensLocals.forEach(ensLocal => {
                const option = document.createElement('option');
                option.value = ensLocal.id;
                option.textContent = ensLocal.nom;
                select.appendChild(option);
            });

            // Si està a mode edició, selecciona valor actual
            const currentEnsLocalId = select.querySelector('option[selected]')?.value;
            if (currentEnsLocalId) {
                select.value = currentEnsLocalId;
            }
        } catch (error) {
            handleError('Error carregant ensLocals', error);
        }
    }

    // Detecta canvis en els camps del formulari
    setupChangeDetection() {
        const fields = ['code', 'name', 'rules', 'ensLocal'];

        fields.forEach(field => {
            const element = this.elements[field];
            const eventType = field === 'ensLocal' ? 'change' : 'input';

            element?.addEventListener(eventType, () => {
                this.hasUnsavedChanges = true;
            });
        });
    }

    // Avís de canvis no guardats
    setupBeforeUnload() {
        window.addEventListener('beforeunload', (e) => {
            if (this.hasUnsavedChanges) {
                e.preventDefault();
            }
        });
    }

    getFormData() {
        const {code, name, rules, ensLocal, id} = this.elements;

        const ensLocalId = ensLocal.value?.trim();
        const ensLocalData = {
            id: ensLocalId || '',
            nom: ensLocalId ? (ensLocal.options[ensLocal.selectedIndex]?.text || '') : ''
        };

        const styleData = {
            code: code.value.trim(),
            name: name.value.trim(),
            rules: rules.value,
            ensLocal: ensLocalData
        };

        const isEdit = id && id.value;
        if (isEdit) {
            styleData.id = parseInt(id.value, 10);
        }

        return {styleData, isEdit};
    }

    async handleSubmit(event) {
        event.preventDefault();

        const {styleData, isEdit} = this.getFormData();

        try {
            const result = await stylesApi.saveStyle(styleData, isEdit);

            if (result.item?.id) {
                this.hasUnsavedChanges = false;
                alert('Èxit: L\'estil s\'ha guardat correctament');
                window.location.replace(`/plantilles-web/styles/edit/${result.item.id}`);
            }
        } catch (error) {
            handleError('Error al guardar l\'estil', error);
        }
    }

    async init() {
        if (!this.form) return; // Si no hi ha formulari, no fem res
        await this.loadEnsLocals(); // Carregar els ens locals disponibles
        this.setupChangeDetection(); // Detectar canvis en els camps del formulari
        this.setupBeforeUnload();   // Avís si hi ha canvis no guardats al sortir
        this.form.addEventListener('submit', (e) => this.handleSubmit(e)); // Afegir event listener de submit al formulari
    }
}

// Inicialització
document.addEventListener('DOMContentLoaded', async () => {
    const formManager = new StyleFormManager();
    await formManager.init();
});