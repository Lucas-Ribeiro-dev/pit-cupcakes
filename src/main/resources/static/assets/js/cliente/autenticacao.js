import { api, refreshCsrf } from '../compartilhado/api.js';

const ALLOWED_DESTINATIONS = {
  carrinho: '/paginas/compra/carrinho.html',
  checkout: '/paginas/compra/checkout.html',
  enderecos: '/paginas/conta/enderecos.html',
  pedidos: '/paginas/conta/pedidos.html',
  'admin-produtos': '/admin/produtos.html',
  'admin-pedidos': '/admin/pedidos.html'
};

const form = document.querySelector('#accountForm');
const message = document.querySelector('#formMessage');
const registration = document.body.dataset.page === 'cadastro';
const params = new URLSearchParams(location.search);
const destination = params.get('destino');
if (params.get('saida') === '1') message.textContent = 'Sessão encerrada.';

const switchLink = document.querySelector('#loginLink');
if (switchLink && destination && ALLOWED_DESTINATIONS[destination]) {
  const baseHref = switchLink.getAttribute('href').split('?')[0];
  switchLink.href = `${baseHref}?destino=${encodeURIComponent(destination)}`;
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  message.textContent = '';
  const password = form.elements.senha.value;
  if (registration && password !== form.elements.confirmacao.value) {
    message.textContent = 'As senhas não coincidem.';
    form.elements.confirmacao.focus();
    return;
  }
  if (registration && ([...password].length < 8 || [...password].length > 64 || new TextEncoder().encode(password).length > 72)) {
    message.textContent = 'A senha deve ter de 8 a 64 caracteres Unicode e no máximo 72 bytes UTF-8.';
    form.elements.senha.focus();
    return;
  }
  const submit = form.querySelector('button[type=submit]');
  submit.disabled = true;
  try {
    if (registration) {
      await api('/api/usuarios', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nome: form.elements.nome.value, email: form.elements.email.value, senha: password })
      });
      form.reset();
      message.textContent = 'Conta criada com sucesso. Entre para continuar.';
      document.querySelector('#loginLink').focus();
    } else {
      await api('/api/autenticacao/login', {
        method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ email: form.elements.email.value, senha: password })
      });
      form.reset();
      await refreshCsrf();
      location.replace(ALLOWED_DESTINATIONS[destination] || '/paginas/conta/minha-conta.html');
    }
  } catch (error) {
    message.textContent = error.message;
    form.elements.senha.value = '';
    if (registration) form.elements.confirmacao.value = '';
  } finally {
    submit.disabled = false;
  }
});
