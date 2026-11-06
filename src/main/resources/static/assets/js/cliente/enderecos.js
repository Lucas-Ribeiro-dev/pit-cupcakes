import { api } from '../compartilhado/api.js';

const form = document.querySelector('#addressForm');
const list = document.querySelector('#addressList');
const message = document.querySelector('#formMessage');
const postalMessage = document.querySelector('#postalMessage');
const shippingMessage = document.querySelector('#shippingMessage');
const content = document.querySelector('#privateContent');
const fields = ['destinatario', 'cep', 'logradouro', 'numero', 'complemento', 'bairro', 'cidade', 'uf'];
let editing;
let lookupSequence = 0;
let quoteSequence = 0;
let lookupController;
let pendingPostalCode;
const revisions = Object.fromEntries(fields.map(field => [field, 0]));
const redirect = () => location.replace('/paginas/conta/login.html?destino=enderecos');
function report(error, target = message) {
  if (error.status === 401) redirect();
  else target.textContent = error.message;
}
function invalidateQuote() { quoteSequence++; shippingMessage.textContent = ''; }
function cancelLookup() {
  lookupSequence++;
  lookupController?.abort();
  pendingPostalCode = undefined;
  postalMessage.textContent = '';
}
form.addEventListener('input', event => {
  if (event.target.name in revisions) revisions[event.target.name]++;
  invalidateQuote();
  if (event.target.name === 'cep') cancelLookup();
});
function reset() {
  cancelLookup(); invalidateQuote(); editing = undefined; form.reset();
  document.querySelector('#formTitle').textContent = 'Novo endereço';
  message.textContent = '';
}
document.querySelector('#cancelButton').addEventListener('click', reset);
async function lookup() {
  const postalCode = form.elements.cep.value;
  if (!/^[0-9]{5}-?[0-9]{3}$/.test(postalCode)) {
    cancelLookup(); postalMessage.textContent = 'Informe um CEP com oito dígitos.'; return;
  }
  if (pendingPostalCode === postalCode) return;
  cancelLookup();
  pendingPostalCode = postalCode;
  const sequence = lookupSequence;
  const originalRevisions = { ...revisions };
  lookupController = new AbortController();
  postalMessage.textContent = 'Buscando CEP… Você também pode preencher manualmente.';
  try {
    const data = await api(`/api/conta/cep/${encodeURIComponent(postalCode)}`, { signal: lookupController.signal });
    if (sequence !== lookupSequence || postalCode !== form.elements.cep.value) return;
    for (const field of ['logradouro', 'bairro', 'cidade', 'uf']) {
      if (revisions[field] === originalRevisions[field]) form.elements[field].value = data[field] || '';
    }
    invalidateQuote();
    postalMessage.textContent = 'Consulta concluída. Revise os campos e complete os dados ausentes.';
  } catch (error) {
    if (sequence === lookupSequence) report(error, postalMessage);
  } finally {
    if (sequence === lookupSequence) pendingPostalCode = undefined;
  }
}
document.querySelector('#lookupButton').addEventListener('click', lookup);
form.elements.cep.addEventListener('blur', () => {
  if (/^[0-9]{5}-?[0-9]{3}$/.test(form.elements.cep.value)) lookup();
});
async function quote(address) {
  const sequence = ++quoteSequence;
  const destination = address ? `${address.logradouro}, ${address.numero}, CEP ${address.cep}: ` : '';
  shippingMessage.textContent = destination + 'Consultando frete…';
  try {
    const data = await api('/api/conta/frete/cotacao', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(address ? { modalidade: 'ENTREGA', enderecoId: address.id } : { modalidade: 'RETIRADA' })
    });
    if (sequence !== quoteSequence) return;
    shippingMessage.textContent = address
      ? `Frete para ${address.logradouro}, ${address.numero} — CEP ${address.cep}: R$ ${data.valor.replace('.', ',')}. Cotação provisória.`
      : 'Retirada gratuita: R$ 0,00.';
  } catch (error) {
    if (sequence === quoteSequence) {
      report(error, shippingMessage);
      if (error.status !== 401) shippingMessage.textContent = destination + shippingMessage.textContent;
    }
  }
}
document.querySelector('#pickupButton').addEventListener('click', () => quote());
function button(label, action) {
  const element = document.createElement('button'); element.type = 'button'; element.textContent = label;
  element.addEventListener('click', action); return element;
}
async function loadList() {
  const addresses = await api('/api/conta/enderecos');
  list.replaceChildren();
  if (!addresses.length) list.textContent = 'Nenhum endereço cadastrado.';
  for (const address of addresses) {
    const article = document.createElement('article');
    const description = document.createElement('p');
    description.textContent = `${address.destinatario}: ${address.logradouro}, ${address.numero}${address.complemento ? ' — ' + address.complemento : ''}, ${address.bairro}, ${address.cidade}/${address.uf}, CEP ${address.cep}`;
    article.append(description, button('Editar', async () => {
      try {
        const current = await api(`/api/conta/enderecos/${address.id}`);
        reset(); editing = current;
        fields.forEach(field => { form.elements[field].value = current[field] || ''; });
        document.querySelector('#formTitle').textContent = 'Editar endereço'; form.elements.destinatario.focus();
      } catch (error) { report(error); }
    }), button('Remover', async () => {
      if (!confirm('Remover este endereço do cadastro?')) return;
      invalidateQuote();
      try {
        await api(`/api/conta/enderecos/${address.id}?versao=${address.versao}`, { method: 'DELETE' });
        if (editing?.id === address.id) reset();
        message.textContent = 'Endereço removido.'; await loadList();
      } catch (error) { report(error); await loadList().catch(report); }
    }), button('Consultar entrega', () => quote(address)));
    list.append(article);
  }
}
form.addEventListener('submit', async event => {
  event.preventDefault(); cancelLookup(); invalidateQuote();
  const data = Object.fromEntries(fields.map(field => [field, form.elements[field].value]));
  if (editing) data.versao = editing.versao;
  const submit = form.querySelector('[type=submit]'); submit.disabled = true;
  try {
    await api(editing ? `/api/conta/enderecos/${editing.id}` : '/api/conta/enderecos', {
      method: editing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)
    });
    reset(); message.textContent = 'Endereço salvo. Consulte novamente o frete.'; await loadList();
  } catch (error) {
    report(error);
    if (error.status === 409) {
      message.textContent += ' Seu texto foi preservado. Copie o que precisar e use Editar na lista para carregar a versão atual.';
      await loadList().catch(report);
    }
  } finally { submit.disabled = false; }
});
window.addEventListener('pageshow', async () => {
  content.hidden = true;
  try { await api('/api/autenticacao/sessao'); await loadList(); content.hidden = false; }
  catch (error) { report(error, document.querySelector('#pageMessage')); }
});
window.addEventListener('pagehide', () => { content.hidden = true; reset(); list.replaceChildren(); });
