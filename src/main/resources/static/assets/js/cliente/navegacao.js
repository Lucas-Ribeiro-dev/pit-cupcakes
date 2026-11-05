import { api, logout } from '../compartilhado/api.js';

const navigation = document.querySelector('#accountNavigation');
export async function updateNavigation() {
  if (!navigation) return;
  navigation.replaceChildren();
  try {
    await api('/api/autenticacao/sessao');
    const account = document.createElement('a');
    account.href = '/paginas/conta/minha-conta.html';
    account.textContent = 'Minha conta';
    const exit = document.createElement('button');
    exit.type = 'button';
    exit.textContent = 'Sair';
    exit.addEventListener('click', async () => {
      exit.disabled = true;
      try {
        await logout();
        location.replace('/paginas/conta/login.html?saida=1');
      } catch (error) {
        document.querySelector('#navigationMessage').textContent = error.message;
        exit.disabled = false;
      }
    });
    navigation.append(account, exit);
  } catch (error) {
    if (error.status !== 401) {
      document.querySelector('#navigationMessage').textContent = error.message;
      return;
    }
    for (const [label, page] of [['Entrar', 'login'], ['Criar conta', 'cadastro']]) {
      const link = document.createElement('a');
      link.href = `/paginas/conta/${page}.html`;
      link.textContent = label;
      navigation.append(link);
    }
  }
}
updateNavigation();
