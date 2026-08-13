document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.nav-btn, .btn').forEach((button) => {
    button.addEventListener('mouseenter', () => {
      button.style.transform = 'translateY(-1px)';
    });

    button.addEventListener('mouseleave', () => {
      button.style.transform = 'translateY(0)';
    });
  });
});
