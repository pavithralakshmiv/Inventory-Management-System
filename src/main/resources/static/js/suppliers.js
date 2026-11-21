(async function () {
    await initPage('suppliers');
    const controller = crud({
        endpoint: '/api/suppliers',
        label: 'Supplier',
        fields: ['name', 'contactPerson', 'mobile', 'email', 'address', 'status'],
        columns: [
            { key: 'id' }, { key: 'name' }, { key: 'contactPerson' }, { key: 'mobile' }, { key: 'email' },
            { render: r => badge(r.status) }
        ],
        onSelect: row => { document.getElementById('rid').value = row.id; }
    });
    document.getElementById('btnHistory').addEventListener('click', async () => {
        const row = controller.state.selected;
        if (!row) {
            toast('Select a supplier first.', 'error');
            return;
        }
        try {
            const history = await api('/api/suppliers/' + row.id + '/purchases');
            const body = history.length
                ? '<div class="table-wrap"><table class="grid"><thead><tr><th>Purchase No</th><th>Date</th><th>Products</th><th class="num">Total</th><th>Status</th></tr></thead><tbody>' +
                  history.map(h => '<tr><td>' + esc(h.purchaseNumber) + '</td><td>' + fmtDate(h.purchaseDate) + '</td><td>' +
                      lineSummary(h.items) + '</td><td class="num">' + money(h.total) + '</td><td>' + badge(h.status) + '</td></tr>').join('') + '</tbody></table></div>'
                : '<p>No purchases from this supplier yet.</p>';
            openModal('Purchase History - ' + row.name, body);
        } catch (e) {
            toast(e.message, 'error');
        }
    });
})();
