(async function () {
    await initPage('sales');
    const $ = id => document.getElementById(id);
    let rows = [];
    let selected = null;

    const builder = itemBuilder({
        productId: 'productId', qtyId: 'qty', priceId: 'price', addBtnId: 'btnAdd', tbodyId: 'cartBody',
        subtotalId: 'subtotal', discountId: 'discount', discountAmountId: 'discountAmt', totalId: 'total',
        priceKey: 'sellingPrice', showStock: true
    });

    async function loadLists() {
        await fillSelect('customerId', '/api/customers', c => c.name + ' (' + c.mobile + ')', '-- Walk-in Customer --', c => c.status === 'Active');
        builder.setProducts(await api('/api/products'));
    }

    function resetForm() {
        builder.clear();
        $('customerId').value = '';
        $('discount').value = 0;
        $('paymentMethod').value = 'Cash';
        $('discount').dispatchEvent(new Event('input'));
    }

    function render() {
        fillTable('tbody', rows, [
            { key: 'invoiceNumber' }, { render: r => fmtDate(r.saleDate) }, { key: 'customerName' },
            { render: r => lineSummary(r.items) }, { render: r => money(r.total), num: true },
            { render: r => money(r.profit), num: true }, { key: 'paymentMethod' }, { render: r => badge(r.status) }
        ], { onRowClick: r => { selected = r; render(); }, selectedId: selected ? selected.id : null });
    }

    async function load() {
        const q = $('searchBox').value.trim();
        try {
            rows = await api('/api/sales' + (q ? '?q=' + encodeURIComponent(q) : ''));
            if (selected) { selected = rows.find(r => r.id === selected.id) || null; }
            render();
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    function offerInvoice(sale) {
        openModal('Sale Completed',
            '<p>Invoice <b>' + esc(sale.invoiceNumber) + '</b> created for ' + esc(sale.customerName) +
            '.</p><p>Total: <b>' + money(sale.total) + '</b></p>',
            [{ label: 'Open Invoice', onClick: () => { window.open('/api/sales/' + sale.id + '/invoice', '_blank'); } }, { label: 'Close' }]);
    }

    $('btnSaveTxn').addEventListener('click', async () => {
        if (builder.isEmpty()) { toast('Add at least one product.', 'error'); return; }
        const pct = Number($('discount').value || 0);
        if (isNaN(pct) || pct < 0 || pct > 100) { toast('Discount must be between 0 and 100.', 'error'); return; }
        try {
            const sale = await api('/api/sales', {
                method: 'POST',
                body: {
                    customerId: $('customerId').value ? Number($('customerId').value) : null,
                    items: builder.items(),
                    discountPercent: pct,
                    paymentMethod: $('paymentMethod').value
                }
            });
            resetForm();
            builder.setProducts(await api('/api/products'));
            await load();
            offerInvoice(sale);
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnInvoice').addEventListener('click', () => {
        if (!selected) { toast('Select a sale from the history first.', 'error'); return; }
        window.open('/api/sales/' + selected.id + '/invoice', '_blank');
    });

    $('btnCancelTxn').addEventListener('click', async () => {
        if (!selected) { toast('Select a sale from the history first.', 'error'); return; }
        if (selected.status === 'Cancelled') { toast('This sale is already cancelled.', 'error'); return; }
        if (!(await confirmDialog('Cancel sale ' + selected.invoiceNumber + '? The sold quantities will be returned to stock.'))) { return; }
        try {
            await api('/api/sales/' + selected.id + '/cancel', { method: 'PUT' });
            toast('Sale cancelled and stock restored.');
            builder.setProducts(await api('/api/products'));
            await load();
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnSearch').addEventListener('click', load);
    $('btnRefresh').addEventListener('click', () => { $('searchBox').value = ''; selected = null; load(); });
    $('searchBox').addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); load(); } });
    $('btnReset').addEventListener('click', resetForm);
    $('btnClose').addEventListener('click', () => { window.location.href = 'dashboard.html'; });
    $('entityForm').addEventListener('submit', e => e.preventDefault());

    loadLists().catch(e => toast(e.message, 'error'));
    load();
})();
