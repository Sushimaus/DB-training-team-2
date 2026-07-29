// TICKET-ADV100 — theme toggle persisted to localStorage
document.addEventListener('DOMContentLoaded', () => {
  const btn = document.getElementById('theme-toggle');
  if (!btn) return;

  const updateAria = (theme) => {
    btn.setAttribute('aria-pressed', theme === 'dark' ? 'true' : 'false');
  };

  // Set initial state
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  updateAria(current);

  btn.addEventListener('click', () => {
    const active = document.documentElement.getAttribute('data-theme') || 'light';
    const next = active === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('reconx-theme', next);
    updateAria(next);
  });
});

