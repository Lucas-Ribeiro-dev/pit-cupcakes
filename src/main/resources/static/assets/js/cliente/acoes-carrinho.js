import { api } from '../compartilhado/api.js';

let pending = false;

export async function addToCart(productId, quantity, button, message) {
  if (pending) {
    message.className = 'cart-feedback cart-feedback-error';
    message.textContent = 'Aguarde a inclusão em andamento.';
    return;
  }
  pending = true;
  button.disabled = true;
  message.className = '';
  message.replaceChildren();
  try {
    await api('/api/carrinho/itens', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ produtoId: productId, quantidade: quantity })
    });
    message.className = 'cart-feedback cart-feedback-success';
    message.setAttribute('aria-live', 'polite');
    const textSpan = document.createElement('span');
    textSpan.textContent = 'Produto adicionado ao carrinho.';
    const cartLink = document.createElement('a');
    cartLink.href = '/paginas/compra/carrinho.html';
    cartLink.className = 'cart-feedback-link';
    cartLink.textContent = 'Conferir itens';
    message.append(textSpan, document.createTextNode(' '), cartLink);
  } catch (error) {
    message.className = 'cart-feedback cart-feedback-error';
    message.setAttribute('aria-live', 'polite');
    message.textContent = `${error.message} Consulte o carrinho antes de tentar adicionar novamente.`;
  } finally {
    button.disabled = false;
    pending = false;
  }
}
