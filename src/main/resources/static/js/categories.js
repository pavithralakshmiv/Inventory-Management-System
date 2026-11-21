(async function () {
    await initPage('categories');
    crud({
        endpoint: '/api/categories',
        label: 'Category',
        fields: ['name', 'description', 'status'],
        columns: [{ key: 'id' }, { key: 'name' }, { key: 'description' }, { render: r => badge(r.status) }],
        onSelect: row => { document.getElementById('rid').value = row.id; }
    });
})();
