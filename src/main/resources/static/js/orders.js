(async function () {
    await initPage('orders');
    const $ = id => document.getElementById(id);

    const builder = itemBuilder({
        productId: 'productId', qtyId: 'qty', priceId: 'price', addBtnId: 'btnAdd', tbodyId: 'cartBody',
        subtotalId: 'subtotal', discountId: 'discount', discountAmountId: 'discountAmt', totalId: 'total',
        priceKey: 'sellingPrice', showStock: true
    });

    async function loadLists() {
        await fillSelect('customerId', '/api/customers', c => c.name + ' (' + c.mobile + ')', '-- Select Customer --', c => c.status === 'Active');
        builder.setProducts(await api('/api/products'));
    }

    function resetForm() {
        builder.clear();
        $('customerId').value = '';
        $('orderNumber').value = '';
        $('discount').value = 0;
        $('paymentStatus').value = 'Unpaid';
        $('discount').dispatchEvent(new Event('input'));
    }

    $('btnSaveOrder').addEventListener('click', async () => {
        if (!$('customerId').value) { toast('Select a customer.', 'error'); return; }
        if (builder.isEmpty()) { toast('Add at least one product.', 'error'); return; }
        const pct = Number($('discount').value || 0);
        if (isNaN(pct) || pct < 0 || pct > 100) { toast('Discount must be between 0 and 100.', 'error'); return; }
        try {
            const order = await api('/api/orders', {
                method: 'POST',
                body: {
                    customerId: Number($('customerId').value),
                    items: builder.items(),
                    discountPercent: pct,
                    paymentStatus: $('paymentStatus').value
                }
            });
            toast('Order ' + order.orderNumber + ' saved. Total: ' + money(order.total));
            resetForm();
            $('orderNumber').value = order.orderNumber;
            builder.setProducts(await api('/api/products'));
        } catch (e) {
            toast(e.message, 'error');
        }
    });

    $('btnReset').addEventListener('click', resetForm);
    $('btnClose').addEventListener('click', () => { window.location.href = 'dashboard.html'; });
    loadLists().catch(e => toast(e.message, 'error'));
})();
