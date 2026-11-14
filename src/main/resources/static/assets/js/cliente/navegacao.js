import { api, logout } from '../compartilhado/api.js';

const navigation = document.querySelector('#accountNavigation');
const navigationMessage = document.querySelector('#navigationMessage');

function createCartLink() {
  const cart = document.createElement('a');
  cart.href = '/paginas/compra/carrinho.html';
  cart.className = 'cart-link';
  cart.setAttribute('aria-label', 'Abrir carrinho, 0 itens');
  cart.innerHTML = '<span class="cart-icon" aria-hidden="true"><svg viewBox="0 0 24 24" focusable="false"><path d="M4 5h2l1.3 9.2a2 2 0 0 0 2 1.8h7.9a2 2 0 0 0 1.9-1.5L21 8H7"/><circle cx="10" cy="19" r="1.2"/><circle cx="18" cy="19" r="1.2"/></svg></span><span class="cart-count" aria-hidden="true">0</span>';
  return cart;
}

function markCurrentLink(link) {
  if (link.pathname !== location.pathname) return;
  link.classList.add('nav-current');
  link.setAttribute('aria-current', 'page');
}

function updateCartLink(cart) {
  const link = navigation?.querySelector('.cart-link');
  if (!link) return;
  const quantity = Number(cart?.quantidadeTotal || 0);
  link.querySelector('.cart-count').textContent = String(quantity);
  link.setAttribute('aria-label', `Abrir carrinho, ${quantity} ${quantity === 1 ? 'item' : 'itens'}`);
}

async function loadCartCount() {
  try {
    updateCartLink(await api('/api/carrinho'));
  } catch {
    // O carrinho continua utilizável mesmo quando a leitura do contador falha.
  }
}

export async function updateNavigation() {
  if (!navigation) return;
  navigation.replaceChildren();
  let cartLink;
  try {
    const user = await api('/api/autenticacao/sessao');
    const account = document.createElement('a');
    account.href = '/paginas/conta/minha-conta.html';
    account.className = 'nav-account';
    account.textContent = 'Minha conta';
    markCurrentLink(account);
    navigation.append(account);
    if (user.perfil === 'ADMIN') {
      const admin = document.createElement('a');
      admin.href = '/admin/produtos.html';
      admin.textContent = 'Administração';
      navigation.append(admin);
    }
    const exit = document.createElement('button');
    exit.type = 'button';
    exit.textContent = 'Sair';
    exit.addEventListener('click', async () => {
      exit.disabled = true;
      try {
        await logout();
        location.replace('/paginas/conta/login.html?saida=1');
      } catch (error) {
        if (navigationMessage) navigationMessage.textContent = error.message;
        exit.disabled = false;
      }
    });
    navigation.append(exit);
  } catch (error) {
    if (error.status !== 401) {
      if (navigationMessage) navigationMessage.textContent = error.message;
      return;
    }
    for (const [label, page] of [['Entrar', 'login'], ['Criar conta', 'cadastro']]) {
      const link = document.createElement('a');
      link.href = `/paginas/conta/${page}.html`;
      link.textContent = label;
      link.className = label === 'Entrar' ? 'nav-login' : 'nav-signup';
      navigation.append(link);
    }
  }
  cartLink = createCartLink();
  navigation.append(cartLink);
  await loadCartCount();
}

window.addEventListener('cart:updated', (event) => updateCartLink(event.detail));
updateNavigation();
