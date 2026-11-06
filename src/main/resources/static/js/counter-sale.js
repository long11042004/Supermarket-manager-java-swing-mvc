(() => {
  const lines = document.getElementById('counter-sale-lines');
  const template = document.getElementById('counter-sale-line-template');
  const addButton = document.getElementById('add-counter-sale-line');
  const searchForm = document.getElementById('counter-sale-search');
  const searchInput = searchForm?.querySelector('input[name="keyword"]');
  const feedback = document.getElementById('counter-sale-feedback');
  const itemCount = document.getElementById('counter-sale-item-count');
  const picker = document.querySelector('.product-picker');

  if (!lines || !template || !addButton || !feedback || !itemCount || !picker) {
    return;
  }

  const showFeedback = (message, isError = false) => {
    feedback.textContent = message;
    feedback.classList.toggle('is-error', isError);
    feedback.classList.toggle('is-visible', Boolean(message));
  };

  const updateItemCount = () => {
    const count = [...lines.querySelectorAll('input[type="number"]')]
      .reduce((total, input) => total + (input.closest('.counter-sale-line').querySelector('select').value ? Number(input.value) || 0 : 0), 0);
    itemCount.textContent = count === 1
      ? itemCount.dataset.singleItem
      : itemCount.dataset.multipleItems.replace('{0}', String(count));
  };

  const reindexLines = () => {
    lines.querySelectorAll('.counter-sale-line').forEach((line, index) => {
      const select = line.querySelector('select');
      const quantity = line.querySelector('input[type="number"]');
      const labels = line.querySelectorAll('label');
      const removeButton = line.querySelector('.remove-counter-sale-line');

      select.name = `productIds[${index}]`;
      select.id = `product-${index}`;
      quantity.name = `quantities[${index}]`;
      quantity.id = `quantity-${index}`;
      labels[0].htmlFor = select.id;
      labels[1].htmlFor = quantity.id;
      removeButton.setAttribute('aria-label', removeButton.title);
    });
    updateItemCount();
  };

  const addProductToOrder = (productButton) => {
    const productId = productButton.dataset.productId;
    const stock = Number(productButton.dataset.stock);
    const existingLine = [...lines.querySelectorAll('.counter-sale-line')]
      .find((line) => line.querySelector('select').value === productId);

    if (existingLine) {
      const quantityInput = existingLine.querySelector('input[type="number"]');
      const nextQuantity = Number(quantityInput.value) + 1;
      if (nextQuantity > stock) {
        showFeedback(picker.dataset.stockError.replace('{0}', productButton.dataset.productName), true);
        return;
      }
      quantityInput.value = String(nextQuantity);
    } else {
      let emptyLine = [...lines.querySelectorAll('.counter-sale-line')]
        .find((line) => !line.querySelector('select').value);
      if (!emptyLine) {
        addButton.click();
        emptyLine = lines.lastElementChild;
      }
      const select = emptyLine.querySelector('select');
      if (![...select.options].some((option) => option.value === productId)) {
        showFeedback(picker.dataset.stockError.replace('{0}', productButton.dataset.productName), true);
        return;
      }
      select.value = productId;
      emptyLine.querySelector('input[type="number"]').value = '1';
    }

    showFeedback(picker.dataset.addedMessage.replace('{0}', productButton.dataset.productName));
    updateItemCount();
  };

  addButton.addEventListener('click', () => {
    const index = lines.querySelectorAll('.counter-sale-line').length;
    const row = template.content.cloneNode(true);
    row.querySelectorAll('[name]').forEach((input) => {
      input.name = input.name.replace('__INDEX__', String(index));
    });
    lines.appendChild(row);
    reindexLines();
  });

  document.querySelectorAll('.add-counter-product').forEach((button) => {
    button.addEventListener('click', () => addProductToOrder(button));
  });

  lines.addEventListener('click', (event) => {
    const target = event.target;
    if (!(target instanceof Element)) {
      return;
    }
    const removeButton = target.closest('.remove-counter-sale-line');
    if (!removeButton) {
      return;
    }

    const row = removeButton.closest('.counter-sale-line');
    if (lines.querySelectorAll('.counter-sale-line').length === 1) {
      row.querySelector('select').selectedIndex = 0;
      row.querySelector('input[type="number"]').value = '1';
      showFeedback('');
      updateItemCount();
      return;
    }
    row.remove();
    showFeedback('');
    reindexLines();
    });

    lines.addEventListener('input', updateItemCount);
    lines.addEventListener('change', updateItemCount);

    const preserveOrderDraft = (target) => {
      const url = new URL(target, window.location.href);
      url.searchParams.delete('productIds');
      url.searchParams.delete('quantities');
      lines.querySelectorAll('.counter-sale-line').forEach((line) => {
        const productId = line.querySelector('select').value;
        const quantity = line.querySelector('input[type="number"]').value;
        if (productId) {
          url.searchParams.append('productIds', productId);
          url.searchParams.append('quantities', quantity);
        }
      });

      ['customerName', 'contactPhone', 'note', 'paymentMethod'].forEach((name) => {
        const field = document.querySelector(`[name="${name}"]`);
        if (field?.value) {
          url.searchParams.set(name, field.value);
        }
      });
      return url;
    };

    searchForm?.addEventListener('submit', (event) => {
      event.preventDefault();
      window.location.assign(preserveOrderDraft(searchForm.action + (searchInput?.value ? `?keyword=${encodeURIComponent(searchInput.value)}` : '')).href);
    });

    let searchTimeout;
    searchInput?.addEventListener('input', () => {
      window.clearTimeout(searchTimeout);
      searchTimeout = window.setTimeout(() => searchForm.requestSubmit(), 350);
    });

    document.querySelectorAll('.counter-product-page').forEach((link) => {
      link.addEventListener('click', (event) => {
        event.preventDefault();
        window.location.assign(preserveOrderDraft(link.href).href);
      });
    });

    updateItemCount();
})();
