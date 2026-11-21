(async function () {
    await initPage('stock');
    const $ = id => document.getElementById(id);
    let rows = [];
    let selected = null;
    const params = new URLSearchParams(window.location.search);
    const lowOnly = params.get('lowOnly') === '1';

    function render() {
        fillTable('tbody', rows, [
            { key: 'productId' }, { key: 'productCode' }, { key: 'productName' }, { key: 'categoryName' },
            { key: 'quantity', num: true }, { render: r => money(r.purchasePrice), num: true },
            { render: r => money(r.sellingPrice), num: true }, { render: r => badge(r.stockStatus) }
        ], { onRowClick: r => { selected = r; render(); }, selectedId: selected ? selected.productId : null, idKey: 'productId' });
    }

    async function load() {
        const qs = new URLSearchParams();
        if ($('searchBox').value.trim()) { qs.set('q', $('searchBox').value.trim()); }
        if ($('statusFilter').value) { qs.set('status', $('statusFilter').value); }
        try {
            rows = await api('/api/stock?' + qs.toString());
            if (lowOnly && !$('statusFilter').value) {
                rows = rows.filter(r => r.stockStatus !== 'Available');
            }
            render();
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    $('btnSearch').addEventListener('click', load);
    $('statusFilter').addEventListener('change', load);
    $('searchBox').addEventListener('keydown', e => { if (e.key === 'Enter') { load(); } });
    $('btnRefresh').addEventListener('click', () => {
        $('searchBox').value = ''; $('statusFilter').value = ''; selected = null;
        window.history.replaceState(null, '', 'stock.html');
        load();
    });
    $('btnClose').addEventListener('click', () => { window.location.href = 'dashboard.html'; });
    $('btnMoves').addEventListener('click', async () => {
        try {
            const moves = await api('/api/stock/movements' + (selected ? '?productId=' + selected.productId : ''));
            const body = moves.length
                ? '<div class="table-wrap"><table class="grid"><thead><tr><th>Date</th><th>Product</th><th>Type</th><th class="num">Change</th><th class="num">Balance</th><th>Note</th><th>By</th></tr></thead><tbody>' +
                  moves.map(m => '<tr><td>' + fmtDate(m.createdAt) + '</td><td>' + esc(m.productName) + '</td><td>' + esc(m.movementType) +
                      '</td><td class="num">' + (m.quantityChange > 0 ? '+' : '') + m.quantityChange + '</td><td class="num">' + m.balanceAfter +
                      '</td><td>' + esc(m.note) + '</td><td>' + esc(m.createdBy) + '</td></tr>').join('') + '</tbody></table></div>'
                : '<p>No stock movements recorded.</p>';
            openModal('Stock Movements' + (selected ? ' - ' + selected.productName : ' (latest 200)'), body);
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    load();
})();
