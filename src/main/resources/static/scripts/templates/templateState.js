/**
 * Gestiona l'estat, referències i dades per a la plantilla.
 * */

// Guarda referències
export function createTemplateState() {
    const els = {
        // editor + forms
        htmlContent: document.getElementById('htmlContent'),
        saveForm: document.getElementById('saveForm'),
        configForm: document.getElementById('configForm'),
        styleForm: document.getElementById('styleForm'),

        // template identity
        templateId: document.querySelector('input[name="id"]'),
        boxId: document.querySelector('input[name="boxId"]'),
        boxType: document.querySelector('input[name="boxType"]'),

        // name/code (modal save + modal config)
        nomPlantilla: document.getElementById('nomPlantilla'),
        codiPlantilla: document.getElementById('codiPlantilla'),

        // style
        styleSelect: document.getElementById('stylePlantilla'),
        styleHidden: document.getElementById('stylePlantillaHidden'),
        styleIdFromBackend: document.querySelector('input[name="styleId"]'),

        // variables
        listContainer: document.querySelector('#variableList'),
        searchInput: document.getElementById('searchVariable'),

        variableForm: document.getElementById('variableForm'),
        importModelForm: document.getElementById('importModelForm'),
        editVariableForm: document.getElementById('editVariableForm')
    };

    // Utilitat per mapejar strings dels tipus de variables amb números
    const typeUtils = {
        TYPES: ["short", "int", "long", "float", "double", "char", "boolean", "String", "List", "Map", "Data", "Html", "csv_html"],
        getString(num) {
            return this.TYPES[num] || 'String';
        },
        getNumber(str) {
            return this.TYPES.indexOf(str);
        }
    };

    // Objecte per emmagatzemar l'estat de la plantilla, mètodes sobre l'estat punt d'integració entre mòduls
    const state = {
        hasUnsavedChanges: false,   // flag que indica si hi han canvis no guardats
        variables: [],  // llista de variables actuals (creades, importades o editades)
        config: {width: 210, height: 297, marginTop: 18, marginBottom: 18, marginLeft: 14, marginRight: 14}, //objecte des del modal de configuració per gestionar mida i marges
        configSnapshot: null,   // còpia del config per poder restaurar al canceler
        isConfigSaveAction: false,  // flag per restaurar al tancament del modal o no (si no es guarda)

        els,    // objecte de referències als elements del DOM
        typeUtils,  // utilitats per convertir tipus

        markDirty() {
            this.hasUnsavedChanges = true;
        }, // marca que hi han canvis no guardats
        clearDirty() {
            this.hasUnsavedChanges = false;
        },   // marca que no hi han canvis no guardats

        // Listener per mostrar avís de canvis no guardats si el flag està activat de canvis no guardats
        enableBeforeUnloadWarning() {
            window.addEventListener('beforeunload', (e) => {
                if (this.hasUnsavedChanges) e.preventDefault();
            });
        },

        // Validar si es mode editar o crear una plantilla
        isEdit() {
            const id = this.els.templateId?.value;
            return id && id !== '1';
        },

        // Escolta event input del textarea per marcar el flag de canvis no guardats si l'usuari modifica el text
        wireEditorDirty() {
            this.els.htmlContent?.addEventListener('input', () => this.markDirty());
        },

        // Listener global per afegir funcions al textarea i marcar el flag de canvis no guardats
        wireFunctionItems() {
            document.addEventListener('click', (e) => {
                const item = e.target.closest('.function-item');
                if (!item) return;

                e.preventDefault();

                const htmlText = item.getAttribute('data-html-text');
                if (!htmlText) return;

                const textarea = this.els.htmlContent;
                if (!textarea) return;

                const start = textarea.selectionStart ?? textarea.value.length;
                const end = textarea.selectionEnd ?? textarea.value.length;

                const before = textarea.value.substring(0, start);
                const after = textarea.value.substring(end);

                textarea.value = before + htmlText + after;
                const caret = start + htmlText.length;
                textarea.selectionStart = textarea.selectionEnd = caret;
                textarea.focus();

                this.markDirty();
            });
        },

        // Obtenir l'estil de la plantilla actualment seleccionat
        getPrevStyleId() {
            const hidden = (this.els.styleHidden?.value || '').trim();
            if (hidden !== '') return hidden;

            const backend = (this.els.styleIdFromBackend?.value || '').trim();
            return backend !== '' ? backend : null;
        },

        // Guarda el nou estil aplicat en el hidden
        setStyleHidden(nextStyleId) {
            if (!this.els.styleHidden) return;
            this.els.styleHidden.value = nextStyleId ?? '';
        },

        // Carga el config des del modal
        loadConfigFromConfigModalInputs() {
            const intVal = (id, fallback) => {
                const v = document.getElementById(id)?.value;
                const n = parseInt(v, 10);
                return Number.isFinite(n) ? n : fallback;
            };

            this.config = {
                width: intVal('configAmplada', this.config.width),
                height: intVal('configAllargada', this.config.height),
                marginTop: intVal('configMargeSuperior', this.config.marginTop),
                marginBottom: intVal('configMargeInferior', this.config.marginBottom),
                marginLeft: intVal('configMargeEsquerre', this.config.marginLeft),
                marginRight: intVal('configMargeDret', this.config.marginRight)
            };
        },

        // Construeix l'objecte final que s'enviarà al controlador per crear o actualitzar una plantilla
        buildTemplatePayload() {
            const nom = this.els.nomPlantilla?.value?.trim();
            const code = this.els.codiPlantilla?.value?.trim();
            if (!nom || !code) throw new Error('Falten nom o codi');

            const html = this.els.htmlContent?.value ?? '';

            const existingBoxId = this.isEdit() && this.els.boxId?.value && this.els.boxId.value !== 'null'
                ? parseInt(this.els.boxId.value, 10)
                : null;

            const existingBoxType = this.isEdit() && this.els.boxType?.value && this.els.boxType.value !== 'null'
                ? parseInt(this.els.boxType.value, 10)
                : null;

            const variables = this.variables.map(v => {
                const mapped = {name: v.name, type: this.typeUtils.getNumber(v.type)};
                if (v.id) mapped.id = v.id;
                return mapped;
            });

            const payload = {
                name: nom,
                code,
                parentFolder: 1,
                maxColPosition: 23,
                maxRowPosition: 33,
                pageDimensions: {
                    width: this.config.width,
                    height: this.config.height,
                    marginTop: this.config.marginTop,
                    marginBottom: this.config.marginBottom,
                    marginLeft: this.config.marginLeft,
                    marginRight: this.config.marginRight
                },
                boxes: [{
                    id: existingBoxId,
                    boxType: existingBoxType,
                    rowPosition: 1,
                    colPosition: 1,
                    height: 31,
                    width: 23,
                    innerHtml: html,
                    contentConfiguration: {gspCode: "${raw(text)}", variables: []}
                }],
                variables
            };

            if (this.isEdit()) payload.id = parseInt(this.els.templateId.value, 10);

            const styleId = (this.els.styleSelect?.value || '').trim();
            const hiddenStyle = (this.els.styleHidden?.value || '').trim();
            //const finalStyle = hiddenStyle !== '' ? hiddenStyle : (styleId !== '' ? styleId : null);
            const finalStyle = this.getPrevStyleId();
            if (finalStyle) payload.style = parseInt(finalStyle, 10);

            return payload;
        },

        // Listener per fer el fetch al backend per guardar la plantilla
        wireSave(doSave, handleErrorFn) {
            this.els.saveForm?.addEventListener('submit', async (e) => {
                e.preventDefault();
                try {
                    const result = await doSave();
                    if (result?.item?.id) {
                        alert('Èxit: La plantilla s\'ha guardat correctament');
                        $('#saveModal').modal('hide');
                        window.location.replace(`/plantilles-web/templates/edit/${result.item.id}`);
                    }
                } catch (err) {
                    handleErrorFn('Error', new Error('No ha sigut possible guardar la plantilla correctament'));
                }
            });

            $('#saveModal').on('hidden.bs.modal', () => this.els.saveForm?.reset());
        }
    };

    // Inicialitza el config per sincronitzar amb els valors correctes actuals
    state.loadConfigFromConfigModalInputs();

    return state;
}