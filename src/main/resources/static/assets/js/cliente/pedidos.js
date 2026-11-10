import { api } from '../compartilhado/api.js';
import {
  formatCurrencyBRL,
  formatDateTimeLocal,
  formatOrderMode,
  formatOrderStatus,
  formatSimulatedPayment
} from '../compartilhado/formatters.js';

const message = document.getElementById('ordersMessage');
const loading = document.getElementById('ordersLoading');
const empty = document.getElementById('ordersEmpty');
const listSection = document.getElementById('ordersListSection');
const listContainer = document.getElementById('ordersList');

function showOnly(target) {
  for (const el of [loading, empty, listSection]) el.hidden = el !== target;
}

async function loadOrders() {
  showOnly(loading);
  message.textContent = '';
  listContainer.replaceChildren();
  try {
    await api('/api/autenticacao/sessao');
    const orders = await api('/api/conta/pedidos');
    if (!orders.length) {
      showOnly(empty);
      return;
    }
    for (const order of orders) {
      const article = document.createElement('article');
      const title = document.createElement('h2');
      title.textContent = `Pedido #${order.id}`;

      const info = document.createElement('p');
      info.textContent = `Data: ${formatDateTimeLocal(order.criadoEm)} | Modalidade: ${formatOrderMode(order.modalidade)} | Total: ${formatCurrencyBRL(order.total)}`;

      const status = document.createElement('p');
      status.textContent = `Atendimento: ${formatOrderStatus(order.statusAtendimento)} | Pagamento: ${formatSimulatedPayment(order.resultadoSimulado)}`;

      const linkP = document.createElement('p');
      const link = document.createElement('a');
      link.href = `/paginas/conta/pedido.html?id=${encodeURIComponent(order.id)}`;
      link.textContent = `Ver detalhes do pedido #${order.id}`;
      linkP.append(link);

      article.append(title, info, status, linkP);
      listContainer.append(article);
    }
    showOnly(listSection);
  } catch (error) {
    if (error.status === 401) {
      location.replace('/paginas/conta/login.html?destino=pedidos');
      return;
    }
    loading.hidden = true;
    message.textContent = error.message;
  }
}

window.addEventListener('pageshow', loadOrders);
window.addEventListener('pagehide', () => {
  listSection.hidden = true;
  listContainer.replaceChildren();
});

