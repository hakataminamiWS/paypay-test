function autoClose() {
    setTimeout(function () { this.window.close() }, 5000);
}

window.addEventListener("load", autoClose, {
    capture: true,
    once: true,
    passive: true
});