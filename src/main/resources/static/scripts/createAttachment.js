// noinspection JSUnusedGlobalSymbols
async function deleteAttachment(id) {
    if (confirm('Estàs segur que vols eliminar aquest adjunt?')) {
        try {
            const response = await fetch('/plantilles-web/attachments/delete/' + id, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            if (response.ok) {
                alert('Èxit! Adjunt eliminat correctament.');
                window.location.href = '/plantilles-web/attachments';
            } else {
                const errorText = await response.text();
                alert('Error: ' + errorText);
            }
        } catch (error) {
            alert('Error: No ha sigut possible eliminar l\'adjunt');
        }
    }
}


document.addEventListener('DOMContentLoaded', function () {
    let hasUnsavedChanges = false;

    // Detectar canvis als camps
    document.querySelector('input[name="code"]').addEventListener('input', () => {
        hasUnsavedChanges = true;
    });

    document.querySelector('input[name="name"]').addEventListener('input', () => {
        hasUnsavedChanges = true;
    });

    document.getElementById('fileInput').addEventListener('change', () => {
        hasUnsavedChanges = true;
    });

    // Avís de canvis no guardats
    window.addEventListener('beforeunload', (e) => {
        if (hasUnsavedChanges) {
            e.preventDefault();
        }
    });

    // Actualitzar label de l'arxiu
    const fileInput = document.getElementById('fileInput');
    if (fileInput) {
        fileInput.addEventListener('change', function () {
            this.nextElementSibling.querySelector('span').textContent = this.files[0]?.name || 'Cap arxiu seleccionat';
        });
    }

    const form = document.getElementById("attachmentForm");

    if (form) {
        form.addEventListener("submit", async (event) => {
            event.preventDefault(); // Prevenir enviament per defecte

            const code = document.querySelector('input[name="code"]').value.trim();
            const name = document.querySelector('input[name="name"]').value.trim();
            const fileInput = document.querySelector('input[name="file"]');

            if (!fileInput.files[0] && !document.querySelector('input[name="id"]')) {
                alert('Cal seleccionar un arxiu');
                return;
            }

            const formData = new FormData();
            formData.append('code', code);
            formData.append('name', name);
            if (fileInput.files[0]) {
                formData.append('file', fileInput.files[0]);
            }

            const idInput = document.querySelector('input[name="id"]');
            const isEdit = idInput && idInput.value;

            if (isEdit) {
                formData.append('id', idInput.value);
            }

            try {
                const response = await fetch('/plantilles-web/attachments/save2', {
                    method: isEdit ? 'PUT' : 'POST',
                    body: formData
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    if (errorText.includes('No es permet')) {
                        alert('Error: ' + errorText);
                    } else {
                        alert('Error al guardar l\'adjunt');
                    }
                    return;
                }

                const result = await response.json();

                if (result.item && result.item.id) {
                    hasUnsavedChanges = false;
                    alert('Èxit: L\'adjunt s\'ha guardat correctament');
                    const redirectUrl = '/plantilles-web/attachments/edit/' + result.item.id;
                    window.location.replace(redirectUrl);
                }
            } catch (error) {
                console.error('Error:', error);
                alert('Error: No ha sigut possible guardar l\'adjunt correctament');
            }
        });
    }
});