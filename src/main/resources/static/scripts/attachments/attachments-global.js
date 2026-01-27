import {attachmentsApi} from './attachments-api.js';
import {handleError} from '../http-utils.js';

/**
 * Elimina un adjunt (accessible globalment des d'HTML)
 * @param {number} id - ID de l'adjunt a eliminar
 */
window.deleteAttachment = async function (id) {
    if (!confirm('Estàs segur que vols eliminar aquest adjunt?')) {
        return;
    }

    try {
        await attachmentsApi.deleteAttachment(id);
        alert('Èxit! Adjunt eliminat correctament.');
        window.location.href = '/plantilles-web/attachments';
    } catch (error) {
        handleError('Error', error);
    }
};