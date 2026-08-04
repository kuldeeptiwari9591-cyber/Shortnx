document.getElementById('expandForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const errorEl = document.getElementById('errorMsg');
    const resultEl = document.getElementById('resultBox');
    errorEl.classList.remove('show');
    resultEl.classList.remove('show');

    const code = document.getElementById('code').value.trim();
    try {
        const res = await fetch('/api/expand?code=' + encodeURIComponent(code));
        const data = await res.json();
        if (!res.ok || !data.success) {
            errorEl.textContent = data.error || 'Could not expand that link.';
            errorEl.classList.add('show');
            return;
        }
        document.getElementById('longUrlText').textContent = data.longUrl;
        document.getElementById('statusText').textContent = data.active
            ? '✓ Active'
            : '⚠ Expired or disabled';
        const visitLink = document.getElementById('visitLink');
        visitLink.href = data.longUrl;
        resultEl.classList.add('show');
    } catch (err) {
        errorEl.textContent = 'Network error. Please try again.';
        errorEl.classList.add('show');
    }
});
