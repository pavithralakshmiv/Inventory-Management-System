(async function () {
    await initPage('view-orders');
    const $ = id => document.getElementById(id);
    let rows = [];
    let selected = null;

    const columns = [
        { key: 'orderNumber' }, { key: 'customerName' }, { render: r => fmtDate(r.orderDate) },
        { render: r => lineSummary(r.items) }, { key: 'totalQuantity', num: true },
        { render: r => money(r.total), num: true }, { render: r => badge(r.paymentStatus) }, { render: r => badge(r.status) }
    ];

    function render() {
        fillTable('tbody', rows, columns, { onRowClick: select, selectedId: selected ? selected.id : null });
    }

    function select(row) {
        selected = row;
        $('newStatus').value = row.status;
        $('newPayment').value = row.paymentStatus;
        render();
    }

    async function load() {
        const params = new URLSearchParams();
        if ($('searchBox').value.trim()) { params.set('q', $('searchBox').value.trim()); }
        if ($('statusFilter').value) { params.set('status', $('statusFilter').value); }
        if ($('fromDate').value) { params.set('from', $('fromDate').value); }
        if ($('toDate').value) { params.set('to', $('toDate').value); }
        try {
            rows = await api('/api/orders?' + params.toString());
            if (selected) { selected = rows.find(r => r.id === selected.id) || null; }
            render();
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    function need() {
        if (!selected) { toast('Select an order first.', 'error'); return false; }
        return true;
    }

    $('btnSearch').addEventListener('click', load);
    $('btnRefresh').addEventListener('click', () => {
        $('searchBox').value = ''; $('statusFilter').value = ''; $('fromDate').value = ''; $('toDate').value = '';
        selected = null;
        load();
    });
    $('statusFilter').addEventListener('change', load);
    $('searchBox').addEventListener('keydown', e => { if (e.key === 'Enter') { load(); } });
    $('btnClose').addEventListener('click', () => { window.location.href = 'dashboard.html'; });

    $('btnView').addEventListener('click', () => {
        if (!need()) { return; }
        const o = selected;
        openModal('Order ' + o.orderNumber,
            '<p><b>Customer:</b> ' + esc(o.customerName) + '<br><b>Date:</b> ' + fmtDate(o.orderDate) +
            '<br><b>Status:</b> ' + esc(o.status) + ' &nbsp; <b>Payment:</b> ' + esc(o.paymentStatus) +
            '<br><b>Created by:</b> ' + esc(o.createdBy) + '</p>' + linesTable(o.items) +
            '<div class="totals"><div><span>Subtotal</span><span>' + money(o.subtotal) + '</span></div><div><span>Discount (' +
            o.discountPercent + '%)</span><span>- ' + money(o.discountAmount) + '</span></div><div class="grand"><span>Total</span><span>' +
            money(o.total) + '</span></div></div>');
    });

    $('btnStatus').addEventListener('click', async () => {
        if (!need()) { return; }
        const status = $('newStatus').value;
        if (status === selected.status) { toast('The order already has this status.', 'error'); return; }
        if (status === 'Cancelled' && !(await confirmDialog('Cancel this order? The ordered quantities will be returned to stock and the order cannot be changed afterwards.'))) { return; }
        try {
            await api('/api/orders/' + selected.id + '/status', { method: 'PUT', body: { status: status } });
            toast('Order status updated.');
            await load();
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnPayment').addEventListener('click', async () => {
        if (!need()) { return; }
        try {
            await api('/api/orders/' + selected.id + '/payment', { method: 'PUT', body: { status: $('newPayment').value } });
            toast('Payment status updated.');
            await load();
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    load();
})();
