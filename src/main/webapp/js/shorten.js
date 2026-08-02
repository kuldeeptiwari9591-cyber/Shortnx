// Fetch a session-bound CSRF token on load and attach it before submit.
fetch('/api/csrf-token').then(r => r.json()).then(d => {
    document.getElementById('csrfToken').value = d.csrfToken;
});

document.getElementById('shortenForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const errorEl = document.getElementById('errorMsg');
    const resultEl = document.getElementById('resultBox');
    errorEl.classList.remove('show');
    resultEl.classList.remove('show');

    const formData = new FormData(this);
    try {
        const res = await fetch('/api/shorten', { method: 'POST', body: formData });
        const data = await res.json();
        if (!res.ok || !data.success) {
            errorEl.textContent = data.error || 'Something went wrong.';
            errorEl.classList.add('show');
            return;
        }
        document.getElementById('shortUrlText').textContent = data.shortUrl;
        resultEl.classList.add('show');
    } catch (err) {
        errorEl.textContent = 'Network error. Please try again.';
        errorEl.classList.add('show');
    }
});

document.getElementById('copyBtn').addEventListener('click', function () {
    navigator.clipboard.writeText(document.getElementById('shortUrlText').textContent);
    this.textContent = 'Copied!';
    setTimeout(() => (this.textContent = 'Copy'), 1500);
});
