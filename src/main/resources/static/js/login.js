document.getElementById('loginForm').addEventListener('submit', async event => {
    event.preventDefault();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    if (!email || !password) {
        toast('Enter your email and password.', 'error');
        return;
    }
    const button = document.getElementById('btnLogin');
    button.disabled = true;
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            credentials: 'same-origin',
            body: new URLSearchParams({ email: email, password: password })
        });
        const data = await response.json().catch(() => ({}));
        if (response.ok) {
            window.location.href = 'dashboard.html';
        } else {
            toast(data.message || 'Login failed.', 'error');
        }
    } catch (e) {
        toast('Cannot reach the server.', 'error');
    } finally {
        button.disabled = false;
    }
});

document.getElementById('btnCloseApp').addEventListener('click', async () => {
    if (await confirmDialog('Close the application?')) {
        window.close();
        setTimeout(() => toast('Your browser blocked closing the tab. You can close it manually.', 'error'), 400);
    }
});

document.getElementById('forgotLink').addEventListener('click', event => {
    event.preventDefault();
    openModal('Forgot Password',
        '<p>For security, passwords cannot be reset from the login page.</p>' +
        '<p>Please contact your administrator. An administrator can set a new password for you from <b>Manage Users</b>.</p>');
});
