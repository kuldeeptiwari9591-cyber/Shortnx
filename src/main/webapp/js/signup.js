document.getElementById('signupForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    const errorEl = document.getElementById('errorMsg');
    errorEl.classList.remove('show');
    const formData = new FormData(this);
    try {
        const res = await fetch('/api/signup', { method: 'POST', body: formData });
        const data = await res.json();
        if (!res.ok || !data.success) {
            errorEl.textContent = data.error || 'Signup failed.';
            errorEl.classList.add('show');
            return;
        }
        window.location.href = '/dashboard.html';
    } catch (err) {
        errorEl.textContent = 'Network error. Please try again.';
        errorEl.classList.add('show');
    }
});
