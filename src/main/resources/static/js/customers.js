(async function () {
    await initPage('customers');
    const controller = crud({
        endpoint: '/api/customers',
        label: 'Customer',
        fields: ['name', 'mobile', 'email', 'address', 'status'],
        columns: [
            { key: 'id' }, { key: 'name' }, { key: 'mobile' }, { key: 'email' }, { key: 'address' },
            { render: r => badge(r.status) }
        ],
        onSelect: row => { document.getElementById('rid').value = row.id; }
    });
    document.getElementById('btnHistory').addEventListener('click', async () => {
        const row = controller.state.selected;
        if (!row) {
            toast('Select a customer first.', 'error');
            return;
        }
        try {
            const history = await api('/api/customers/' + row.id + '/history');
            const body = history.length
                ? '<div class="table-wrap"><table class="grid"><thead><tr><th>Type</th><th>Number</th><th>Date</th><th class="num">Total</th><th>Status</th></tr></thead><tbody>' +
                  history.map(h => '<tr><td>' + esc(h.type) + '</td><td>' + esc(h.number) + '</td><td>' + fmtDate(h.date) +
                      '</td><td class="num">' + money(h.total) + '</td><td>' + badge(h.status) + '</td></tr>').join('') + '</tbody></table></div>'
                : '<p>This customer has no orders or sales yet.</p>';
            openModal('History - ' + row.name, body);
        } catch (e) {
            toast(e.message, 'error');
        }
    });
})();
