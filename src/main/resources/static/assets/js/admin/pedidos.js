import { api } from '../compartilhado/api.js';
import {
  formatCurrencyBRL,
  formatDateTimeLocal,
  formatOrderMode,
  formatOrderStatus,
  formatSimulatedPayment
} from '../compartilhado/formatters.js';

const pageMessage = document.getElementById('adminOrdersMessage');
const unauthSection = document.getElementById('adminOrdersUnauthenticated');
const forbiddenSection = document.getElementById('adminOrdersForbidden');
const content = document.getElementById('adminOrdersContent');
const selectedSection = document.getElementById('adminSelectedOrderSection');
const selectedBody = document.getElementById('adminSelectedOrderBody');
const statusForm = document.getElementById('adminStatusForm');
const statusSelect = document.getElementById('nextStatusSelect');
const selectedNotice = document.getElementById('adminSelectedOrderNotice');
const ordersList = document.getElementById('adminOrdersList');

let activeOrder = null;
let busy = false;

function showState(target) {
  for (const el of [unauthSection, forbiddenSection, content]) {
    el.hidden = el !== target;
  }
}

function renderSelectedOrder(order) {
  activeOrder = order;
  selectedBody.replaceChildren();
  document.getElementById('selectedOrderHeading').textContent = `Atendimento do pedido #${order.id}`;

  const summary = document.createElement('p');
  summary.textContent = `Cliente: ${order.clienteNome} (${order.clienteEmail}) | Data: ${formatDateTimeLocal(order.criadoEm)} | Versão: ${order.versao}`;

  const stateInfo = document.createElement('p');
  stateInfo.textContent = `Modalidade: ${formatOrderMode(order.modalidade)} | Status atual: ${formatOrderStatus(order.statusAtendimento)} | Pagamento: ${formatSimulatedPayment(order.resultadoSimulado)}`;

  const totals = document.createElement('p');
  totals.textContent = `Subtotal: ${formatCurrencyBRL(order.subtotal)} | Frete: ${formatCurrencyBRL(order.frete)} | Total: ${formatCurrencyBRL(order.total)}`;

  selectedBody.append(summary, stateInfo, totals);

  if (order.modalidade === 'ENTREGA' && order.endereco) {
    const a = order.endereco;
    const addr = document.createElement('p');
    addr.textContent = `Endereço de entrega: ${a.destinatario} — ${a.logradouro}, ${a.numero}${a.complemento ? ' (' + a.complemento + ')' : ''}, ${a.bairro}, ${a.cidade}/${a.uf}, CEP ${a.cep}`;
    selectedBody.append(addr);
  }

  const itemsList = document.createElement('ul');
  for (const item of order.itens) {
    const li = document.createElement('li');
    li.textContent = `${item.nome} — ${item.quantidade} x ${formatCurrencyBRL(item.precoUnitario)} = ${formatCurrencyBRL(item.subtotal)}`;
    itemsList.append(li);
  }
  selectedBody.append(itemsList);

  statusSelect.replaceChildren();
  const allowed = order.proximosStatusPermitidos || [];
  if (allowed.length > 0) {
    for (const st of allowed) {
      const option = document.createElement('option');
      option.value = st;
      option.textContent = formatOrderStatus(st);
      statusSelect.append(option);
    }
    statusForm.hidden = false;
    selectedNotice.textContent = '';
  } else {
    statusForm.hidden = true;
    selectedNotice.textContent = 'Este pedido encontra-se em estado final e não recebe novas transições de atendimento.';
  }

  selectedSection.hidden = false;
}

async function loadOrders() {
  const orders = await api('/api/admin/pedidos');
  ordersList.replaceChildren();
  if (!orders.length) {
    ordersList.textContent = 'Nenhum pedido registrado até o momento.';
    return;
  }
  for (const order of orders) {
    const article = document.createElement('article');
    const title = document.createElement('h3');
    title.textContent = `Pedido #${order.id} — ${order.clienteNome} (${order.clienteEmail})`;

    const info = document.createElement('p');
    info.textContent = `Data: ${formatDateTimeLocal(order.criadoEm)} | Modalidade: ${formatOrderMode(order.modalidade)} | Status: ${formatOrderStatus(order.statusAtendimento)} | Pagamento: ${formatSimulatedPayment(order.resultadoSimulado)} | Total: ${formatCurrencyBRL(order.total)}`;

    const inspectBtn = document.createElement('button');
    inspectBtn.type = 'button';
    inspectBtn.textContent = `Gerenciar pedido #${order.id}`;
    inspectBtn.addEventListener('click', async () => {
      try {
        const detail = await api(`/api/admin/pedidos/${order.id}`);
        renderSelectedOrder(detail);
      } catch (error) {
        pageMessage.textContent = error.message;
      }
    });

    article.append(title, info, inspectBtn);
    ordersList.append(article);
  }
}

statusForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !activeOrder) return;
  busy = true;
  selectedNotice.textContent = '';
  const updateButton = document.getElementById('updateStatusButton');
  updateButton.disabled = true;

  try {
    const updated = await api(`/api/admin/pedidos/${activeOrder.id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        statusAtendimento: statusSelect.value,
        versao: activeOrder.versao
      })
    });
    renderSelectedOrder(updated);
    selectedNotice.textContent = updated.statusAtendimento === 'CANCELADO'
      ? `Pedido #${updated.id} cancelado no fluxo acadêmico (sem estorno financeiro real).`
      : `Status do pedido #${updated.id} atualizado para ${formatOrderStatus(updated.statusAtendimento)}.`;
    await loadOrders();
  } catch (error) {
    selectedNotice.textContent = error.message;
    if (error.status === 409) {
      const refreshed = await api(`/api/admin/pedidos/${activeOrder.id}`).catch(() => null);
      if (refreshed) renderSelectedOrder(refreshed);
      await loadOrders().catch(() => {});
    }
  } finally {
    updateButton.disabled = false;
    busy = false;
  }
});

async function initAdminOrders() {
  unauthSection.hidden = true;
  forbiddenSection.hidden = true;
  content.hidden = true;
  selectedSection.hidden = true;
  pageMessage.textContent = '';
  try {
    const user = await api('/api/autenticacao/sessao');
    if (user.perfil !== 'ADMIN') {
      showState(forbiddenSection);
      return;
    }
    await loadOrders();
    showState(content);
  } catch (error) {
    if (error.status === 401) {
      showState(unauthSection);
      return;
    }
    if (error.status === 403) {
      showState(forbiddenSection);
      return;
    }
    pageMessage.textContent = error.message;
  }
}

window.addEventListener('pageshow', initAdminOrders);
window.addEventListener('pagehide', () => {
  content.hidden = true;
  selectedSection.hidden = true;
  ordersList.replaceChildren();
});

