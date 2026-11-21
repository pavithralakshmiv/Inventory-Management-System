const registerForm = document.getElementById('registerForm');

registerForm.addEventListener('submit', async event => {
    event.preventDefault();
    if (!registerForm.reportValidity()) {
        return;
    }
    const password = document.getElementById('password').value;
    if (password !== document.getElementById('confirmPassword').value) {
        toast('Password and confirm password do not match.', 'error');
        return;
    }
    const body = {};
    ['name', 'email', 'mobile', 'password', 'confirmPassword', 'address'].forEach(id => {
        body[id] = document.getElementById(id).value.trim();
    });
    body.password = password;
    body.confirmPassword = document.getElementById('confirmPassword').value;
    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || 'Registration failed.');
        }
        toast(data.message);
        registerForm.reset();
        setTimeout(() => { window.location.href = 'index.html'; }, 1500);
    } catch (e) {
        toast(e.message, 'error');
    }
});

document.getElementById('btnReset').addEventListener('click', () => registerForm.reset());
