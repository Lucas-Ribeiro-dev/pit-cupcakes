import { api } from '../compartilhado/api.js';

const details = document.querySelector('#accountDetails');
const adminLinks = document.querySelector('#adminLinks');
async function loadAccount() {
  details.hidden = true;
  if (adminLinks) adminLinks.hidden = true;
  document.querySelector('#accountName').textContent = '';
  document.querySelector('#accountEmail').textContent = '';
  try {
    const user = await api('/api/autenticacao/sessao');
    document.querySelector('#accountName').textContent = user.nome;
    document.querySelector('#accountEmail').textContent = user.email;
    if (adminLinks) adminLinks.hidden = user.perfil !== 'ADMIN';
    details.hidden = false;
  } catch (error) {
    if (error.status === 401) location.replace('/paginas/conta/login.html');
    else document.querySelector('#formMessage').textContent = error.message;
  }
}
window.addEventListener('pageshow', loadAccount);
window.addEventListener('pagehide', () => {
  details.hidden = true;
  if (adminLinks) adminLinks.hidden = true;
  document.querySelector('#accountName').textContent = '';
  document.querySelector('#accountEmail').textContent = '';
});
