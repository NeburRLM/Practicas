// noinspection JSUnusedGlobalSymbols
async function deleteStyle(id) {
    if (confirm('Estàs segur que vols eliminar aquest estil?')) {
        try {
            const token = document.querySelector('meta[name="_csrf"]').content;
            const header = document.querySelector('meta[name="_csrf_header"]').content;

            const config = {};
            config.headers = {};
            config.headers[header] = token;
            config.headers['content-type'] = 'application/json';
            config.method = 'POST';

            const response = await fetch('/plantilles-web/styles/delete/' + id, config);
            if (response.ok) {
                alert('Èxit! Estil eliminat correctament.');
                window.location.href = '/plantilles-web/styles';
            } else {
                const errorText = await response.text();
                alert('Error: ' + errorText);
            }
        } catch (error) {
            alert('Error: No ha sigut possible eliminar l\'estil');
        }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    // Carregar ensLocals
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const config = {};
    config.headers = {};
    config.headers[header] = token;
    config.method = 'GET';

    fetch('/plantilles-web/styles/ensLocals/list', config)
        .then(response => response.json())
        .then(ensLocals => {
            const select = document.querySelector('select[name="ensLocal"]');
            ensLocals.forEach(ensLocal => {
                const option = document.createElement('option');
                option.value = ensLocal.id;
                option.textContent = ensLocal.nom;
                select.appendChild(option);
            });

            // Si està a mode edició, selecciona valor actual
            const currentEnsLocalId = select.querySelector('option[selected]')?.value;
            if (currentEnsLocalId) {
                select.value = currentEnsLocalId;
            }
        })
        .catch(error => console.error('Error carregant ensLocals:', error));

    let hasUnsavedChanges = false;

    // Detectar canvis als camps
    document.querySelector('input[name="code"]').addEventListener('input', () => {
        hasUnsavedChanges = true;
    });

    document.querySelector('input[name="name"]').addEventListener('input', () => {
        hasUnsavedChanges = true;
    });

    document.querySelector('textarea[name="rules"]').addEventListener('input', () => {
        hasUnsavedChanges = true;
    });

    document.querySelector('select[name="ensLocal"]').addEventListener('change', () => {
        hasUnsavedChanges = true;
    });

    // Avís de canvis no guardats abans de sortir de la pàgina
    window.addEventListener('beforeunload', (e) => {
        if (hasUnsavedChanges) {
            e.preventDefault();
        }
    });

    const form = document.getElementById("styleForm");

    if (form) {
        form.addEventListener("submit", async (event) => {
            event.preventDefault(); // Prevenir enviament per defecte

            const code = document.querySelector('input[name="code"]').value.trim();
            const name = document.querySelector('input[name="name"]').value.trim();
            const rules = document.querySelector('textarea[name="rules"]').value;

            const ensLocalSelect = document.querySelector('select[name="ensLocal"]');
            const selectedEnsLocalId = ensLocalSelect.value;

            const ensLocalData = {
                id: selectedEnsLocalId && selectedEnsLocalId.trim() !== "" ? selectedEnsLocalId : "",
                nom: selectedEnsLocalId && selectedEnsLocalId.trim() !== "" ?
                    (ensLocalSelect.options[ensLocalSelect.selectedIndex]?.text || "") : ""
            };

            const styleData = {
                code: code,
                name: name,
                rules: rules,
                ensLocal: ensLocalData
            };

            const idInput = document.querySelector('input[name="id"]');
            const isEdit = idInput && idInput.value;

            if (isEdit) {
                styleData.id = parseInt(idInput.value);
            }

            try {
                const token = document.querySelector('meta[name="_csrf"]').content;
                const header = document.querySelector('meta[name="_csrf_header"]').content;
                const config = {};
                config.headers = {};
                config.headers[header] = token;
                config.headers['content-type'] = 'application/json';
                config.method = isEdit ? 'PUT' : 'POST';
                config.body = JSON.stringify(styleData);

                const response = await fetch('/plantilles-web/styles/save2', config);

                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('Error response:', errorText);
                    alert('Error al guardar l\'estil: ' + errorText);
                    return;
                }

                const result = await response.json();

                if (result.item && result.item.id) {
                    hasUnsavedChanges = false;
                    alert('Èxit: L\'estil s\'ha guardat correctament');
                    const redirectUrl = '/plantilles-web/styles/edit/' + result.item.id;
                    window.location.replace(redirectUrl);
                }
            } catch (error) {
                console.error('Error:', error);
                alert('Error: No ha sigut possible guardar l\'estil correctament');
            }
        });
    }
});