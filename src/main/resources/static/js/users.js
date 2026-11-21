(async function () {
    await initPage('users');
    crud({
        endpoint: '/api/users',
        label: 'User',
        fields: ['name', 'mobile', 'email', 'password', 'address', 'status', 'role'],
        columns: [
            { key: 'id' }, { key: 'name' }, { key: 'mobile' }, { key: 'email' }, { key: 'address' },
            { render: r => badge(r.status) }, { key: 'role' }
        ],
        onSelect: row => { document.getElementById('rid').value = row.id; },
        beforeSave: (body, method) => {
            if (method === 'POST' && !body.password) {
                throw new Error('Password is required for a new user.');
            }
        }
    });
})();
