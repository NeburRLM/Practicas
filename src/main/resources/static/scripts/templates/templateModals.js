/**
 * Mòdul dedicat al comportament dels modals style i config.
 * */

export function wireTemplateModals(state) {
    wireStyleModal(state);
    wireConfigModal(state);
}

// Selecciona l'estil escollit per l'usuari per mostrar-lo després d'aplicar-ho
function wireStyleModal(state) {
    $('#styleModal').on('show.bs.modal', () => {
        const prevStyleId = state.getPrevStyleId();
        if (prevStyleId && state.els.styleSelect) {
            state.els.styleSelect.value = prevStyleId;
        }
    });
    state.els.styleForm?.addEventListener('submit', (e) => {
        e.preventDefault();

        const selected = (state.els.styleSelect?.value || '').trim();
        const nextStyleId = selected !== '' ? selected : null;

        const prevStyleId = state.getPrevStyleId();
        if (nextStyleId !== prevStyleId) state.markDirty();

        state.setStyleHidden(nextStyleId);

        $('#styleModal').modal('hide');
    });
}

// Gestiona el modal de configuració amb snapshot guardat a memòria
function wireConfigModal(state) {
    const $configModal = $('#configModal');

    const applyConfigToModalInputs = (cfg) => {
        const setVal = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.value = String(value ?? '');
        };

        setVal('configAmplada', cfg.width);
        setVal('configAllargada', cfg.height);
        setVal('configMargeSuperior', cfg.marginTop);
        setVal('configMargeInferior', cfg.marginBottom);
        setVal('configMargeEsquerre', cfg.marginLeft);
        setVal('configMargeDret', cfg.marginRight);

        setVal('configNom', state.els.nomPlantilla?.value ?? '');
        setVal('configCodi', state.els.codiPlantilla?.value ?? '');
    };

    $configModal.on('show.bs.modal', () => {
        state.isConfigSaveAction = false;
        state.configSnapshot = structuredClone(state.config);
        applyConfigToModalInputs(state.configSnapshot);
    });

    document.getElementById('configCancelBtn')?.addEventListener('click', () => {
        state.isConfigSaveAction = false;
    });

    $configModal.on('hide.bs.modal', () => {
        if (state.isConfigSaveAction) return;
        if (!state.configSnapshot) return;

        state.config = structuredClone(state.configSnapshot);
        applyConfigToModalInputs(state.configSnapshot);
    });

    state.els.configForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        state.isConfigSaveAction = true;

        const prev = structuredClone(state.config);
        const prevName = state.els.nomPlantilla?.value ?? '';
        const prevCode = state.els.codiPlantilla?.value ?? '';

        const configNom = document.getElementById('configNom')?.value?.trim() ?? '';
        const configCodi = document.getElementById('configCodi')?.value?.trim() ?? '';
        if (!configNom || !configCodi) return;

        state.els.nomPlantilla.value = configNom;
        state.els.codiPlantilla.value = configCodi;

        state.loadConfigFromConfigModalInputs();

        const cfgChanged = JSON.stringify(prev) !== JSON.stringify(state.config);
        const nameChanged = configNom !== prevName;
        const codeChanged = configCodi !== prevCode;

        if (cfgChanged || nameChanged || codeChanged) state.markDirty();

        $('#configModal').modal('hide');
    });
}