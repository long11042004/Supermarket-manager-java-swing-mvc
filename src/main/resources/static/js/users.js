document.addEventListener('DOMContentLoaded', () => {
  const searchForm = document.querySelector('form[method="get"][action="/users"]');
  if (searchForm) {
    const input = searchForm.querySelector('input[name="keyword"]');
    if (input) {
      input.addEventListener('input', () => {
        if (input.value.trim() === '') {
          searchForm.submit();
        }
      });
    }
  }

  document.querySelectorAll('form').forEach((form) => {
    if (!form.action.includes('/users/')) {
      return;
    }

    form.addEventListener('submit', () => {
      const submitButton = form.querySelector('button[type="submit"]');
      if (submitButton) {
        submitButton.disabled = true;
        submitButton.textContent = submitButton.dataset.savingMessage || submitButton.textContent;
      }
    });
  });
});
