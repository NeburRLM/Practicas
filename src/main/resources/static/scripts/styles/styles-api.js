import { createFetchConfig } from '../http-utils.js';

export const stylesApi = {
    // Elimina estil
    async deleteStyle(id) {
        const config = createFetchConfig('DELETE');
        const response = await fetch(`/plantilles-web/styles/delete/${id}`, config);

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text);
        }
        return response;
    },

    // Carrega els ens locals disponibles
    async loadEnsLocals() {
        const config = createFetchConfig('GET');
        const response = await fetch('/plantilles-web/styles/ensLocals/list', config);

        if (!response.ok) {
            throw new Error('Error carregant ensLocals');
        }
        return response.json();
    },

    // Guarda/actualitza estil
    async saveStyle(styleData, isEdit) {
        const config = createFetchConfig(isEdit ? 'PUT' : 'POST', styleData);
        const response = await fetch('/plantilles-web/styles/save2', config);

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text);
        }
        return response.json();
    }
};