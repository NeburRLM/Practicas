import {createFetchConfig, getCsrfConfig} from '../http-utils.js';

/**
 * Centralitza totes les crides fetch dels templates.
 * */

export const templatesApi = {
    // Elimina plantilla
    async deleteTemplate(id) {
        const config = createFetchConfig('DELETE');
        const response = await fetch(`/plantilles-web/templates/delete/${id}`, config);

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text);
        }
        return response;
    },

    // Importa variables del model
    async importVariables(modelId) {
        const config = createFetchConfig('GET');
        const response = await fetch(`/plantilles-web/templates/import-variables?modelId=${modelId}`, config);

        if (!response.ok) {
            throw new Error('Error al importar variables del model');
        }
        return response.json();
    },

    // Guarda/actualitza plantilla
    async saveTemplate(templateData, isEdit) {
        const config = createFetchConfig(isEdit ? 'PUT' : 'POST', templateData);
        const response = await fetch('/plantilles-web/templates/save2', config);

        if (!response.ok) {
            throw new Error('Error al guardar la plantilla');
        }
        return response.json();
    },

    // Importa plantilla des d'un fitxer JSON
    async importTemplate(templateData) {
        const config = createFetchConfig('POST', templateData);
        const response = await fetch('/plantilles-web/templates/import', config);

        if (!response.ok) {
            throw new Error('Error important la plantilla');
        }
        return response;
    },

    // Exporta plantilla a JSON
    async exportTemplate(id) {
        const {token, header} = getCsrfConfig();

        const config = {
            method: 'GET',
            headers: {
                [header]: token
            }
        };

        const response = await fetch(`/plantilles-web/templates/export/${id}`, config);

        if (!response.ok) {
            throw new Error('Error a l\'exportar');
        }
        return response.text();
    }
};
