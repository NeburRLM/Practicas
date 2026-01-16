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