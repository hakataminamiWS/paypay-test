function autoClose() {
    setTimeout(window.close, 5000);
}

window.addEventListener("load", autoClose, {
    capture: true,
    once: true,
    passive: true
});