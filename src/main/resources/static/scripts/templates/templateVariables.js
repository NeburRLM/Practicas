import {handleError} from '../http-utils.js';

/**
 * Mòdul que gestiona el component de les variables de la plantilla.
 * */


export async function wireTemplateVariables(state, templatesApi) {
    loadExistingVariablesFromDom(state);
    wireVariableForm(state);
    wireEditVariableForm(state);
    wireVariableListActions(state);
    wireSearchFilter(state);
    await wireImportModelForm(state, templatesApi);
}

// Crea un element <li> per una variable d'una llista de variables
function createVariableListItem(variable) {
    const li = document.createElement('li');
    li.className = 'list-group-item d-flex align-items-center';
    li.style.padding = '0.5rem';

    if (variable.id) li.setAttribute('data-variable-id', variable.id);

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

// Carrega les variables existents del DOM en l'estat de la plantilla, per a poder editar-les
function loadExistingVariablesFromDom(state) {
    const existingItems = state.els.listContainer?.querySelectorAll('li') || [];
    existingItems.forEach(li => {
        const nameSpan = li.querySelector('.variable-name');
        const typeSpan = li.querySelector('.badge');
        const variableId = li.getAttribute('data-variable-id');

        if (!nameSpan || !typeSpan) return;

        state.variables.push({
            id: variableId ? parseInt(variableId, 10) : null,
            name: nameSpan.textContent.trim(),
            type: typeSpan.textContent.trim()
        });
    });
}

// Formulari per crear una nova variable
function wireVariableForm(state) {
    state.els.variableForm?.addEventListener('submit', (e) => {
        e.preventDefault();

        const name = document.getElementById('variableName')?.value.trim();
        const type = document.getElementById('variableType')?.value;

        if (!name || !type) return;

        state.variables.push({name, type});
        state.els.listContainer.appendChild(createVariableListItem({name, type}));

        state.markDirty();
        state.els.variableForm.reset();
        $('#variableModal').modal('hide');
    });
}

// Formulari per importar variables d'un model existent
function wireImportModelForm(state, templatesApi) {
    state.els.importModelForm?.addEventListener('submit', async (e) => {
        e.preventDefault();

        const modelId = document.getElementById('modelId')?.value.trim();
        if (!modelId) return;

        try {
            const result = await templatesApi.importVariables(modelId);
            const imported = result.item || [];

            imported.forEach(v => {
                if (state.variables.some(x => x.name === v.name)) return;

                const varWithType = {name: v.name, type: state.typeUtils.getString(v.type)};
                state.variables.push(varWithType);
                state.els.listContainer.appendChild(createVariableListItem(varWithType));
            });

            state.markDirty();
            state.els.importModelForm.reset();
            $('#importModelModal').modal('hide');
        } catch (err) {
            handleError('Error', new Error('No ha sigut possible importar les variables del model'));
        }
    });
}

// Formulari per editar una variable existent
function wireEditVariableForm(state) {
    state.els.editVariableForm?.addEventListener('submit', (e) => {
        e.preventDefault();

        const index = parseInt(document.getElementById('editVariableIndex')?.value, 10);
        const newName = document.getElementById('editVariableName')?.value.trim();
        const newType = document.getElementById('editVariableType')?.value;

        if (!newName || !newType || Number.isNaN(index)) return;

        state.variables[index] = {...state.variables[index], name: newName, type: newType};

        const li = state.els.listContainer.children[index];
        li.querySelector('.variable-name').textContent = newName;
        li.querySelector('.badge').textContent = newType;

        state.markDirty();
        $('#editVariableModal').modal('hide');
    });
}

// Click delegation sobre la llista per crear, editar o eliminar variables
function wireVariableListActions(state) {
    state.els.listContainer?.addEventListener('click', (e) => {
        const li = e.target.closest('.list-group-item');
        if (!li) return;

        if (e.target.closest('.add-variable-btn')) {
            const nameSpan = li.querySelector('.variable-name');
            const variableName = nameSpan?.textContent.trim();
            if (!variableName) return;

            insertAtCursor(state.els.htmlContent, `\${${variableName}}`);
            return;
        }

        if (e.target.closest('.edit-variable-btn')) {
            const nameSpan = li.querySelector('.variable-name');
            const typeSpan = li.querySelector('.badge');

            document.getElementById('editVariableIndex').value = Array.from(state.els.listContainer.children).indexOf(li);
            document.getElementById('editVariableName').value = nameSpan.textContent.trim();
            document.getElementById('editVariableType').value = typeSpan.textContent.trim();

            $('#editVariableModal').modal('show');
            return;
        }

        if (e.target.closest('.delete-variable-btn')) {
            const index = Array.from(state.els.listContainer.children).indexOf(li);
            state.variables.splice(index, 1);
            li.remove();
            state.markDirty();
        }
    });
}

// Filtrar variables al cercador
function wireSearchFilter(state) {
    state.els.searchInput?.addEventListener('input', (e) => {
        const text = e.target.value.trim().toLowerCase();

        state.els.listContainer.innerHTML = '';

        const matched = text === ''
            ? state.variables
            : state.variables.filter(v => v.name.toLowerCase().includes(text));

        matched.forEach(v => state.els.listContainer.appendChild(createVariableListItem(v)));
    });
}

// Inserir la variable on esta el cursor del textarea HTML
function insertAtCursor(textarea, text) {
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const before = textarea.value.substring(0, start);
    const after = textarea.value.substring(end);
    textarea.value = before + text + after;
    textarea.selectionStart = textarea.selectionEnd = start + text.length;
    textarea.focus();
}