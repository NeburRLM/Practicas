import {createFetchConfig} from '../http-utils.js';

export const attachmentsApi = {
    // Elimina l'adjunt
    async deleteAttachment(id) {
        const config = createFetchConfig('DELETE');
        const response = await fetch(`/plantilles-web/attachments/delete/${id}`, config);
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text);
        }
        return response;
    },

    // Guarda/actualitza l'adjunt
    async saveAttachment(formData, isEdit) {
        const config = createFetchConfig(isEdit ? 'PUT' : 'POST', formData, true);
        const response = await fetch('/plantilles-web/attachments/save2', config);
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text);
        }
        return response.json();
    }
};
