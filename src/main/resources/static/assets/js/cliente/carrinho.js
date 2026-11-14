import { api } from '../compartilhado/api.js';
import { formatCurrencyBRL, resolveProductImage, FALLBACK_IMAGE_PATH } from '../compartilhado/formatters.js';

const message = document.getElementById('cartMessage');
const loading = document.getElementById('cartLoading');
const errorState = document.getElementById('cartError');
const empty = document.getElementById('cartEmpty');
const content = document.getElementById('cartContent');
const items = document.getElementById('cartItems');
let pending = false;
let authenticated = false;

function show(element) {
  for (const state of [loading, errorState, empty, content]) state.hidden = state !== element;
}

function syncQuantityButtons(input, decrease, increase) {
  const value = Number(input.value);
  const valid = Number.isInteger(value);
  decrease.disabled = !valid || value <= Number(input.min);
  increase.disabled = !valid || value >= Number(input.max);
}

function createQuantityControl(item, article) {
  const group = document.createElement('div');
  group.className = 'cart-quantity-group';

  const label = document.createElement('label');
  label.className = 'cart-quantity-label';
  label.textContent = 'Quantidade';

  const control = document.createElement('div');
  control.className = 'quantity-control';

  const input = document.createElement('input');
  input.id = `cart-quantity-${item.produtoId}`;
  input.type = 'number';
  input.min = '1';
  input.max = '99';
  input.step = '1';
  input.value = item.quantidade;
  input.inputMode = 'numeric';
  input.disabled = !item.disponivel;
  input.setAttribute('aria-label', `Quantidade de ${item.nome}`);
  label.htmlFor = input.id;

  const decrease = document.createElement('button');
  decrease.type = 'button';
  decrease.className = 'quantity-button';
  decrease.textContent = '−';
  decrease.setAttribute('aria-label', `Diminuir quantidade de ${item.nome}`);
  decrease.disabled = !item.disponivel;
  decrease.addEventListener('click', () => {
    const value = Number(input.value);
    if (Number.isInteger(value) && value > Number(input.min)) input.value = value - 1;
    syncQuantityButtons(input, decrease, increase);
  });

  const increase = document.createElement('button');
  increase.type = 'button';
  increase.className = 'quantity-button';
  increase.textContent = '+';
  increase.setAttribute('aria-label', `Aumentar quantidade de ${item.nome}`);
  increase.disabled = !item.disponivel;
  increase.addEventListener('click', () => {
    const value = Number(input.value);
    if (Number.isInteger(value) && value < Number(input.max)) input.value = value + 1;
    syncQuantityButtons(input, decrease, increase);
  });

  input.addEventListener('input', () => syncQuantityButtons(input, decrease, increase));
  control.append(decrease, input, increase);

  const update = document.createElement('button');
  update.type = 'button';
  update.className = 'cart-update-button';
  update.textContent = 'Atualizar';
  update.disabled = !item.disponivel;
  update.addEventListener('click', () => {
    const value = Number(input.value);
    const feedback = article.querySelector('.cart-item-message');
    if (!Number.isInteger(value) || value < 1 || value > 99) {
      feedback.textContent = 'Informe uma quantidade inteira entre 1 e 99.';
      input.focus();
      return;
    }
    mutate(`/api/carrinho/itens/${item.produtoId}`, 'PUT', { quantidade: value }, feedback);
  });

  group.append(label, control, update);
  if (item.disponivel) syncQuantityButtons(input, decrease, increase);
  return group;
}

function createItem(item) {
  const article = document.createElement('article');
  article.className = 'cart-item';
  article.dataset.productId = item.produtoId;

  const image = document.createElement('img');
  image.className = 'cart-item-image';
  image.src = resolveProductImage(item);
  image.alt = item.nome || 'Cupcake';
  image.onerror = () => { image.src = FALLBACK_IMAGE_PATH; };

  const details = document.createElement('div');
  details.className = 'cart-item-details';

  const title = document.createElement('h2');
  title.textContent = item.nome;

  const unitPrice = document.createElement('p');
  unitPrice.className = 'cart-item-unit-price';
  unitPrice.textContent = item.disponivel
    ? `${formatCurrencyBRL(item.precoUnitario)} por unidade`
    : item.motivoBloqueio === 'PRODUTO_NAO_ENCONTRADO'
      ? 'Produto não encontrado. Remova este item.'
      : 'Produto indisponível. Remova este item.';

  const controls = createQuantityControl(item, article);
  const remove = document.createElement('button');
  remove.type = 'button';
  remove.className = 'cart-remove-button';
  remove.textContent = 'Remover';
  remove.addEventListener('click', () => mutate(`/api/carrinho/itens/${item.produtoId}`, 'DELETE', undefined, article.querySelector('.cart-item-message'), article));

  const itemActions = document.createElement('div');
  itemActions.className = 'cart-item-actions';
  itemActions.append(controls, remove);

  const feedback = document.createElement('p');
  feedback.className = 'cart-item-message';
  feedback.setAttribute('role', 'status');
  feedback.setAttribute('aria-live', 'polite');

  details.append(title, unitPrice, itemActions, feedback);

  const subtotal = document.createElement('div');
  subtotal.className = 'cart-item-subtotal';
  const subtotalLabel = document.createElement('span');
  subtotalLabel.textContent = 'Subtotal do item';
  const subtotalValue = document.createElement('strong');
  subtotalValue.textContent = formatCurrencyBRL(item.subtotal);
  subtotal.append(subtotalLabel, subtotalValue);

  article.append(image, details, subtotal);
  return article;
}

function render(cart, sessionState = authenticated) {
  authenticated = sessionState;
  items.replaceChildren();
  window.dispatchEvent(new CustomEvent('cart:updated', { detail: cart }));
  if (cart.vazio) { show(empty); return; }

  for (const item of cart.itens) items.appendChild(createItem(item));

  document.getElementById('cartSubtotal').textContent = formatCurrencyBRL(cart.subtotal);
  document.getElementById('blockedNotice').hidden = !cart.possuiItensBloqueados;
  const checkoutWrapper = document.getElementById('checkoutActionWrapper');
  const visitorActions = document.getElementById('visitorActions');
  checkoutWrapper.hidden = cart.possuiItensBloqueados || !authenticated;
  visitorActions.hidden = cart.possuiItensBloqueados || authenticated;
  show(content);
}

async function readSession() {
  try {
    await api('/api/autenticacao/sessao');
    return true;
  } catch {
    return false;
  }
}

async function load() {
  show(loading);
  try {
    const [cart, sessionState] = await Promise.all([api('/api/carrinho'), readSession()]);
    render(cart, sessionState);
    return cart;
  } catch (error) {
    message.textContent = error.message;
    show(errorState);
    return null;
  }
}

async function mutate(path, method, body, feedback, article) {
  if (pending) return;
  pending = true;
  message.textContent = '';
  if (feedback) feedback.textContent = method === 'PUT' ? 'Atualizando quantidade...' : 'Removendo item...';
  for (const button of items.querySelectorAll('button')) button.disabled = true;
  try {
    const result = await api(path, {
      method, ...(body ? { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) } : {})
    });
    if (method === 'DELETE') {
      const updated = await load();
      if (updated?.vazio) empty.querySelector('a')?.focus();
      else items.querySelector('button')?.focus();
    } else {
      render(result);
    }
  } catch (error) {
    if (feedback) feedback.textContent = error.message;
    else message.textContent = error.message;
    await load();
  } finally {
    pending = false;
  }
}

document.getElementById('retryCart').addEventListener('click', load);
load();
