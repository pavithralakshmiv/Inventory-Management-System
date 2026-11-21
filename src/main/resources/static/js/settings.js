(async function () {
    await initPage('settings');
    const $ = id => document.getElementById(id);
    const keys = ['business_name', 'business_address', 'business_phone', 'business_email', 'currency_symbol', 'low_stock_threshold'];

    $('pEmail').value = ME.email;
    $('pName').value = ME.name || '';
    $('pMobile').value = ME.mobile || '';
    $('pAddress').value = ME.address || '';

    try {
        const settings = await api('/api/settings');
        keys.forEach(k => { $(k).value = settings[k] || ''; });
    } catch (e) {
        toast(e.message, 'error');
    }

    if (!isAdmin()) {
        keys.forEach(k => { $(k).disabled = true; });
        $('btnSettings').disabled = true;
        $('adminNote').textContent = '(only administrators can change these)';
    }

    $('btnProfile').addEventListener('click', async () => {
        if (!$('profileForm').reportValidity()) { return; }
        try {
            ME = await api('/api/auth/profile', {
                method: 'PUT',
                body: { name: $('pName').value.trim(), mobile: $('pMobile').value.trim(), address: $('pAddress').value.trim() }
            });
            renderNav('settings');
            toast('Profile updated successfully.');
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnPassword').addEventListener('click', async () => {
        if (!$('passwordForm').reportValidity()) { return; }
        if ($('newPassword').value !== $('confirmPassword').value) {
            toast('New password and confirm password do not match.', 'error');
            return;
        }
        try {
            await api('/api/auth/password', {
                method: 'PUT',
                body: { oldPassword: $('oldPassword').value, newPassword: $('newPassword').value, confirmPassword: $('confirmPassword').value }
            });
            $('passwordForm').reset();
            toast('Password changed successfully.');
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnSettings').addEventListener('click', async () => {
        if (!$('businessForm').reportValidity()) { return; }
        const body = {};
        keys.forEach(k => { body[k] = $(k).value.trim(); });
        try {
            await api('/api/settings', { method: 'PUT', body: body });
            ME = await api('/api/auth/me');
            renderNav('settings');
            toast('Settings saved successfully.');
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnClose').addEventListener('click', () => { window.location.href = 'dashboard.html'; });
})();
