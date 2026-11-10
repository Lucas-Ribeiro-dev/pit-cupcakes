import { api } from '../compartilhado/api.js';
import {
  formatCurrencyBRL,
  formatDateTimeLocal,
  formatOrderMode,
  formatOrderStatus,
  formatSimulatedPayment
} from '../compartilhado/formatters.js';

const message = document.getElementById('checkoutMessage');
const loading = document.getElementById('checkoutLoading');
const emptyState = document.getElementById('checkoutEmpty');
const blockedState = document.getElementById('checkoutBlocked');
const mainSection = document.getElementById('checkoutMain');
const revisionForm = document.getElementById('revisionForm');
const pickupInfo = document.getElementById('pickupInfo');
const deliverySelection = document.getElementById('deliverySelection');
const addressListContainer = document.getElementById('checkoutAddressList');
const revisionSummarySection = document.getElementById('revisionSummarySection');
const confirmForm = document.getElementById('confirmForm');
const resultSection = document.getElementById('orderResultSection');

let addresses = [];
let activeRevision = null;
let pendingNetworkConfirmation = false;
let busy = false;

function redirectLogin() {
  location.replace('/paginas/conta/login.html?destino=checkout');
}

function showOnly(target) {
  for (const el of [loading, emptyState, blockedState, mainSection]) {
    el.hidden = el !== target;
  }
}

function invalidateActiveRevision(reason) {
  if (pendingNetworkConfirmation) return;
  activeRevision = null;
  revisionSummarySection.hidden = true;
  if (reason) message.textContent = reason;
}

function renderAddressChoices() {
  addressListContainer.replaceChildren();
  if (!addresses.length) {
    const emptyMsg = document.createElement('p');
    emptyMsg.textContent = 'Você ainda não possui endereços cadastrados. Cadastre um endereço em Meus endereços ou escolha retirada gratuita.';
    addressListContainer.append(emptyMsg);
    return;
  }
  const fieldset = document.createElement('fieldset');
  const legend = document.createElement('legend');
  legend.textContent = 'Selecione o endereço de entrega';
  fieldset.append(legend);
  addresses.forEach((addr, idx) => {
    const label = document.createElement('label');
    const radio = document.createElement('input');
    radio.type = 'radio';
    radio.name = 'enderecoId';
    radio.value = String(addr.id);
    if (idx === 0) radio.checked = true;
    radio.addEventListener('change', () => invalidateActiveRevision('Endereço alterado. Gere uma nova revisão do pedido.'));
    label.append(
      radio,
      document.createTextNode(
        ` ${addr.destinatario} — ${addr.logradouro}, ${addr.numero}${addr.complemento ? ' (' + addr.complemento + ')' : ''}, ${addr.bairro}, ${addr.cidade}/${addr.uf}, CEP ${addr.cep}`
      )
    );
    fieldset.append(label);
  });
  addressListContainer.append(fieldset);
}

function updateModalityVisibility() {
  const mode = revisionForm.elements.modalidade.value;
  pickupInfo.hidden = mode !== 'RETIRADA';
  deliverySelection.hidden = mode !== 'ENTREGA';
}

revisionForm.elements.modalidade.forEach(radio => {
  radio.addEventListener('change', () => {
    updateModalityVisibility();
    invalidateActiveRevision('Modalidade alterada. Gere uma nova revisão do pedido.');
  });
});

function renderRevision(revision) {
  activeRevision = revision;
  pendingNetworkConfirmation = false;
  document.getElementById('revisionIdDisplay').textContent = revision.revisaoId;
  document.getElementById('revisionExpiresDisplay').textContent = formatDateTimeLocal(revision.expiraEm);
  document.getElementById('revisionModeDisplay').textContent = formatOrderMode(revision.modalidade);

  const addrDisplay = document.getElementById('revisionAddressDisplay');
  if (revision.modalidade === 'ENTREGA' && revision.endereco) {
    const a = revision.endereco;
    addrDisplay.textContent = `Endereço de entrega: ${a.destinatario} — ${a.logradouro}, ${a.numero}${a.complemento ? ' (' + a.complemento + ')' : ''}, ${a.bairro}, ${a.cidade}/${a.uf}, CEP ${a.cep}`;
    addrDisplay.hidden = false;
  } else {
    addrDisplay.textContent = '';
    addrDisplay.hidden = true;
  }

  const itemsContainer = document.getElementById('revisionItemsList');
  itemsContainer.replaceChildren();
  for (const item of revision.itens) {
    const article = document.createElement('article');
    const p = document.createElement('p');
    p.textContent = `${item.nome} — ${item.quantidade} x ${formatCurrencyBRL(item.precoUnitario)} = ${formatCurrencyBRL(item.subtotal)}`;
    article.append(p);
    itemsContainer.append(article);
  }

  document.getElementById('revisionSubtotalDisplay').textContent = formatCurrencyBRL(revision.subtotal);
  document.getElementById('revisionShippingDisplay').textContent = formatCurrencyBRL(revision.frete);
  document.getElementById('revisionTotalDisplay').textContent = formatCurrencyBRL(revision.total);
  revisionSummarySection.hidden = false;
  resultSection.hidden = true;
}

function describeSpecificError(error) {
  switch (error.code) {
    case 'CARRINHO_VAZIO':
      return 'Seu carrinho está vazio. Adicione produtos antes de continuar.';
    case 'ITEM_BLOQUEADO':
    case 'PRODUTO_INDISPONIVEL':
      return `${error.message} Remova os itens indisponíveis no carrinho antes de prosseguir.`;
    case 'FORA_DA_AREA_DE_ENTREGA':
    case 'ENTREGA_NAO_CONFIGURADA':
      return `${error.message} Você pode selecionar a modalidade de retirada gratuita na loja.`;
    case 'REVISAO_EXPIRADA':
      return 'A revisão expirou após 10 minutos. Clique em "Gerar revisão do pedido" para atualizar os valores.';
    case 'REVISAO_DESATUALIZADA':
    case 'ENDERECO_DESATUALIZADO':
      return `${error.message} Clique em "Gerar revisão do pedido" para revisar os valores atualizados.`;
    case 'CONFIRMACAO_DIVERGENTE':
      return `${error.message} Gere uma nova revisão se desejar realizar outra tentativa.`;
    default:
      return error.message || 'Não foi possível concluir a operação.';
  }
}

revisionForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy) return;
  busy = true;
  message.textContent = '';
  resultSection.hidden = true;
  const reviewButton = document.getElementById('reviewButton');
  reviewButton.disabled = true;

  try {
    const mode = revisionForm.elements.modalidade.value;
    let payload;
    if (mode === 'RETIRADA') {
      payload = { modalidade: 'RETIRADA' };
    } else {
      const selectedId = Number(revisionForm.elements.enderecoId?.value);
      const selectedAddress = addresses.find(a => a.id === selectedId);
      if (!selectedAddress) {
        message.textContent = 'Selecione um endereço cadastrado para entrega ou escolha retirada gratuita.';
        return;
      }
      payload = {
        modalidade: 'ENTREGA',
        enderecoId: selectedAddress.id,
        versaoEndereco: selectedAddress.versao
      };
    }

    const revision = await api('/api/conta/checkout/revisao', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    renderRevision(revision);
    message.textContent = 'Revisão gerada com sucesso. Confira os valores e escolha o resultado do pagamento simulado.';
  } catch (error) {
    if (error.status === 401) {
      redirectLogin();
      return;
    }
    invalidateActiveRevision();
    message.textContent = describeSpecificError(error);
    if (error.code === 'ENDERECO_DESATUALIZADO' || error.code === 'ENDERECO_NAO_ENCONTRADO') {
      addresses = await api('/api/conta/enderecos').catch(() => addresses);
      renderAddressChoices();
    }
  } finally {
    reviewButton.disabled = false;
    busy = false;
  }
});

confirmForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !activeRevision) return;
  busy = true;
  message.textContent = '';
  const confirmButton = document.getElementById('confirmButton');
  const reviewButton = document.getElementById('reviewButton');
  confirmButton.disabled = true;
  reviewButton.disabled = true;

  const simulatedResult = confirmForm.elements.resultadoSimulado.value;
  const revisionIdToConfirm = activeRevision.revisaoId;

  try {
    const order = await api('/api/conta/pedidos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        revisaoId: revisionIdToConfirm,
        resultadoSimulado: simulatedResult
      })
    });

    pendingNetworkConfirmation = false;
    activeRevision = null;
    revisionSummarySection.hidden = true;

    const summaryEl = document.getElementById('orderResultSummary');
    const detailsEl = document.getElementById('orderResultDetails');
    const actionsEl = document.getElementById('orderResultActions');
    actionsEl.replaceChildren();

    const detailLink = document.createElement('a');
    detailLink.href = `/paginas/conta/pedido.html?id=${encodeURIComponent(order.id)}`;
    detailLink.textContent = `Ver detalhes do pedido #${order.id}`;

    const ordersLink = document.createElement('a');
    ordersLink.href = '/paginas/conta/pedidos.html';
    ordersLink.textContent = 'Ir para Meus pedidos';

    if (order.resultadoSimulado === 'APROVADO') {
      mainSection.hidden = true;
      resultSection.className = 'order-result-box result-box-approved';
      summaryEl.textContent = `Pedido #${order.id} confirmado com pagamento simulado aprovado!`;
      detailsEl.textContent = `Status de atendimento: ${formatOrderStatus(order.statusAtendimento)} | Modalidade: ${formatOrderMode(order.modalidade)} | Total: ${formatCurrencyBRL(order.total)}. Seu carrinho foi limpo.`;
      actionsEl.append(detailLink, document.createTextNode(' | '), ordersLink);
      message.textContent = `Pedido #${order.id} registrado com sucesso.`;
    } else {
      resultSection.className = 'order-result-box result-box-refused';
      summaryEl.textContent = `Pedido #${order.id} registrado com pagamento simulado recusado (${formatSimulatedPayment(order.resultadoSimulado)}).`;
      detailsEl.textContent = `Status do pedido: ${formatOrderStatus(order.statusAtendimento)}. Seu carrinho foi mantido para que você possa gerar uma nova revisão e tentar novamente.`;
      actionsEl.append(detailLink, document.createTextNode(' | '), ordersLink);
      message.textContent = 'Pagamento simulado recusado. Gere uma nova revisão acima se desejar confirmar um novo pedido.';
    }
    resultSection.hidden = false;
  } catch (error) {
    if (error.status === 401) {
      redirectLogin();
      return;
    }
    // Se ocorreu falha de rede sem resposta HTTP, preserva a chave revisaoId para repeticao explicita segura
    if (!error.status) {
      pendingNetworkConfirmation = true;
      message.textContent = `${error.message} A chave da revisão (${revisionIdToConfirm}) foi preservada. Você pode clicar em "Confirmar pedido" novamente para reenviar a mesma confirmação sem duplicar o pedido, ou consultar Meus pedidos.`;
      return;
    }
    pendingNetworkConfirmation = false;
    if (error.code === 'REVISAO_EXPIRADA' || error.code === 'REVISAO_DESATUALIZADA' || error.code === 'CONFIRMACAO_DIVERGENTE') {
      activeRevision = null;
      revisionSummarySection.hidden = true;
    }
    message.textContent = describeSpecificError(error);
  } finally {
    confirmButton.disabled = false;
    reviewButton.disabled = false;
    busy = false;
  }
});

async function initCheckout() {
  showOnly(loading);
  resultSection.hidden = true;
  revisionSummarySection.hidden = true;
  message.textContent = '';
  try {
    await api('/api/autenticacao/sessao');
    const [cart, savedAddresses] = await Promise.all([
      api('/api/carrinho'),
      api('/api/conta/enderecos')
    ]);
    addresses = savedAddresses;
    if (cart.vazio) {
      showOnly(emptyState);
      return;
    }
    if (cart.possuiItensBloqueados) {
      showOnly(blockedState);
      return;
    }
    renderAddressChoices();
    updateModalityVisibility();
    showOnly(mainSection);
  } catch (error) {
    if (error.status === 401) {
      redirectLogin();
      return;
    }
    message.textContent = error.message;
  }
}

window.addEventListener('pageshow', initCheckout);
window.addEventListener('pagehide', () => {
  mainSection.hidden = true;
  revisionSummarySection.hidden = true;
  resultSection.hidden = true;
});

