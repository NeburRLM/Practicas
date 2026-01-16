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

document.addEventListener('DOMContentLoaded', function () {
    const fileInput = document.getElementById('fileInput');
    if (fileInput) {
        fileInput.addEventListener('change', function () {
            this.nextElementSibling.textContent = this.files[0]?.name || 'Tria un arxiu...';
        });
    }
});

function handleExportClick(btn) {
    const id = btn.getAttribute('data-id');
    const code = btn.getAttribute('data-code');
    exportTemplate(id, code);
}

function importTemplate() {
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
            if (typeof result === 'string') {
                const templateData = JSON.parse(result);

                const token = document.querySelector('meta[name="_csrf"]').content;
                const header = document.querySelector('meta[name="_csrf_header"]').content;

                const config = {};
                config.headers = {};
                config.headers[header] = token;
                config.headers['content-type'] = 'application/json';
                config.method = 'POST';
                config.body = JSON.stringify(templateData);

                const response = await fetch('/plantilles-web/templates/import', config);
                if (response.ok) {
                    $('#exampleModal').modal('hide');
                    alert('Èxit! Plantilla importada correctament.');
                    window.location.href = '/plantilles-web/templates';
                } else {
                    alert('Error important la plantilla');
                }
            } else {
                alert('El resultat no és un string');
            }
        } catch (error) {
            alert('L\'arxiu no conté un JSON vàlid');
        }
    };
    reader.readAsText(file);
}

function exportTemplate(id, code) {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const config = {};
    config.headers = {};
    config.headers[header] = token;
    config.method = 'GET';

    fetch('/plantilles-web/templates/export/' + id, config)
        .then(response => {
            if (!response.ok) throw new Error('Error al exportar');
            return response.text();
        })
        .then(text => {
            const json = JSON.parse(text);
            const content = json.item || json;

            const blob = new Blob([JSON.stringify(content, null, 2)], {
                type: 'application/json'
            });

            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = code + '.json';
            a.click();
            URL.revokeObjectURL(url);
        })
        .catch(err => {
            alert('Error exportant la plantilla: ' + err.message);
        });
}