import {templatesApi} from './templates-api.js';
import {handleError} from '../http-utils.js';
import {createTemplateState} from './templateState.js';
import {wireTemplateModals} from './templateModals.js';
import {wireTemplateVariables} from './templateVariables.js';

/**
 * Punt d'entrada de la vista de creació/edició de la plantilla.
 * */

document.addEventListener('DOMContentLoaded', async () => {
    const state = createTemplateState();    // crea estat compartit entre els components de la plantilla

    state.enableBeforeUnloadWarning();  // activa avís de canvis no guardats al sortir

    // Connecta els mòduls de la vista
    wireTemplateModals(state);  // modals d'estil i configuració
    await wireTemplateVariables(state, templatesApi);   // component de variables de plantilla

    state.wireEditorDirty();    // detecció de canvis al editor de HTML del textarea
    state.wireFunctionItems();  // component per afegir funcions a la plantilla

    // Guardat persistent final al backend
    state.wireSave(async () => {
        const payload = state.buildTemplatePayload();
        const result = await templatesApi.saveTemplate(payload, state.isEdit());
        state.clearDirty();
        return result;
    }, handleError);
});