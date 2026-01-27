import {templatesApi} from './templates-api.js';
import {handleError} from '../http-utils.js';

/**
 * Elimina una plantilla (accessible globalment des d'HTML)
 * @param {number} id - ID de la plantilla a eliminar
 */
window.deleteTemplate = async function (id) {
    if (!confirm('Estàs segur que vols eliminar aquest element?')) {
        return;
    }

    try {
        await templatesApi.deleteTemplate(id);
        alert('Èxit! Plantilla eliminada correctament.');
        window.location.href = '/plantilles-web/templates';
    } catch (error) {
        handleError('Error', new Error('No ha sigut possible eliminar la plantilla'));
    }
};

/**
 * Exporta una plantilla a JSON
 * @param {number} id - ID de la plantilla
 * @param {string} code - Codi de la plantilla per al nom del fitxer
 */
window.exportTemplate = async function (id, code) {
    try {
        const text = await templatesApi.exportTemplate(id);
        const json = JSON.parse(text);
        const content = json.item || json;

        const blob = new Blob([JSON.stringify(content, null, 2)], {
            type: 'application/json'
        });

        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${code}.json`;
        a.click();
        URL.revokeObjectURL(url);
    } catch (error) {
        handleError('Error exportant la plantilla', error);
    }
};

/**
 * Importa una plantilla des d'un fitxer JSON
 */
window.importTemplate = async function () {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];

    if (!file) {
        alert('Si us plau, selecciona un arxiu JSON');
        return;
    }

    const reader = new FileReader();
    reader.onload = async function (e) {
        try {
            const result = e.target.result;
            if (typeof result !== 'string') {
                alert('El resultat no és un string');
                return;
            }

            const templateData = JSON.parse(result);
            await templatesApi.importTemplate(templateData);

            $('#exampleModal').modal('hide');
            alert('Èxit! Plantilla importada correctament.');
            window.location.href = '/plantilles-web/templates';
        } catch (error) {
            if (error instanceof SyntaxError) {
                alert('L\'arxiu no conté un JSON vàlid');
            } else {
                handleError('Error important la plantilla', error);
            }
        }
    };
    reader.readAsText(file);
};

/**
 * Gestiona el click en el botó d'exportar
 * @param {HTMLElement} btn - Botó amb data attributes
 */
window.handleExportClick = async function (btn) {
    const id = parseInt(btn.getAttribute('data-id'), 10);
    const code = btn.getAttribute('data-code');
    await window.exportTemplate(id, code);
};

/**
 * Restaura els valors originals del formulari
 */
window.restoreOriginalValues = function () {
    const formManager = window.templateFormManager;
    if (formManager?.originalValues) {
        formManager.elements.nomPlantilla.value = formManager.originalValues.name;
        formManager.elements.codiPlantilla.value = formManager.originalValues.code;
    }
};

// Configuració del file input per mostrar el nom del fitxer seleccionat
document.addEventListener('DOMContentLoaded', () => {
    const fileInput = document.getElementById('fileInput');
    if (fileInput) {
        fileInput.addEventListener('change', function () {
            this.nextElementSibling.textContent = this.files[0]?.name || 'Tria un arxiu...';
        });
    }
});