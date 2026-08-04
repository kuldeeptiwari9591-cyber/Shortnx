async function loadLinks() {
    const body = document.getElementById('linksBody');
    try {
        const res = await fetch('/api/links');
        if (res.status === 401 || res.redirected) {
            window.location.href = '/login';
            return;
        }
        const links = await res.json();
        if (links.length === 0) {
            body.innerHTML = '<tr><td colspan="5">No links yet. <a href="/shorten.html">Create one.</a></td></tr>';
            return;
        }
        body.innerHTML = links.map(l => `
            <tr>
                <td class="mono">${escapeHtml(l.shortCode)}</td>
                <td>${escapeHtml(truncate(l.longUrl, 50))}</td>
                <td>${l.clickCount}</td>
                <td>${new Date(l.createdAt).toLocaleDateString()}</td>
                <td><button class="btn btn-ghost" data-delete-id="${l.id}">Delete</button></td>
            </tr>
        `).join('');

        // CSP blocks inline onclick="..." handlers, so wire delete buttons
        // up here instead of inline in the generated markup.
        body.querySelectorAll('[data-delete-id]').forEach(btn => {
            btn.addEventListener('click', () => deleteLink(btn.getAttribute('data-delete-id')));
        });
    } catch (e) {
        body.innerHTML = '<tr><td colspan="5">Could not load links.</td></tr>';
    }
}

async function deleteLink(id) {
    if (!confirm('Delete this link?')) return;
    await fetch('/api/links?id=' + encodeURIComponent(id), { method: 'DELETE' });
    loadLinks();
}

// Escaping here matters just as much as on the server: this data
// came from the DB and is being written into innerHTML, so a
// stored XSS payload in a long_url would execute in the dashboard
// of whoever views it if we skipped this step.
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
function truncate(str, n) {
    return str.length > n ? str.slice(0, n) + '…' : str;
}

document.getElementById('logoutBtn').addEventListener('click', async () => {
    await fetch('/api/logout', { method: 'POST' });
    window.location.href = '/';
});

loadLinks();
