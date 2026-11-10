import { api } from '../compartilhado/api.js';
import { formatCurrencyBRL } from '../compartilhado/formatters.js';

const message = document.getElementById('cartMessage');
const loading = document.getElementById('cartLoading');
const errorState = document.getElementById('cartError');
const empty = document.getElementById('cartEmpty');
const content = document.getElementById('cartContent');
const items = document.getElementById('cartItems');
let pending = false;

function show(element) {
  for (const state of [loading, errorState, empty, content]) state.hidden = state !== element;
}

function render(cart) {
  items.replaceChildren();
  if (cart.vazio) { show(empty); return; }
  for (const item of cart.itens) {
    const article = document.createElement('article');
    const title = document.createElement('h2');
    title.textContent = item.nome;
    const description = document.createElement('p');
    description.textContent = item.disponivel
      ? `${formatCurrencyBRL(item.precoUnitario)} por unidade. Subtotal: ${formatCurrencyBRL(item.subtotal)}`
      : item.motivoBloqueio === 'PRODUTO_NAO_ENCONTRADO'
        ? 'Produto não encontrado. Remova este item.' : 'Produto indisponível. Remova este item.';
    const label = document.createElement('label');
    label.textContent = `Quantidade de ${item.nome}`;
    const quantity = document.createElement('input');
    quantity.type = 'number'; quantity.min = '1'; quantity.max = '99'; quantity.step = '1';
    quantity.value = item.quantidade; quantity.disabled = !item.disponivel;
    label.append(quantity);
    const update = document.createElement('button');
    update.type = 'button'; update.textContent = 'Alterar quantidade'; update.disabled = !item.disponivel;
    update.addEventListener('click', () => {
      const value = Number(quantity.value);
      if (!Number.isInteger(value) || value < 1 || value > 99) {
        message.textContent = 'Informe uma quantidade inteira de 1 a 99.'; quantity.focus(); return;
      }
      mutate(`/api/carrinho/itens/${item.produtoId}`, 'PUT', { quantidade: value });
    });
    const remove = document.createElement('button');
    remove.type = 'button'; remove.textContent = 'Remover';
    remove.addEventListener('click', () => mutate(`/api/carrinho/itens/${item.produtoId}`, 'DELETE'));
    article.append(title, description, label, update, remove);
    items.append(article);
  }
  document.getElementById('cartSubtotal').textContent = formatCurrencyBRL(cart.subtotal);
  document.getElementById('blockedNotice').hidden = !cart.possuiItensBloqueados;
  const checkoutWrapper = document.getElementById('checkoutActionWrapper');
  if (checkoutWrapper) checkoutWrapper.hidden = cart.possuiItensBloqueados;
  show(content);
}

async function load() {
  show(loading);
  try { render(await api('/api/carrinho')); }
  catch (error) { message.textContent = error.message; show(errorState); }
}

async function mutate(path, method, body) {
  if (pending) return;
  pending = true;
  message.textContent = '';
  for (const button of items.querySelectorAll('button')) button.disabled = true;
  try {
    const result = await api(path, {
      method, ...(body ? { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) } : {})
    });
    if (method === 'DELETE') await load(); else render(result);
  } catch (error) {
    message.textContent = `${error.message} Consulte o estado atualizado antes de tentar novamente.`;
    await load();
  } finally { pending = false; }
}

document.getElementById('retryCart').addEventListener('click', load);
load();
