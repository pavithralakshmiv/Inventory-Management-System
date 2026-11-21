(async function () {
    await initPage('logs');
    const $ = id => document.getElementById(id);
    const actions = ['LOGIN', 'LOGOUT', 'PRODUCT_ADDED', 'PRODUCT_UPDATED', 'PRODUCT_DELETED', 'CUSTOMER_ADDED',
        'CUSTOMER_UPDATED', 'CUSTOMER_DELETED', 'SUPPLIER_ADDED', 'SUPPLIER_UPDATED', 'SUPPLIER_DELETED',
        'CATEGORY_ADDED', 'CATEGORY_UPDATED', 'CATEGORY_DELETED', 'ORDER_CREATED', 'ORDER_STATUS_UPDATED',
        'PURCHASE_COMPLETED', 'PURCHASE_CANCELLED', 'SALE_COMPLETED', 'SALE_CANCELLED', 'USER_ADDED', 'USER_UPDATED',
        'USER_DELETED', 'USER_REGISTERED', 'PASSWORD_CHANGED', 'PROFILE_UPDATED', 'SETTINGS_UPDATED', 'REPORT_EXPORTED'];
    $('actionFilter').innerHTML += actions.map(a => '<option value="' + a + '">' + a + '</option>').join('');

    async function load() {
        const qs = new URLSearchParams();
        if ($('searchBox').value.trim()) { qs.set('q', $('searchBox').value.trim()); }
        if ($('actionFilter').value) { qs.set('action', $('actionFilter').value); }
        if ($('fromDate').value) { qs.set('from', $('fromDate').value); }
        if ($('toDate').value) { qs.set('to', $('toDate').value); }
        try {
            const rows = await api('/api/logs?' + qs.toString());
            fillTable('tbody', rows, [
                { key: 'id' }, { key: 'username' }, { key: 'action' }, { key: 'description' }, { render: r => fmtDate(r.createdAt) }
            ]);
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    $('btnSearch').addEventListener('click', load);
    $('actionFilter').addEventListener('change', load);
    $('btnRefresh').addEventListener('click', () => {
        $('searchBox').value = ''; $('actionFilter').value = ''; $('fromDate').value = ''; $('toDate').value = '';
        load();
    });
    $('searchBox').addEventListener('keydown', e => { if (e.key === 'Enter') { load(); } });
    $('btnClose').addEventListener('click', () => { window.location.href = 'dashboard.html'; });
    load();
})();
