const btn = document.querySelector('#qrCodeMake');

if (redirectURL && btn) {
    function listener() {
        window.addEventListener('focus', () => { window.location.href = redirectURL });
    }

    btn.removeEventListener('click', listener);
    btn.addEventListener('click', listener, {
        capture: true,
        once: true,
        passive: true
    });
}