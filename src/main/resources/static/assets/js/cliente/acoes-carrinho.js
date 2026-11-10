import { api } from '../compartilhado/api.js';

let pending = false;

export async function addToCart(productId, quantity, button, message) {
  if (pending) { message.textContent = 'Aguarde a inclusão em andamento.'; return; }
  pending = true;
  button.disabled = true;
  message.textContent = '';
  try {
    await api('/api/carrinho/itens', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ produtoId: productId, quantidade: quantity })
    });
    message.textContent = 'Produto adicionado ao carrinho.';
  } catch (error) {
    message.textContent = `${error.message} Consulte o carrinho antes de tentar adicionar novamente.`;
  } finally {
    button.disabled = false;
    pending = false;
  }
}
