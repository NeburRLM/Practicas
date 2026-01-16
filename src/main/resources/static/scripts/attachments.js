
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