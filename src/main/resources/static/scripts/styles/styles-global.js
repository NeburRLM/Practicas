import {stylesApi} from './styles-api.js';
import {handleError} from '../http-utils.js';

/**
 * Elimina un estil (accessible globalment des d'HTML)
 * @param {number} id - ID de l'estil a eliminar
 */
window.deleteStyle = async function (id) {
    if (!confirm('Estàs segur que vols eliminar aquest estil?')) {
        return;
    }

    try {
        await stylesApi.deleteStyle(id);
        alert('Èxit! Estil eliminat correctament.');
        window.location.href = '/plantilles-web/styles';
    } catch (error) {
        handleError('Error', new Error('No ha sigut possible eliminar l\'estil'));
    }
};