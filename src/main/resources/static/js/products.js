(async function () {
    await initPage('products');
    const $ = id => document.getElementById(id);
    const today = () => new Date().toISOString().substring(0, 10);

    await fillSelect('categoryId', '/api/categories', c => c.name, '-- Select Category --', c => c.status === 'Active');
    await fillSelect('supplierId', '/api/suppliers', s => s.name, '-- No Supplier --', s => s.status === 'Active');
    const categories = await api('/api/categories');
    categories.forEach(c => {
        const option = document.createElement('option');
        option.value = c.id;
        option.textContent = c.name;
        $('filterCategory').appendChild(option);
    });

    function showPreview(path) {
        $('previewBox').innerHTML = path ? '<img src="' + esc(path) + '" alt="Product image">' : 'No image';
    }

    const controller = crud({
        endpoint: '/api/products',
        label: 'Product',
        fields: ['productCode', 'name', 'categoryId', 'supplierId', 'purchasePrice', 'sellingPrice', 'quantity',
            'brand', 'productDate', 'description', 'status', 'imagePath'],
        columns: [
            { key: 'id' },
            { render: r => r.imagePath ? '<img class="thumb" src="' + esc(r.imagePath) + '" alt="">' : '' },
            { key: 'productCode' }, { key: 'name' }, { key: 'categoryName' }, { key: 'supplierName' },
            { render: r => money(r.purchasePrice), num: true }, { render: r => money(r.sellingPrice), num: true },
            { key: 'quantity', num: true }, { render: r => badge(r.stockStatus) }
        ],
        extraQuery: () => ({ categoryId: $('filterCategory').value }),
        toPayload: v => ({
            productCode: v.productCode.trim(),
            name: v.name.trim(),
            categoryId: Number(v.categoryId),
            supplierId: v.supplierId ? Number(v.supplierId) : null,
            imagePath: v.imagePath || null,
            purchasePrice: Number(v.purchasePrice),
            sellingPrice: Number(v.sellingPrice),
            quantity: parseInt(v.quantity, 10),
            brand: v.brand,
            productDate: v.productDate || null,
            description: v.description,
            status: v.status
        }),
        onSelect: row => { showPreview(row.imagePath); $('imageFile').value = ''; },
        onReset: () => { showPreview(null); $('productDate').value = today(); $('quantity').value = 0; $('imageFile').value = ''; },
        beforeSave: async body => {
            const file = $('imageFile').files[0];
            if (file) {
                const data = new FormData();
                data.append('file', file);
                const result = await api('/api/files/upload', { method: 'POST', body: data });
                body.imagePath = result.path;
            }
        }
    });

    $('productDate').value = today();
    $('filterCategory').addEventListener('change', controller.load);
    $('imageFile').addEventListener('change', () => {
        const file = $('imageFile').files[0];
        if (file) {
            showPreview(URL.createObjectURL(file));
        }
    });
})();
