let processedTemplate = null;

window.addEventListener('resize', () => {
    if (processedTemplate && processedTemplate.boxes && processedTemplate.boxes.length > 0) {
        let htmlContent = '';
        processedTemplate.boxes.forEach(box => {
            if (box.innerHtml) {
                const textarea = document.createElement('textarea');
                textarea.innerHTML = box.innerHtml;
                const decodedHtml = textarea.value;
                htmlContent += decodedHtml;
            }
        });
        paginateContent(htmlContent, document.getElementById('pdfViewerContainer'));
    }
});

function generateProva() {
    let template = JSON.parse(JSON.stringify(window.currentTemplate));

    // Actualitzar els valors de les variables
    let hasEmptyFields = false;
    document.querySelectorAll('.variable-input').forEach(input => {
        const variableId = input.getAttribute('data-variable-id');
        const value = input.value.trim();

        if (!value) {
            input.classList.add('is-invalid');
            hasEmptyFields = true;
        } else {
            input.classList.remove('is-invalid');
            const variable = template.variables.find(v => v.id === parseInt(variableId, 10));
            if (variable) {
                variable.value = value;
            }
        }
    });

    if (hasEmptyFields) {
        alert('Cal omplir tots els valors de les variables abans de generar la prova.');
        return;
    }

    // Mostrar loading
    const viewerContainer = document.getElementById('pdfViewerContainer');
    viewerContainer.innerHTML = `
                <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                     style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                    <div class="d-flex flex-column align-items-center justify-content-center h-100 text-success">
                        <i class="fa fa-spinner fa-spin mb-3" style="font-size: 5rem;"></i>
                        <span>Generant prova...</span>
                    </div>
                </div>
            `;

    // Obtenir token CSRF
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    const config = {};
    config.headers = {};
    config.headers[header] = token;
    config.headers['content-type'] = 'application/json';
    config.method = 'POST';
    config.body = JSON.stringify(template);

    // Enviar al endpoint /process-template
    fetch('/plantilles-web/templates/process-template', config)
        .then(response => response.json())
        .then(async data => {
            if (data && data.item) {
                processedTemplate = data.item;
                console.log('Prova procesada:', processedTemplate);
                await renderProcessedTemplate(data.item);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error generant la prova');
            viewerContainer.innerHTML = `
                        <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                             style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                            <div class="d-flex flex-column align-items-center justify-content-center h-100 text-danger">
                                <i class="fa fa-exclamation-triangle mb-3" style="font-size: 5rem;"></i>
                                <span>Error generant la prova</span>
                            </div>
                        </div>
                    `;
        });
}

function loadCSS(styleId) {
    if (!styleId) return Promise.resolve();

    const existingStyle = document.getElementById('template-style');
    if (existingStyle) {
        existingStyle.remove();
    }

    // Obtenir token CSRF
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const config = {};
    config.headers = {};
    config.headers[header] = token;
    config.method = 'GET';

    return fetch(`/plantilles-web/styles/css/${styleId}`, config)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.text();
        })
        .then(cssText => {
            const scopedCSS = scopeCSSToSelector(cssText, '.a4-content');

            const style = document.createElement('style');
            style.id = 'template-style';
            style.textContent = scopedCSS;
            document.head.appendChild(style);

            console.log('Estils carregats y aplicats');
        })
        .catch(error => {
            console.error('Error cargant estils:', error);
        });
}

function scopeCSSToSelector(css, selector) {
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
            if (sel.startsWith('@')) {
                return sel;
            }
            if (sel.includes(selector)) {
                return sel;
            }
            return `${selector} ${sel}`;
        }).join(', ');

        scopedRules.push(`${scopedSelectors} ${declarations}}`);
    });

    return scopedRules.join('\n');
}

async function renderProcessedTemplate(processedTemplate) {
    const viewerContainer = document.getElementById('pdfViewerContainer');

    if (processedTemplate.boxes && processedTemplate.boxes.length > 0) {
        let htmlContent = '';
        processedTemplate.boxes.forEach(box => {
            if (box.innerHtml) {
                const textarea = document.createElement('textarea');
                textarea.innerHTML = box.innerHtml;
                const decodedHtml = textarea.value;
                htmlContent += decodedHtml;
            }
        });

        // Dividir contingut en pàgines A4
        paginateContent(htmlContent, viewerContainer);

        if (processedTemplate.style) {
            await loadCSS(processedTemplate.style);
        }
    } else {
        viewerContainer.innerHTML = `
                    <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                         style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                        <div class="d-flex flex-column align-items-center justify-content-center h-100 text-muted">
                            <i class="fa fa-file-text-o mb-3" style="font-size: 5rem; opacity: 0.3;"></i>
                            <span>No hi ha contingut per mostrar</span>
                        </div>
                    </div>
                `;
    }
}

function paginateContent(htmlContent, container) {
    // Calcular escala responsive
    const containerWidth = container.clientWidth - 40; // Restar padding
    const a4Width = 794; // 210mm en px a 96 DPI
    const scale = Math.min(1, containerWidth / a4Width);
    const scaledWidth = a4Width * scale;
    const scaledHeight = 1123 * scale; // 297mm en px a 96 DPI

    container.innerHTML = `
        <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
             style="width: ${scaledWidth}px; min-height: ${scaledHeight}px; border: 1px solid #e7eaec; transform-origin: top center;">
            <div class="a4-content" style="padding: ${18 * scale}mm ${14 * scale}mm; width: 100%; box-sizing: border-box; transform: scale(${scale}); transform-origin: top left;">
                ${htmlContent}
            </div>
        </div>
    `;

    container.scrollTop = 0;
}

function updateFileLabel(input) {
    document.getElementById('fileName').textContent = input.files[0]?.name || 'Cap arxiu seleccionat';
}

function importValors() {
    const fileInput = document.getElementById('fileInputValors');
    const file = fileInput.files[0];

    if (!file) {
        alert('Cal especificar el fitxer que conté els valors');
        return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
        try {
            const result = e.target.result;
            if (typeof result === 'string') {
                const jsonData = JSON.parse(result);
                const valorsData = jsonData.items || jsonData;

                let importedCount = 0;
                Object.keys(valorsData).forEach(variableName => {
                    const input = document.querySelector(`input[data-variable-name="${variableName}"]`);
                    if (input) {
                        input.value = valorsData[variableName];
                        input.classList.remove('is-invalid');
                        importedCount++;
                    }
                });
                alert(`S'han importat ${importedCount} valors correctament`);
            } else {
                alert('El resultat no es un string válido');
            }
        } catch (error) {
            console.error('Error parsing JSON:', error);
            alert('El fitxer no conté un JSON vàlid');
        }
    };
    reader.readAsText(file);
}

function generatePdf() {
    if (!processedTemplate) {
        alert('No hi ha template processat per generar el PDF');
        return;
    }

    const templateId = window.currentTemplateId;
    const viewerContainer = document.getElementById('pdfViewerContainer');
    const iboxTitle = document.querySelector('.ibox-title');

    // Mostrar indicador de càrrega
    viewerContainer.innerHTML = `
        <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
             style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
            <div class="d-flex flex-column align-items-center justify-content-center h-100 text-success">
                <i class="fa fa-spinner fa-spin mb-3" style="font-size: 5rem;"></i>
                <span>Generant PDF...</span>
            </div>
        </div>
    `;

    // Obtener token CSRF
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const config = {};
    config.headers = {};
    config.headers[header] = token;
    config.headers['content-type'] = 'application/json';
    config.method = 'POST';
    config.body = JSON.stringify(processedTemplate);

    fetch(`/plantilles-web/templates/getPdfPreview/${templateId}`, config)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error generando PDF');
            }
            return response.blob();
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            //viewerContainer.style.overflow = 'hidden';
            // Añadir botón de descarga al ibox-title
            const downloadBtn = document.createElement('div');
            downloadBtn.className = 'pull-right';
            downloadBtn.innerHTML = `
            <button class="btn btn-sm btn-default" onclick="downloadPdf('${url}', '${processedTemplate.name || 'document'}')">
                <i class="fa fa-download"></i>
            </button>
        `;
            iboxTitle.appendChild(downloadBtn);

            // Mostrar PDF render
            viewerContainer.innerHTML = `
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
                            <p>Tu navegador no soporta visualización de PDF.</p>
                        </iframe>
                    </object>
                </div>
            </div>
            `;
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error generant el PDF');
            viewerContainer.innerHTML = `
            <div class="bg-white shadow-sm mx-auto mb-4 position-relative"
                 style="width: 210mm; height: 297mm; border: 1px solid #e7eaec;">
                <div class="d-flex flex-column align-items-center justify-content-center h-100 text-danger">
                    <i class="fa fa-exclamation-triangle mb-3" style="font-size: 5rem;"></i>
                    <span>Error generant el PDF</span>
                </div>
            </div>
        `;
        });
}

// Funció auxiliar per descarregar el PDF
function downloadPdf(url, filename) {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename + '.pdf';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}