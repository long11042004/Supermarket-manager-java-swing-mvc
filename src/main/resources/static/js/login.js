document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('form');
  if (!form) return;

  const submitButton = form.querySelector('button[type="submit"]');
  if (!submitButton) return;

  form.addEventListener('submit', () => {
    submitButton.disabled = true;
    submitButton.textContent = 'Đang đăng nhập...';
  });
});
