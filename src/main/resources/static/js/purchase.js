(async function () {
    await initPage('purchase');
    const $ = id => document.getElementById(id);
    let rows = [];
    let selected = null;

    const builder = itemBuilder({
        productId: 'productId', qtyId: 'qty', priceId: 'price', addBtnId: 'btnAdd', tbodyId: 'cartBody',
        subtotalId: 'subtotal', totalId: 'total', priceKey: 'purchasePrice', editablePrice: true, showStock: false
    });

    if (!isAdmin()) { $('btnCancelTxn').classList.add('hidden'); }

    async function loadLists() {
        await fillSelect('supplierId', '/api/suppliers', s => s.name, '-- Select Supplier --', s => s.status === 'Active');
        await fillSelect('supplierFilter', '/api/suppliers', s => s.name, 'All Suppliers');
        builder.setProducts(await api('/api/products'));
    }

    function resetForm() {
        builder.clear();
        $('supplierId').value = '';
    }

    function render() {
        fillTable('tbody', rows, [
            { key: 'purchaseNumber' }, { render: r => fmtDate(r.purchaseDate) }, { key: 'supplierName' },
            { render: r => lineSummary(r.items) }, { render: r => money(r.total), num: true }, { render: r => badge(r.status) }
        ], { onRowClick: r => { selected = r; render(); }, selectedId: selected ? selected.id : null });
    }

    async function load() {
        const qs = new URLSearchParams();
        if ($('searchBox').value.trim()) { qs.set('q', $('searchBox').value.trim()); }
        if ($('supplierFilter').value) { qs.set('supplierId', $('supplierFilter').value); }
        try {
            rows = await api('/api/purchases?' + qs.toString());
            if (selected) { selected = rows.find(r => r.id === selected.id) || null; }
            render();
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    $('btnSaveTxn').addEventListener('click', async () => {
        if (!$('supplierId').value) { toast('Select a supplier.', 'error'); return; }
        if (builder.isEmpty()) { toast('Add at least one product.', 'error'); return; }
        try {
            const purchase = await api('/api/purchases', {
                method: 'POST',
                body: { supplierId: Number($('supplierId').value), items: builder.items() }
            });
            toast('Purchase ' + purchase.purchaseNumber + ' saved. Stock updated.');
            resetForm();
            builder.setProducts(await api('/api/products'));
            await load();
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnViewTxn').addEventListener('click', () => {
        if (!selected) { toast('Select a purchase from the history first.', 'error'); return; }
        const items = selected.items.map(i => ({ productCode: i.productCode, productName: i.productName, quantity: i.quantity, unitPrice: i.unitPrice, lineTotal: i.lineTotal }));
        openModal('Purchase ' + selected.purchaseNumber,
            '<p><b>Supplier:</b> ' + esc(selected.supplierName) + '<br><b>Date:</b> ' + fmtDate(selected.purchaseDate) +
            '<br><b>Status:</b> ' + esc(selected.status) + '</p>' + linesTable(items) +
            '<div class="totals"><div class="grand"><span>Total</span><span>' + money(selected.total) + '</span></div></div>');
    });

    $('btnCancelTxn').addEventListener('click', async () => {
        if (!selected) { toast('Select a purchase from the history first.', 'error'); return; }
        if (selected.status === 'Cancelled') { toast('This purchase is already cancelled.', 'error'); return; }
        if (!(await confirmDialog('Cancel purchase ' + selected.purchaseNumber + '? The purchased quantities will be removed from stock (only possible if that stock is still available).'))) { return; }
        try {
            await api('/api/purchases/' + selected.id + '/cancel', { method: 'PUT' });
            toast('Purchase cancelled and stock reversed.');
            builder.setProducts(await api('/api/products'));
            await load();
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnSearch').addEventListener('click', load);
    $('supplierFilter').addEventListener('change', load);
    $('btnRefresh').addEventListener('click', () => { $('searchBox').value = ''; $('supplierFilter').value = ''; selected = null; load(); });
    $('searchBox').addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); load(); } });
    $('btnReset').addEventListener('click', resetForm);
    $('btnClose').addEventListener('click', () => { window.location.href = 'dashboard.html'; });
    $('entityForm').addEventListener('submit', e => e.preventDefault());

    loadLists().catch(e => toast(e.message, 'error'));
    load();
})();
