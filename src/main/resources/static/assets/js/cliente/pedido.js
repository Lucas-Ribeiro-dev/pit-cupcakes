import { api } from '../compartilhado/api.js';
import {
  formatCurrencyBRL,
  formatDateTimeLocal,
  formatOrderMode,
  formatOrderStatus,
  formatSimulatedPayment
} from '../compartilhado/formatters.js';

const message = document.getElementById('orderDetailMessage');
const loading = document.getElementById('orderDetailLoading');
const content = document.getElementById('orderDetailContent');

async function loadOrderDetail() {
  loading.hidden = false;
  content.hidden = true;
  message.textContent = '';

  const rawId = new URLSearchParams(location.search).get('id');
  if (!rawId || !/^[1-9][0-9]*$/.test(rawId)) {
    loading.hidden = true;
    message.textContent = 'O identificador do pedido informado é inválido.';
    return;
  }

  try {
    await api('/api/autenticacao/sessao');
    const order = await api(`/api/conta/pedidos/${encodeURIComponent(rawId)}`);

    document.getElementById('orderDetailTitle').textContent = `Detalhes do pedido #${order.id}`;
    document.getElementById('detailId').textContent = `#${order.id}`;
    document.getElementById('detailDate').textContent = formatDateTimeLocal(order.criadoEm);
    document.getElementById('detailMode').textContent = formatOrderMode(order.modalidade);
    document.getElementById('detailStatus').textContent = formatOrderStatus(order.statusAtendimento);
    document.getElementById('detailPayment').textContent = formatSimulatedPayment(order.resultadoSimulado);

    const addrSection = document.getElementById('detailAddressSection');
    const pickupSection = document.getElementById('detailPickupSection');
    if (order.modalidade === 'ENTREGA' && order.endereco) {
      const a = order.endereco;
      document.getElementById('detailAddressText').textContent =
        `${a.destinatario} — ${a.logradouro}, ${a.numero}${a.complemento ? ' — ' + a.complemento : ''}, ${a.bairro}, ${a.cidade}/${a.uf}, CEP ${a.cep}`;
      addrSection.hidden = false;
      pickupSection.hidden = true;
    } else {
      addrSection.hidden = true;
      pickupSection.hidden = false;
    }

    const itemsList = document.getElementById('detailItemsList');
    itemsList.replaceChildren();
    for (const item of order.itens) {
      const article = document.createElement('article');
      const p = document.createElement('p');
      p.textContent = `${item.nome} — Quantidade: ${item.quantidade} | Preço unitário: ${formatCurrencyBRL(item.precoUnitario)} | Subtotal: ${formatCurrencyBRL(item.subtotal)}`;
      article.append(p);
      itemsList.append(article);
    }

    document.getElementById('detailSubtotal').textContent = formatCurrencyBRL(order.subtotal);
    document.getElementById('detailShipping').textContent = formatCurrencyBRL(order.frete);
    document.getElementById('detailTotal').textContent = formatCurrencyBRL(order.total);

    loading.hidden = true;
    content.hidden = false;
  } catch (error) {
    if (error.status === 401) {
      location.replace('/paginas/conta/login.html?destino=pedidos');
      return;
    }
    loading.hidden = true;
    message.textContent = error.message;
  }
}

window.addEventListener('pageshow', loadOrderDetail);
window.addEventListener('pagehide', () => {
  content.hidden = true;
});

