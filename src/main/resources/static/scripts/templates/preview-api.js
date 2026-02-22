import {createFetchConfig, getCsrfConfig} from '../http-utils.js';

export const previewApi = {
    async validateHtml(template) {
        const config = createFetchConfig('POST', template);
        const response = await fetch('/plantilles-web/templates/validate-html', config);
        if (!response.ok) throw new Error('Error validant HTML');
        return response.json();
    },

    // Genera prova de la plantilla
    async processTemplate(template) {
        const config = createFetchConfig('POST', template);
        const response = await fetch('/plantilles-web/templates/process-template', config);

        if (!response.ok) {
            throw new Error('Error generant la prova');
        }
        return response.json();
    },

    // Carrega el CSS de l'estil
    async loadStyleCSS(styleId) {
        const {token, header} = getCsrfConfig();

        const config = {
            method: 'GET',
            headers: {
                [header]: token
            }
        };

        const response = await fetch(`/plantilles-web/styles/css/${styleId}`, config);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.text();
    },

    // Genera PDF a partir de la prova
    async generatePdf(templateId, processedTemplate) {
        const config = createFetchConfig('POST', processedTemplate);
        const response = await fetch(`/plantilles-web/templates/getPdfPreview/${templateId}`, config);

        if (!response.ok) {
            throw new Error('Error generant PDF');
        }
        return response.blob();
    }
};