
// noinspection JSUnusedGlobalSymbols
async function deleteTemplate(id) {
    if (confirm('Estàs segur que vols eliminar aquest element?')) {
        try {
            const token = document.querySelector('meta[name="_csrf"]').content;
            const header = document.querySelector('meta[name="_csrf_header"]').content;
            const config = {};
            config.headers = {};
            config.headers[header] = token;
            config.headers['content-type'] = 'application/json';
            config.method = 'POST';

            const response = await fetch('/plantilles-web/templates/delete/' + id, config);

            if (response.ok) {
                alert('Èxit! Plantilla eliminada correctament.');
                window.location.href = '/plantilles-web/templates';
            } else {
                const errorText = await response.text();
                alert('Error: ' + errorText);
            }



        } catch (error) {
            alert('Error: No ha sigut possible eliminar la plantilla');
        }

    }
}

document.addEventListener("DOMContentLoaded", () => {
    let hasUnsavedChanges = false;

    // Mapping base - índex
    const TYPES = [
        "short", "int", "long", "float", "double",
        "char", "boolean", "String", "List", "Map",
        "Data", "Html", "csv_html"
    ];

    // Funcions de mapping
    const getTypeString = (num) => TYPES[num] || 'String';
    const getTypeNumber = (str) => TYPES.indexOf(str);

    const originalName = document.getElementById('nomPlantilla').value;
    const originalCode = document.getElementById('codiPlantilla').value;


    const textarea = document.getElementById("htmlContent");

    const searchInput = document.getElementById('searchVariable');
    const listContainer = document.querySelector('.list-group ul');

    let originalVariables = [];

    function createVariableListItem(variable) {
        const li = document.createElement('li');
        li.className = 'list-group-item d-flex align-items-center';
        li.style.padding = '0.5rem';
        if (variable.id) {
            li.setAttribute('data-variable-id', variable.id);
        }
        li.innerHTML = `  
        <div class="btn-group btn-group-sm mr-1" role="group">  
            <button type="button" class="btn btn-sm btn-success mr-1 add-variable-btn" title="Afegir" style="padding: 0.25rem 0.4rem;">  
                <i class="fa fa-plus"></i>  
            </button>  
            <button type="button" class="btn btn-sm btn-primary mr-1 edit-variable-btn" title="Editar" style="padding: 0.25rem 0.4rem;"> 
                <i class="fa fa-edit"></i>  
            </button>  
            <button type="button" class="btn btn-sm btn-danger mr-1 delete-variable-btn" title="Eliminar" style="padding: 0.25rem 0.4rem;"> 
                <i class="fa fa-trash"></i>  
            </button>  
        </div>  
        <span class="variable-name flex-grow-1"  
          style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; min-width: 0; font-size: 0.80rem;"  
          title="${variable.name}">${variable.name}</span>  
        <span class="badge badge-secondary" style="font-size: 0.7rem;">${variable.type}</span>  
    `;
        return li;
    }

    // Carregar variables desde el backend si existeixen
    const existingItems = listContainer.querySelectorAll('li');
    existingItems.forEach(li => {
        const nameSpan = li.querySelector('.variable-name');
        const typeSpan = li.querySelector('.badge');
        const variableId = li.getAttribute('data-variable-id');
        if (nameSpan && typeSpan) {
            originalVariables.push({
                id: variableId ? parseInt(variableId) : null,
                name: nameSpan.textContent.trim(),
                type: typeSpan.textContent.trim()
            });
        }
    });

    const existingStyleId = document.querySelector('input[name="styleId"]')?.value;
    if (existingStyleId) {
        const styleSelect = document.getElementById('stylePlantilla');
        styleSelect.value = existingStyleId;
    }

    textarea.addEventListener('input', () => {
        hasUnsavedChanges = true;
    });


    const variableForm = document.getElementById("variableForm");
    if (variableForm) {
        variableForm.addEventListener("submit", (e) => {
            e.preventDefault();

            const name = document.getElementById('variableName').value.trim();
            const type = document.getElementById('variableType').value;

            if (!name || !type) return;

            // Afegir la variable al array
            originalVariables.push({name, type});

            // Crear el element <li>
            const li = createVariableListItem({name, type});
            listContainer.appendChild(li);

            hasUnsavedChanges = true;
            // Netejar el formulari y tancar el modal
            variableForm.reset();
            $('#variableModal').modal('hide');
        });
    }

    // Formulari de importar model
    const importModelForm = document.getElementById("importModelForm");
    if (importModelForm) {
        importModelForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const modelId = document.getElementById('modelId').value.trim();

            if (!modelId) return;

            try {
                const token = document.querySelector('meta[name="_csrf"]').content;
                const header = document.querySelector('meta[name="_csrf_header"]').content;

                const config = {};
                config.headers = {};
                config.headers[header] = token;
                config.headers['content-type'] = 'application/json';
                config.method = 'GET';

                const response = await fetch(`/plantilles-web/templates/import-variables?modelId=${modelId}`, config);

                if (!response.ok) {
                    console.error('Error al importar variables del modelo');
                    alert('Error: No ha sigut possible importar les variables del model');
                    return;
                }

                const result = await response.json();
                const importedVariables = result.item || [];


                importedVariables.forEach(variable => {
                    if (!originalVariables.some(v => v.name === variable.name)) {
                        const variableWithType = {
                            name: variable.name,
                            type: getTypeString(variable.type)
                        };

                        originalVariables.push(variableWithType);

                        const li = createVariableListItem({
                            id: variable.id,
                            name: variable.name,
                            type: getTypeString(variable.type)
                        });
                        listContainer.appendChild(li);
                    }
                });

                hasUnsavedChanges = true;
                importModelForm.reset();
                $('#importModelModal').modal('hide');
                console.log(`${importedVariables.length} variables importades correctament`);

            } catch (error) {
                console.error('Error:', error);
                alert('Error: No ha sigut possible importar les variables del model');
            }
        });
    }

    function restoreOriginalValues() {
        document.getElementById('nomPlantilla').value = originalName;
        document.getElementById('codiPlantilla').value = originalCode;
    }

    // Funció per insertar en textarea
    function insertAtCursor(textarea, text) {
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const before = textarea.value.substring(0, start);
        const after = textarea.value.substring(end);
        textarea.value = before + text + after;
        textarea.selectionStart = textarea.selectionEnd = start + text.length;
        textarea.focus();
    }

    document.querySelectorAll('.function-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const htmlText = e.currentTarget.getAttribute('data-html-text');
            const textarea = document.getElementById("htmlContent");
            insertAtCursor(textarea, htmlText);
        });
    });

    // Event delegation pels botons de la llista
    listContainer.addEventListener("click", (e) => {
        const li = e.target.closest(".list-group-item");
        if (!li) return;

        // Botó afegir
        if (e.target.closest('.add-variable-btn')) {
            const nameSpan = li.querySelector('.variable-name');
            if (nameSpan) {
                const variableName = nameSpan.textContent.trim();
                insertAtCursor(textarea, `${'${' + variableName + '}'}`);
            }
        }

        // Botó editar
        if (e.target.closest('.edit-variable-btn')) {
            const nameSpan = li.querySelector('.variable-name');
            const typeSpan = li.querySelector('.badge');

            document.getElementById('editVariableIndex').value = Array.from(listContainer.children).indexOf(li);
            document.getElementById('editVariableName').value = nameSpan.textContent.trim();
            document.getElementById('editVariableType').value = typeSpan.textContent.trim();

            $('#editVariableModal').modal('show');
        }

        // Botó eliminar
        if (e.target.closest('.delete-variable-btn')) {
            const index = Array.from(listContainer.children).indexOf(li);
            originalVariables.splice(index, 1);
            li.remove();
            hasUnsavedChanges = true;
        }
    });

    // Handler pel formulari de edició de variables
    const editVariableForm = document.getElementById("editVariableForm");
    if (editVariableForm) {
        editVariableForm.addEventListener("submit", (e) => {
            e.preventDefault();

            const index = parseInt(document.getElementById('editVariableIndex').value);
            const newName = document.getElementById('editVariableName').value.trim();
            const newType = document.getElementById('editVariableType').value;

            if (!newName || !newType) return;

            // Actualitzar al array
            originalVariables[index] = {
                ...originalVariables[index],
                name: newName,
                type: newType
            };

            // Actualitzar al DOM
            const li = listContainer.children[index];
            li.querySelector('.variable-name').textContent = newName;
            li.querySelector('.badge').textContent = newType;

            hasUnsavedChanges = true;
            $('#editVariableModal').modal('hide');
        });
    }

    // Filtrar dinàmic del buscador de variables
    searchInput.addEventListener('input', (e) => {
        const text = e.target.value.trim().toLowerCase();
        console.log("Text actual del buscador:", text);

        // Netejar llista abans de mostrar coincidncies
        listContainer.innerHTML = '';

        // Filtrar per coincidencia
        const matched = text === ""
            ? originalVariables
            : originalVariables.filter(v => v.name.toLowerCase().includes(text));

        console.log("Variables que coincideixenn:", matched);

        // Construcció <li> amb badge de tipus
        matched.forEach(v => {
            const li = createVariableListItem(v);
            listContainer.appendChild(li);
        });

        // Si ja existeix la variable a la llista
        if (originalVariables.some(v => v.name.toLowerCase() === text)) {
            console.log(`¡La variable "${text}" ja existeix a la llista!`);
        }
    });

    window.addEventListener('beforeunload', (e) => {
        if (hasUnsavedChanges) {
            e.preventDefault();
        }
    });

    const styleForm = document.getElementById("styleForm");
    const saveForm = document.getElementById("saveForm");

    if (styleForm) {
        styleForm.addEventListener("submit", (e) => submitStyleForm(e));
    }
    if (saveForm) {
        saveForm.addEventListener("submit", (e) => submitSaveForm(e));
    }
    const configForm = document.getElementById("configForm");

    if (configForm) {
        configForm.addEventListener("submit", (e) => submitConfigForm(e));
    }


    function submitStyleForm(e) {
        e.preventDefault();

        const selectedStyle = document.getElementById('stylePlantilla').value;

        if (selectedStyle && selectedStyle.trim() !== "") {
            document.getElementById('stylePlantillaHidden').value = selectedStyle;
        } else {
            document.getElementById('stylePlantillaHidden').value = null;
        }

        $('#styleModal').modal('hide');
    }

    async function submitSaveForm(e) {
        e.preventDefault();

        const nom = document.getElementById('nomPlantilla').value.trim();
        const code = document.getElementById('codiPlantilla').value.trim();

        if (!nom || !code) return;

        const templateId = document.querySelector('input[name="id"]').value;
        const isEdit = templateId && templateId !== '1';

        // Obtenir ID box en mode edició
        let existingBoxId = null;
        let existingBoxType = null;
        if (isEdit) {
            const boxIdInput = document.querySelector('input[name="boxId"]');
            const boxTypeInput = document.querySelector('input[name="boxType"]');

            if (boxIdInput && boxIdInput.value !== 'null') {
                existingBoxId = parseInt(boxIdInput.value);
            }
            if (boxTypeInput && boxTypeInput.value !== 'null') {
                existingBoxType = parseInt(boxTypeInput.value);
            }
        }

        const width = document.getElementById('configAmplada')?.value || 210;
        const height = document.getElementById('configAllargada')?.value || 297;
        const marginTop = document.getElementById('configMargeSuperior')?.value || 18;
        const marginBottom = document.getElementById('configMargeInferior')?.value || 18;
        const marginLeft = document.getElementById('configMargeEsquerre')?.value || 14;
        const marginRight = document.getElementById('configMargeDret')?.value || 14;

        const styleId = document.getElementById('stylePlantilla').value;

        const htmlContent = document.getElementById('htmlContent').value;

        const variables = originalVariables.map((v) => {
            const mapped = {
                name: v.name,
                type: getTypeNumber(v.type)
            };

            // ID només mode edició
            if (v.id) {
                mapped.id = v.id;
            }

            return mapped;
        });

        const templateData = {
            name: nom,
            code: code,
            parentFolder: 1,
            maxColPosition: 23,
            maxRowPosition: 33,
            pageDimensions: {
                width: parseInt(width),
                height: parseInt(height),
                marginTop: parseInt(marginTop),
                marginBottom: parseInt(marginBottom),
                marginLeft: parseInt(marginLeft),
                marginRight: parseInt(marginRight)
            },
            boxes: [{
                id: existingBoxId,
                boxType: existingBoxType,
                rowPosition: 1,
                colPosition: 1,
                height: 28,
                width: 23,
                innerHtml: htmlContent,
                contentConfiguration: {
                    gspCode: "${raw(text)}",
                    variables: []
                }
            }],
            variables: variables
        };

        if (isEdit) {
            templateData.id = parseInt(templateId);
        }

        if (styleId) {
            templateData.style = parseInt(styleId);
        }

        try {
            const token = document.querySelector('meta[name="_csrf"]').content;
            const header = document.querySelector('meta[name="_csrf_header"]').content;
            const config = {};
            config.headers = {};
            config.headers[header] = token;
            config.headers['content-type'] = 'application/json';
            config.method = isEdit ? 'PUT' : 'POST';
            config.body = JSON.stringify(templateData);

            const response = await fetch('/plantilles-web/templates/save2', config);

            if (!response.ok) {
                console.error('Error al guardar la plantilla');
                alert('Error: No ha sigut possible guardar la plantilla correctament');
                return;
            }

            const result = await response.json();

            if (result.item && result.item.id) {
                alert('Èxit: La plantilla s\'ha guardat correctament');
                hasUnsavedChanges = false;
                $('#saveModal').modal('hide');

                // URL de redirecció
                const redirectUrl = '/plantilles-web/templates/edit/' + result.item.id;

                window.location.replace(redirectUrl);

            }
        } catch (error) {
            console.error('Error:', error);
            alert('Error: No ha sigut possible guardar la plantilla correctament');
        }
    }



    function submitConfigForm(e) {
        e.preventDefault();

        const nom = document.getElementById('configNom').value.trim();
        const codi = document.getElementById('configCodi').value.trim();
        const amplada = document.getElementById('configAmplada').value;
        const allargada = document.getElementById('configAllargada').value;
        const margeSuperior = document.getElementById('configMargeSuperior').value;
        const margeInferior = document.getElementById('configMargeInferior').value;
        const margeEsquerre = document.getElementById('configMargeEsquerre').value;
        const margeDret = document.getElementById('configMargeDret').value;

        if (!nom || !codi) return;
        const nomPlantillaField = document.getElementById('nomPlantilla');
        const codiPlantillaField = document.getElementById('codiPlantilla');
        document.getElementById('nomPlantilla').value = nom;
        document.getElementById('codiPlantilla').value = codi;

        nomPlantillaField.dispatchEvent(new Event('input', {bubbles: true}));
        codiPlantillaField.dispatchEvent(new Event('input', {bubbles: true}));

        const mainForm = document.getElementById('editorForm');

        // Afegir camps ocults al formulari principal
        const fields = [
            {name: 'configNom', value: nom},
            {name: 'configCodi', value: codi},
            {name: 'configAmplada', value: amplada},
            {name: 'configAllargada', value: allargada},
            {name: 'configMargeSuperior', value: margeSuperior},
            {name: 'configMargeInferior', value: margeInferior},
            {name: 'configMargeEsquerre', value: margeEsquerre},
            {name: 'configMargeDret', value: margeDret}
        ];

        fields.forEach(field => {
            const hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = field.name;
            hiddenInput.value = field.value;
            mainForm.appendChild(hiddenInput);
        });

        $('#configModal').modal('hide');

        console.log('Configuració guardada:', {nom, codi, amplada, allargada});
    }

    // Reset formularis al tancar modals
    $('#saveModal').on('hidden.bs.modal', function () {
        document.getElementById('saveForm').reset();
    });

    $('#configModal').on('hidden.bs.modal', function () {
        document.getElementById('configForm').reset();
    });
});
