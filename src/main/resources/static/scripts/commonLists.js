document.addEventListener("DOMContentLoaded", () => {

    document.querySelectorAll("th input").forEach(input => {
        input.addEventListener("click", e => e.stopPropagation());
        input.addEventListener("focus", e => e.stopPropagation());
    });

    window.addEventListener('pageshow', function(event) {
        if (event.persisted) {
            window.location.reload();
        }
    });
});