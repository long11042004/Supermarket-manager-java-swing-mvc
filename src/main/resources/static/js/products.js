document.addEventListener('DOMContentLoaded', () => {
  const countBadge = document.querySelector('.badge.bg-primary.fs-6');
  if (countBadge) {
    countBadge.title = 'Current product count';
  }

  const showFormButton = document.getElementById('show-product-form');
  const hideFormButton = document.getElementById('hide-product-form');
  const productFormCard = document.getElementById('product-form-card');

  const toggleProductForm = (show) => {
    if (!productFormCard) {
      return;
    }

    productFormCard.classList.toggle('form-hidden', !show);
    if (showFormButton) {
      showFormButton.classList.toggle('d-none', show);
    }
    if (hideFormButton) {
      hideFormButton.classList.toggle('d-none', !show);
    }
  };

  const isEditing = productFormCard && productFormCard.dataset.editing === 'true';
  if (productFormCard) {
    toggleProductForm(Boolean(isEditing) || !productFormCard.classList.contains('form-hidden'));
  }

  if (showFormButton) {
    showFormButton.addEventListener('click', () => toggleProductForm(true));
  }

  if (hideFormButton) {
    hideFormButton.addEventListener('click', () => toggleProductForm(false));
  }

  document.querySelectorAll('form').forEach((form) => {
    form.addEventListener('submit', (event) => {
      const submitButton = form.querySelector('button[type="submit"]');
      if (!submitButton) {
        return;
      }

      const confirmMessage = submitButton.dataset.confirmMessage || 'Are you sure?';
      if (form.getAttribute('method')?.toLowerCase() === 'post' && form.action.includes('/delete')) {
        if (!window.confirm(confirmMessage)) {
          event.preventDefault();
          return;
        }
      }

      const savingMessage = submitButton.dataset.savingMessage || 'Saving...';
      submitButton.disabled = true;
      submitButton.textContent = savingMessage;
    });
  });
});
