document.addEventListener('DOMContentLoaded', () => {
  const productFilterForm = document.getElementById('product-filter-form');
  if (productFilterForm) {
    const keywordInput = productFilterForm.querySelector('input[name="keyword"]');
    const categorySelect = productFilterForm.querySelector('select[name="category"]');
    let searchTimeout;

    if (keywordInput) {
      keywordInput.addEventListener('input', () => {
        window.clearTimeout(searchTimeout);
        searchTimeout = window.setTimeout(() => productFilterForm.requestSubmit(), 300);
      });
    }

    if (categorySelect) {
      categorySelect.addEventListener('change', () => {
        window.clearTimeout(searchTimeout);
        productFilterForm.requestSubmit();
      });
    }
  }

  const countBadge = document.querySelector('.badge.bg-primary.fs-6');
  if (countBadge) {
    countBadge.title = 'Current product count';
  }

  const openProductFormButton = document.getElementById('show-product-form');
  const closeProductFormButton = document.getElementById('hide-product-form');
  const productFormDialog = document.getElementById('product-form-dialog');

  if (openProductFormButton && productFormDialog) {
    openProductFormButton.addEventListener('click', () => {
      if (!productFormDialog.open) {
        productFormDialog.showModal();
      }
    });
  }

  if (closeProductFormButton && productFormDialog) {
    closeProductFormButton.addEventListener('click', () => productFormDialog.close());
  }

  if (productFormDialog) {
    if (productFormDialog.dataset.editing === 'true' && !productFormDialog.open) {
      productFormDialog.showModal();
    }

    productFormDialog.addEventListener('click', (event) => {
      if (event.target === productFormDialog) {
        productFormDialog.close();
      }
    });
  }

  document.querySelectorAll('[data-product-detail-target]').forEach((trigger) => {
    const dialog = document.getElementById(trigger.dataset.productDetailTarget);
    if (!dialog) {
      return;
    }

    trigger.addEventListener('click', () => {
      if (!dialog.open) {
        dialog.showModal();
      }
    });
  });

  document.querySelectorAll('.product-detail-dialog').forEach((dialog) => {
    dialog.querySelector('[data-close-product-detail]')?.addEventListener('click', () => dialog.close());
    dialog.addEventListener('click', (event) => {
      if (event.target === dialog) {
        dialog.close();
      }
    });

    if (dialog.dataset.autoOpen === 'true' && !dialog.open) {
      dialog.showModal();
    }
  });

  const openCartButton = document.getElementById('open-cart-button');
  const closeCartButton = document.getElementById('close-cart-button');
  const cartDialog = document.getElementById('cart-dialog');

  if (openCartButton && cartDialog) {
    openCartButton.addEventListener('click', () => {
      if (!cartDialog.open) {
        cartDialog.showModal();
      }
    });

    cartDialog.addEventListener('click', (event) => {
      if (event.target === cartDialog) {
        cartDialog.close();
      }
    });
  }

  if (cartDialog?.dataset.autoOpen === 'true' && !cartDialog.open) {
    cartDialog.showModal();
  }

  if (closeCartButton && cartDialog) {
    closeCartButton.addEventListener('click', () => cartDialog.close());
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
