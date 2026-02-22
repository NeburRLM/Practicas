import {previewApi} from './preview-api.js';
import {handleError} from '../http-utils.js';

// Estat global
let processedTemplate = null;

// Constants
const A4_WIDTH_PX = 794;
const A4_HEIGHT_PX = 1123;
const PADDING_TOP_MM = 18;
const PADDING_LEFT_MM = 14;

// Controlar aplicació de CSS després de mostrar la prova de la plantilla generada
const cssUtils = {
    scopeCSSToSelector(css, selector) {
        const rules = css.split('}');
        const scopedRules = [];

        rules.forEach(rule => {
            const trimmedRule = rule.trim();
            if (!trimmedRule) return;

            const declarationStart = trimmedRule.indexOf('{');
            if (declarationStart === -1) return;

            const selectorsText = trimmedRule.substring(0, declarationStart).trim();
            const declarations = trimmedRule.substring(declarationStart);

            const selectors = selectorsText.split(',').map(s => s.trim());

            const scopedSelectors = selectors.map(sel => {
                if (sel.startsWith('@') || sel.includes(selector)) {
                    return sel;
                }
                return `${selector} ${sel}`;
            }).join(', ');

            scopedRules.push(`${scopedSelectors} ${declarations}}`);
        });

        return scopedRules.join('\n');
    },

    // Carregar estils de la plantilla seleccionada
    async loadCSS(styleId) {
        if (!styleId) return;

        const existingStyle = document.getElementById('template-style');
        if (existingStyle) {
            existingStyle.remove();
        }

        try {
            const cssText = await previewApi.loadStyleCSS(styleId);
            const scopedCSS = this.scopeCSSToSelector(cssText, '.a4-content');

            const style = document.createElement('style');
            style.id = 'template-style';
            style.textContent = scopedCSS;
            document.head.appendChild(style);

            console.log('Estils carregats i aplicats');
        } catch (error) {
            console.error('Error carregant estils:', error);
        }
    }
};

// Renderització dinàmica de l'estat
const renderUtils = {
    showLoading(container, message = 'Generant prova...') {
        container.innerHTML = `
            <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                 style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                <div class="d-flex flex-column align-items-center justify-content-center h-100 text-success">
                    <i class="fa fa-spinner fa-spin mb-3" style="font-size: 5rem;"></i>
                    <span>${message}</span>
                </div>
            </div>
        `;
    },

    showError(container, message = 'Error generant la prova') {
        container.innerHTML = `
            <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                 style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                <div class="d-flex flex-column align-items-center justify-content-center h-100 text-danger">
                    <i class="fa fa-exclamation-triangle mb-3" style="font-size: 5rem;"></i>
                    <span>${message}</span>
                </div>
            </div>
        `;
    },

    showEmpty(container) {
        container.innerHTML = `
            <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                 style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                <div class="d-flex flex-column align-items-center justify-content-center h-100 text-muted">
                    <i class="fa fa-file-text-o mb-3" style="font-size: 5rem; opacity: 0.3;"></i>
                    <span>No hi ha contingut per mostrar</span>
                </div>
            </div>
        `;
    },

    calculateScale(containerWidth) {
        const availableWidth = containerWidth - 40; // Restar padding
        return Math.min(1, availableWidth / A4_WIDTH_PX);
    },

    decodeHtml(encodedHtml) {
        const textarea = document.createElement('textarea');
        textarea.innerHTML = encodedHtml;
        return textarea.value;
    },

    extractHtmlContent(boxes) {
        let htmlContent = '';
        boxes.forEach(box => {
            if (box.innerHtml) {
                htmlContent += this.decodeHtml(box.innerHtml);
            }
        });
        return htmlContent;
    },

    paginateContent(htmlContent, container) {
        const scale = this.calculateScale(container.clientWidth);
        const scaledWidth = A4_WIDTH_PX * scale;
        const scaledHeight = A4_HEIGHT_PX * scale;

        container.innerHTML = `
            <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                 style="width: ${scaledWidth}px; min-height: ${scaledHeight}px; border: 1px solid #e7eaec; transform-origin: top center;">
                <div class="a4-content" style="padding: ${PADDING_TOP_MM * scale}mm ${PADDING_LEFT_MM * scale}mm; width: 100%; box-sizing: border-box; transform: scale(${scale}); transform-origin: top left;">
                    ${htmlContent}
                </div>
            </div>
        `;

        container.scrollTop = 0;
    }
};

// Validació de variables
const validationUtils = {
    validateVariables() {
        let hasEmptyFields = false;

        document.querySelectorAll('.variable-input').forEach(input => {
            const value = input.value.trim();

            if (!value) {
                input.classList.add('is-invalid');
                hasEmptyFields = true;
            } else {
                input.classList.remove('is-invalid');
            }
        });

        return !hasEmptyFields;
    },

    updateTemplateVariables(template) {
        document.querySelectorAll('.variable-input').forEach(element => {
            const variableId = element.getAttribute('data-variable-id');
            const variableType = element.getAttribute('data-variable-type');
            let value = element.value;

            const variable = template.variables.find(v => v.id === parseInt(variableId, 10));
            if (variable) {
                // Gestió salts de linea de \r\n
                if (variableType !== '9' && typeof value === 'string') {
                    value = value.replace(/\r\n/g, '\n')
                    .replace(/\r/g, '\n')
                    .replace(/\n/g, '\r\n');
                }
                variable.value = value;
            }
        });
    }
};

// Carrega errors html al modal de la vista
const htmlValidationUtils = {
    showErrorModal(data) {
        const errorBtn = document.getElementById('htmlErrorsBtn');
        const modal = document.getElementById('htmlErrorsModal');
        const content = document.getElementById('htmlErrorsContent');

        const errors = data.errors || [];

        if (errors.length > 0) {
            errorBtn.style.display = 'inline-block';

            let html = `  
                <ul class="list-unstyled">  
            `;

            errors.forEach((error, index) => {
                html += `  
                    <li class="mb-3 p-3 border-left border-danger bg-light">  
                        <strong>${index + 1}. Error:</strong> ${error.message}  
                        <br><small class="text-muted">${error.location}</small>  
                        ${error.code ? `<br><code class="d-block mt-2">${error.code}</code>` : ''}  
                    </li>  
                `;
            });

            html += '</ul>';
            content.innerHTML = html;

            errorBtn.onclick = () => $(modal).modal('show');
        } else {
            errorBtn.style.display = 'none';
        }
    }
};

// Classe principal de gestió de preview
class TemplatePreviewManager {
    constructor() {
        this.template = window.currentTemplate;
        this.templateId = window.currentTemplateId;
        this.container = document.getElementById('pdfViewerContainer');
        this.setupEventListeners();
        this.validateTemplateHTML();
    }

    // Crida a la funció del servei per a retornar els errors del innerHtml del template
    validateTemplateHTML() {
        if (!this.template?.boxes) return;

        previewApi.validateHtml(this.template)
            .then(data => {
                htmlValidationUtils.showErrorModal(data);
            })
            .catch(error => {
                console.error('Error validant HTML:', error);
            });
    }

    setupEventListeners() {
        window.addEventListener('resize', () => {
            if (processedTemplate?.boxes?.length > 0) {
                const htmlContent = renderUtils.extractHtmlContent(processedTemplate.boxes);
                renderUtils.paginateContent(htmlContent, this.container);
            }
        });
    }

    async generateProva() {
        if (!validationUtils.validateVariables()) {
            alert('Cal omplir tots els valors de les variables abans de generar la prova.');
            return;
        }

        const templateCopy = JSON.parse(JSON.stringify(this.template));
        validationUtils.updateTemplateVariables(templateCopy);

        renderUtils.showLoading(this.container);

        try {
            const data = await previewApi.processTemplate(templateCopy);

            if (data?.item) {
                await this.renderProcessedTemplate(data.item);

                            // DESPUÉS hacer la copia profunda
                            processedTemplate = JSON.parse(JSON.stringify(data.item));
                            console.log('Prova processada (después de renderizar):', processedTemplate);
            }
        } catch (error) {
            console.error('Error:', error);
            handleError('Error generant la prova', error);
            renderUtils.showError(this.container);
        }
    }

    async renderProcessedTemplate(template) {
        if (!template.boxes?.length) {
            renderUtils.showEmpty(this.container);
            return;
        }

        const htmlContent = renderUtils.extractHtmlContent(template.boxes);
        renderUtils.paginateContent(htmlContent, this.container);

        if (template.style) {
            await cssUtils.loadCSS(template.style);
        }
    }

    async generatePdf() {
        if (!processedTemplate) {
            alert('No hi ha template processat per generar el PDF');
            return;
        }

        renderUtils.showLoading(this.container, 'Generant PDF...');

        try {
            // Copia processTemplate per normalitzar els valors de les variables per defecte
            const templateForPdf = JSON.parse(JSON.stringify(processedTemplate));

            // Convertir <br/> a \r\n en totes les variables
            if (templateForPdf.variables && Array.isArray(templateForPdf.variables)) {
                templateForPdf.variables.forEach(variable => {
                    if (variable.value && typeof variable.value === 'string') {

                        variable.value = variable.value
                            .replace(/<br\s*\/?>/gi, '\r\n')  // <br>, <br/>, <BR>, etc.
                            .replace(/<br>/gi, '\r\n');        // <br> sin /
                    }
                });
            }

            console.log('Generant PDF amb template (variables normalitzades):', templateForPdf);

            const blob = await previewApi.generatePdf(this.templateId, templateForPdf);
            const url = window.URL.createObjectURL(blob);

            this.addDownloadButton(url);
            this.renderPdfViewer(url);
        } catch (error) {
            console.error('Error:', error);
            handleError('Error generant el PDF', error);
            renderUtils.showError(this.container, 'Error generant el PDF');
        }
    }

    addDownloadButton(url) {
        const iboxTitle = document.querySelector('.ibox-title');
        const existingBtn = iboxTitle.querySelector('.pull-right');

        if (existingBtn) {
            existingBtn.remove();
        }

        const downloadBtn = document.createElement('button');
        downloadBtn.className = 'btn btn-sm btn-default';
        downloadBtn.id = 'download-pdf-btn';
        downloadBtn.style.cssText = 'position: absolute; right: 2%; top: 50%; transform: translateY(-50%);';
        downloadBtn.innerHTML = '<i class="fa fa-download"></i>';

        iboxTitle.appendChild(downloadBtn);

        downloadBtn.addEventListener('click', () => {
            window.downloadPdf(url, processedTemplate.name || 'document');
        });
    }

    renderPdfViewer(url) {
        this.container.innerHTML = `
            <div style="display: flex; justify-content: center; width: 100%;">
                <div class="bg-white shadow-sm mb-4 position-relative"
                     style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                    <object data="${url}#zoom=85"
                            type="application/pdf"
                            width="100%"
                            height="100%"
                            style="min-height: 297mm;">
                        <iframe src="${url}#zoom=85"
                                width="100%"
                                height="100%"
                                style="border: none; min-height: 297mm;">
                            <p>El teu navegador no suporta visualització de PDF.</p>
                        </iframe>
                    </object>
                </div>
            </div>
        `;
    }

    importValors() {
        const fileInput = document.getElementById('fileInputValors');
        const file = fileInput.files[0];

        if (!file) {
            alert('Cal especificar el fitxer que conté els valors');
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const result = e.target.result;
                if (typeof result !== 'string') {
                    alert('El resultat no és un string vàlid');
                    return;
                }

                const jsonData = JSON.parse(result);
                const valorsData = jsonData.items || jsonData;

                let importedCount = 0;
                Object.keys(valorsData).forEach(variableName => {
                    const input = document.querySelector(`input[data-variable-name="${variableName}"]`);
                    const textarea = document.querySelector(`textarea[data-variable-name="${variableName}"]`);
                    const element = input || textarea;

                    if (element) {
                        const variableType = element.getAttribute('data-variable-type');
                        let value = valorsData[variableName];

                        if (value === null || value === undefined) {  // Saltar variables amb valor null o undefined per poder fer la previsualització
                            return;
                        }

                        // Transformació especial per tipus MAP (tipus 9)
                        if (variableType === '9') {  // Comparar amb string '9', no 'Map'
                            value = JSON.stringify(value);
                            value = value.replace(/{/g, '[');
                            value = value.replace(/}/g, ']');
                            if (value === "[]") {
                                value = "[:]";
                            }
                        }

                        // Gestió de salts de línia de \r\n
                        if (typeof value === 'string') {
                            value = value.replace(/\r\n/g, '\n')
                            .replace(/\r/g, '\n')
                            .replace(/\n/g, '\r\n');
                        }

                        element.value = value;
                        element.classList.remove('is-invalid');
                        importedCount++;
                    }
                });

                alert(`S'han importat ${importedCount} valors correctament`);
            } catch (error) {
                if (error instanceof SyntaxError) {
                    alert('El fitxer no conté un JSON vàlid');
                } else {
                    handleError('Error important valors', error);
                }
            }
        };
        reader.readAsText(file);
    }
}

// Funcions globals (accessibles des d'HTML)
let previewManager;

window.generateProva = async function () {
    if (!previewManager) {
        previewManager = new TemplatePreviewManager();
    }
    await previewManager.generateProva();
};

window.generatePdf = async function () {
    if (!previewManager) {
        previewManager = new TemplatePreviewManager();
    }
    await previewManager.generatePdf();
};

window.importValors = function () {
    if (!previewManager) {
        previewManager = new TemplatePreviewManager();
    }
    previewManager.importValors();
};

window.updateFileLabel = function (input) {
    document.getElementById('fileName').textContent = input.files[0]?.name || 'Cap arxiu seleccionat';
};

window.downloadPdf = function (url, filename) {
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
};

// Inicialització
document.addEventListener('DOMContentLoaded', () => {
    previewManager = new TemplatePreviewManager();
});