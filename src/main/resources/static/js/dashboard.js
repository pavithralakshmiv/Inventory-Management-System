(async function () {
    await initPage('dashboard');
    try {
        const d = await api('/api/dashboard');
        document.getElementById('sProducts').textContent = d.totalProducts;
        document.getElementById('sCustomers').textContent = d.totalCustomers;
        document.getElementById('sOrders').textContent = d.totalOrders;
        document.getElementById('sLow').textContent = d.lowStock;

        if (typeof Chart !== 'undefined') {
            new Chart(document.getElementById('stockChart'), {
                type: 'bar',
                data: {
                    labels: d.stockByCategory.map(p => p.label),
                    datasets: [{ label: 'Quantity in stock', data: d.stockByCategory.map(p => p.value), backgroundColor: '#b8860b' }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
                }
            });
        }

        document.getElementById('activities').innerHTML = d.recentActivities.length
            ? d.recentActivities.map(a => '<li>✅ ' + esc(a.description || a.action) +
                '<span class="when">' + esc(a.username) + ' · ' + fmtDate(a.createdAt) + '</span></li>').join('')
            : '<li>No activity yet</li>';

        fillTable('recentProducts', d.recentProducts, [
            { key: 'productCode' }, { key: 'name' }, { key: 'categoryName' },
            { key: 'quantity', num: true }, { render: r => money(r.sellingPrice), num: true }
        ]);
        fillTable('recentOrders', d.recentOrders, [
            { key: 'orderNumber' }, { key: 'customerName' },
            { render: r => money(r.total), num: true }, { render: r => badge(r.status) }
        ]);
    } catch (e) {
        toast(e.message, 'error');
    }
})();
