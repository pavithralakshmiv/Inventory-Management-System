
let ME = null;

const NAV_MAIN = [
    { key: 'users', icon: '👤', label: 'User', href: 'users.html', admin: true },
    { key: 'categories', icon: '🏷️', label: 'Category', href: 'categories.html' },
    { key: 'products', icon: '📦', label: 'Product', href: 'products.html' },
    { key: 'customers', icon: '👥', label: 'Customer', href: 'customers.html' },
    { key: 'orders', icon: '🛒', label: 'Order', href: 'orders.html' },
    { key: 'view-orders', icon: '📋', label: 'View Order', href: 'view-orders.html' }
];

const NAV_MORE = [
    { key: 'dashboard', icon: '🏠', label: 'Dashboard', href: 'dashboard.html' },
    { key: 'suppliers', icon: '🏭', label: 'Suppliers', href: 'suppliers.html' },
    { key: 'stock', icon: '📊', label: 'Current Stock', href: 'stock.html' },
    { key: 'sales', icon: '💰', label: 'Sales', href: 'sales.html' },
    { key: 'purchase', icon: '🧾', label: 'Purchase', href: 'purchase.html' },
    { key: 'reports', icon: '📈', label: 'Reports', href: 'reports.html', admin: true },
    { key: 'logs', icon: '📜', label: 'User Logs', href: 'logs.html', admin: true },
    { key: 'settings', icon: '⚙️', label: 'Settings', href: 'settings.html' }
];

function esc(value) {
    return String(value === null || value === undefined ? '' : value)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function isAdmin() {
    return ME && ME.role === 'ADMIN';
}

async function api(url, options) {
    const opts = Object.assign({ credentials: 'same-origin', headers: {} }, options || {});
    opts.headers = Object.assign({}, opts.headers);
    if (opts.body && !(opts.body instanceof FormData) && typeof opts.body !== 'string') {
        opts.body = JSON.stringify(opts.body);
        opts.headers['Content-Type'] = 'application/json';
    }
    const response = await fetch(url, opts);
    if (response.status === 401) {
        window.location.href = 'index.html';
        throw new Error('Please log in to continue.');
    }
    const type = response.headers.get('content-type') || '';
    const data = type.includes('application/json') ? await response.json() : null;
    if (!response.ok) {
        throw new Error((data && data.message) || 'Request failed (' + response.status + ')');
    }
    return data;
}

function toast(message, type) {
    let box = document.getElementById('toast');
    if (!box) {
        box = document.createElement('div');
        box.id = 'toast';
        document.body.appendChild(box);
    }
    const item = document.createElement('div');
    item.className = 'msg ' + (type === 'error' ? 'error' : 'success');
    item.textContent = message;
    box.appendChild(item);
    setTimeout(() => item.remove(), type === 'error' ? 5500 : 3200);
}

function openModal(title, bodyHtml, buttons) {
    closeModal();
    const overlay = document.createElement('div');
    overlay.className = 'overlay';
    overlay.id = 'modalOverlay';
    overlay.innerHTML = '<div class="modal"><div class="modal-title">' + esc(title) + '</div>' +
        '<div class="modal-body">' + bodyHtml + '</div><div class="modal-foot"></div>';
    const foot = overlay.querySelector('.modal-foot');
    (buttons || [{ label: 'Close' }]).forEach(b => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn';
        btn.textContent = b.label;
        btn.addEventListener('click', () => {
            if (!b.onClick || b.onClick() !== false) {
                closeModal();
            }
        });
        foot.appendChild(btn);
    });
    document.body.appendChild(overlay);
    return overlay;
}

function closeModal() {
    const existing = document.getElementById('modalOverlay');
    if (existing) {
        existing.remove();
    }
}

function confirmDialog(message) {
    return new Promise(resolve => {
        openModal('Please Confirm', '<p>' + esc(message) + '</p>', [
            { label: 'Yes', onClick: () => { resolve(true); } },
            { label: 'No', onClick: () => { resolve(false); } }
        ]);
    });
}

// Fixed INR currency formatting
function money(value) {
    const number = Number(value || 0);
    return '\u20B9' + number.toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function fmtDate(value) {
    if (!value) {
        return '';
    }
    const text = String(value);
    return text.includes('T') ? text.replace('T', ' ').substring(0, 16) : text.substring(0, 10);
}

function statusClass(status) {
    const s = String(status || '').toLowerCase();
    if (['available', 'active', 'completed', 'delivered', 'paid'].includes(s)) {
        return 'ok';
    }
    if (['low stock', 'pending', 'processing', 'partial', 'shipped'].includes(s)) {
        return 'low';
    }
    return 'out';
}

function badge(status) {
    return '<span class="' + statusClass(status) + '">' + esc(status) + '</span>';
}

function renderNav(activeKey) {
    const nav = document.getElementById('nav');
    if (!nav) {
        return;
    }
    const visible = list => list.filter(i => !i.admin || isAdmin());
    const main = visible(NAV_MAIN).map(i =>
        '<a class="nav-btn' + (i.key === activeKey ? ' active' : '') + '" href="' + i.href + '">' + i.icon + ' ' + esc(i.label) + '</a>').join('');
    const more = visible(NAV_MORE).map(i =>
        '<a class="' + (i.key === activeKey ? 'active' : '') + '" href="' + i.href + '">' + i.icon + ' ' + esc(i.label) + '</a>').join('');
    const moreActive = NAV_MORE.some(i => i.key === activeKey) ? ' active' : '';
    nav.innerHTML =
        '<div class="userbar"><a href="dashboard.html">🏠 ' + esc(ME.businessName) + '</a>' +
        '<span>Logged in as <b>' + esc(ME.name) + '</b> (' + esc(ME.role) + ')</span></div>' +
        '<div class="nav">' + main +
        '<div class="nav-more"><button type="button" class="nav-btn' + moreActive + '" id="moreBtn">☰ More ▾</button>' +
        '<div class="nav-menu" id="moreMenu">' + more + '</div></div>' +
        '<button type="button" class="nav-btn" id="logoutBtn">🚪 Logout</button></div>';
    document.getElementById('moreBtn').addEventListener('click', e => {
        e.stopPropagation();
        document.getElementById('moreMenu').classList.toggle('open');
    });
    document.addEventListener('click', () => {
        const menu = document.getElementById('moreMenu');
        if (menu) {
            menu.classList.remove('open');
        }
    });
    document.getElementById('logoutBtn').addEventListener('click', logout);
}

async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
    } finally {
        window.location.href = 'logout.html';
    }
}

async function initPage(activeKey) {
    ME = await api('/api/auth/me');
    renderNav(activeKey);
    if (new URLSearchParams(window.location.search).get('denied')) {
        toast('You do not have permission to open that page.', 'error');
    }
    return ME;
}

function fillTable(tbodyId, rows, columns, options) {
    const opts = options || {};
    const tbody = document.getElementById(tbodyId);
    if (!rows || rows.length === 0) {
        tbody.innerHTML = '<tr><td class="empty" colspan="' + columns.length + '">No records found</td></tr>';
        return;
    }
    tbody.innerHTML = rows.map((row, index) => {
        const selected = opts.selectedId !== undefined && opts.selectedId !== null && row[opts.idKey || 'id'] === opts.selectedId;
        return '<tr data-index="' + index + '"' + (selected ? ' class="selected"' : '') + '>' +
            columns.map(c => '<td' + (c.num ? ' class="num"' : '') + '>' +
                (c.render ? c.render(row) : esc(row[c.key])) + '</td>').join('') + '</tr>';
    }).join('');
    if (opts.onRowClick) {
        tbody.querySelectorAll('tr').forEach(tr => {
            tr.addEventListener('click', () => opts.onRowClick(rows[Number(tr.dataset.index)]));
        });
    }
}

function tableHead(columns) {
    return '<tr>' + columns.map(c => '<th' + (c.num ? ' class="num"' : '') + '>' + esc(c.label) + '</th>').join('') + '</tr>';
}

function crud(cfg) {
    const $ = id => document.getElementById(id);
    const state = { rows: [], selected: null };
    const form = $('entityForm');
    const idKey = cfg.idKey || 'id';

    function payload() {
        const values = {};
        cfg.fields.forEach(f => { values[f] = $(f).value; });
        return cfg.toPayload ? cfg.toPayload(values) : values;
    }

    function setButtons() {
        const has = state.selected !== null;
        if ($('btnSave')) { $('btnSave').disabled = has; }
        if ($('btnUpdate')) { $('btnUpdate').disabled = !has; }
        if ($('btnDelete')) {
            $('btnDelete').disabled = !has;
            $('btnDelete').classList.toggle('hidden', !isAdmin());
        }
    }

    function render() {
        fillTable('tbody', state.rows, cfg.columns, {
            onRowClick: select,
            selectedId: state.selected ? state.selected[idKey] : null,
            idKey: idKey
        });
    }

    function select(row) {
        state.selected = row;
        cfg.fields.forEach(f => {
            const value = row[f];
            $(f).value = value === null || value === undefined ? '' : value;
        });
        if (cfg.onSelect) { cfg.onSelect(row); }
        setButtons();
        render();
    }

    function reset() {
        state.selected = null;
        form.reset();
        if (cfg.onReset) { cfg.onReset(); }
        setButtons();
        render();
    }

    async function load() {
        const q = $('searchBox') ? $('searchBox').value.trim() : '';
        const params = new URLSearchParams();
        if (q) { params.set('q', q); }
        if (cfg.extraQuery) {
            const extra = cfg.extraQuery();
            Object.keys(extra).forEach(k => { if (extra[k]) { params.set(k, extra[k]); } });
        }
        const qs = params.toString();
        try {
            state.rows = await api(cfg.endpoint + (qs ? '?' + qs : ''));
            if (state.selected) {
                state.selected = state.rows.find(r => r[idKey] === state.selected[idKey]) || null;
            }
            render();
            setButtons();
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    async function save(method) {
        if (!form.reportValidity()) { return; }
        if (method === 'PUT' && !state.selected) {
            toast('Select a record from the table first.', 'error');
            return;
        }
        try {
            const body = payload();
            if (cfg.beforeSave) { await cfg.beforeSave(body, method); }
            const url = method === 'PUT' ? cfg.endpoint + '/' + state.selected[idKey] : cfg.endpoint;
            await api(url, { method: method, body: body });
            toast(cfg.label + (method === 'PUT' ? ' updated successfully.' : ' saved successfully.'));
            state.selected = null;
            form.reset();
            if (cfg.onReset) { cfg.onReset(); }
            await load();
            if (cfg.afterSave) { await cfg.afterSave(); }
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    async function remove() {
        if (!state.selected) {
            toast('Select a record from the table first.', 'error');
            return;
        }
        if (!(await confirmDialog('Delete this ' + cfg.label.toLowerCase() + '? This cannot be undone.'))) { return; }
        try {
            await api(cfg.endpoint + '/' + state.selected[idKey], { method: 'DELETE' });
            toast(cfg.label + ' deleted successfully.');
            reset();
            await load();
        } catch (e) {
            toast(e.message, 'error');
        }
    }

    function bind(id, handler) {
        if ($(id)) { $(id).addEventListener('click', handler); }
    }

    bind('btnSave', () => save('POST'));
    bind('btnUpdate', () => save('PUT'));
    bind('btnDelete', remove);
    bind('btnReset', reset);
    bind('btnSearch', load);
    bind('btnRefresh', () => { if ($('searchBox')) { $('searchBox').value = ''; } load(); });
    bind('btnClose', () => { window.location.href = 'dashboard.html'; });
    if ($('searchBox')) {
        $('searchBox').addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); load(); } });
    }
    form.addEventListener('submit', e => e.preventDefault());
    setButtons();
    load();
    return { state: state, load: load, reset: reset, select: select };
}

function itemBuilder(cfg) {
    const $ = id => document.getElementById(id);
    let products = [];
    let cart = [];

    function selectedProduct() {
        return products.find(p => String(p.id) === $(cfg.productId).value);
    }

    function setProducts(list) {
        products = list.filter(p => p.status === 'Active');
        const select = $(cfg.productId);
        select.innerHTML = '<option value="">-- Select Product --</option>' + products.map(p =>
            '<option value="' + p.id + '">' + esc(p.productCode + ' - ' + p.name + (cfg.showStock ? ' (Stock: ' + p.quantity + ')' : '')) + '</option>').join('');
        if ($(cfg.priceId)) { $(cfg.priceId).value = ''; }
    }

    function recalc() {
        const subtotal = cart.reduce((sum, i) => sum + i.quantity * i.price, 0);
        let pct = $(cfg.discountId) ? Number($(cfg.discountId).value || 0) : 0;
        if (isNaN(pct) || pct < 0) { pct = 0; }
        if (pct > 100) { pct = 100; }
        const discount = Math.round(subtotal * pct) / 100;
        $(cfg.subtotalId).textContent = money(subtotal);
        if ($(cfg.discountAmountId)) { $(cfg.discountAmountId).textContent = '- ' + money(discount); }
        $(cfg.totalId).textContent = money(subtotal - discount);
    }

    function renderCart() {
        const tbody = $(cfg.tbodyId);
        if (cart.length === 0) {
            tbody.innerHTML = '<tr><td class="empty" colspan="5">No products added</td></tr>';
        } else {
            tbody.innerHTML = cart.map((i, index) => '<tr><td>' + esc(i.name) + '</td><td class="num">' + i.quantity +
                '</td><td class="num">' + money(i.price) + '</td><td class="num">' + money(i.quantity * i.price) +
                '</td><td><button type="button" class="btn" data-remove="' + index + '">✖</button></td></tr>').join('');
            tbody.querySelectorAll('[data-remove]').forEach(b => b.addEventListener('click', () => {
                cart.splice(Number(b.dataset.remove), 1);
                renderCart();
            }));
        }
        recalc();
    }

    $(cfg.productId).addEventListener('change', () => {
        const p = selectedProduct();
        if ($(cfg.priceId)) { $(cfg.priceId).value = p ? p[cfg.priceKey] : ''; }
    });

    $(cfg.addBtnId).addEventListener('click', () => {
        const p = selectedProduct();
        const quantity = parseInt($(cfg.qtyId).value, 10);
        if (!p) { toast('Select a product first.', 'error'); return; }
        if (!quantity || quantity < 1) { toast('Enter a quantity of at least 1.', 'error'); return; }
        let price = Number(p[cfg.priceKey]);
        if (cfg.editablePrice) {
            price = Number($(cfg.priceId).value);
            if (isNaN(price) || price < 0 || $(cfg.priceId).value === '') { toast('Enter a valid price.', 'error'); return; }
        }
        const existing = cart.find(i => i.productId === p.id && i.price === price);
        const already = cart.filter(i => i.productId === p.id).reduce((s, i) => s + i.quantity, 0);
        if (cfg.showStock && already + quantity > p.quantity) {
            toast('Only ' + p.quantity + ' units of ' + p.name + ' are in stock.', 'error');
            return;
        }
        if (existing) {
            existing.quantity += quantity;
        } else {
            cart.push({ productId: p.id, name: p.name, quantity: quantity, price: price });
        }
        $(cfg.productId).value = '';
        $(cfg.qtyId).value = 1;
        if ($(cfg.priceId)) { $(cfg.priceId).value = ''; }
        renderCart();
    });

    if ($(cfg.discountId)) {
        $(cfg.discountId).addEventListener('input', recalc);
    }
    renderCart();

    return {
        setProducts: setProducts,
        items: () => cart.map(i => cfg.editablePrice
            ? { productId: i.productId, quantity: i.quantity, price: i.price }
            : { productId: i.productId, quantity: i.quantity }),
        isEmpty: () => cart.length === 0,
        clear: () => { cart = []; renderCart(); }
    };
}

function lineSummary(items) {
    return items.map(i => esc(i.productName) + ' × ' + i.quantity).join(', ');
}

function linesTable(items) {
    return '<div class="table-wrap"><table class="grid"><thead><tr><th>Code</th><th>Product</th><th class="num">Qty</th><th class="num">Price</th><th class="num">Total</th></tr></thead><tbody>' +
        items.map(i => '<tr><td>' + esc(i.productCode) + '</td><td>' + esc(i.productName) + '</td><td class="num">' + i.quantity +
            '</td><td class="num">' + money(i.unitPrice) + '</td><td class="num">' + money(i.lineTotal) + '</td></tr>').join('') +
        '</tbody></table></div>';
}

async function fillSelect(selectId, url, labelFn, placeholder, filterFn) {
    const rows = await api(url);
    const list = filterFn ? rows.filter(filterFn) : rows;
    document.getElementById(selectId).innerHTML = (placeholder === null ? '' : '<option value="">' + esc(placeholder) + '</option>') +
        list.map(r => '<option value="' + r.id + '">' + esc(labelFn(r)) + '</option>').join('');
    return rows;
}
