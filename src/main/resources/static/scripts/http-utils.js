/**
 * Centralitza la construcció del token CSRF, la configuració dels headers i el cos de la petició i gestiona els errors.
 * */

// Utilitats per CSRF
export const getCsrfConfig = () => {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (!token || !header) {
        throw new Error('CSRF token no trobat');
    }

    return {token, header};
};

export const createFetchConfig = (method = 'GET', body = null, isFormData = false) => {
    const {token, header} = getCsrfConfig();

    const config = {
        method,
        headers: {
            [header]: token
        }
    };

    // No afegir Content-Type per FormData (el navegador ho fa automàticament)
    if (body && !isFormData) {
        config.headers['Content-Type'] = 'application/json';
        config.body = JSON.stringify(body);
    } else if (body) {
        config.body = body;
    }

    return config;
};

// Gestió d'errors
export const handleError = (message, error) => {
    console.error(message, error);
    alert(`${message}: ${error.message || error}`);
};