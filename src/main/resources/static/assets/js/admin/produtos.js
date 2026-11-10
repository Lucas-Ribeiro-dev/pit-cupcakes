import { api } from '../compartilhado/api.js';
import { formatCurrencyBRL } from '../compartilhado/formatters.js';

const pageMessage = document.getElementById('adminPageMessage');
const unauthSection = document.getElementById('adminUnauthenticated');
const forbiddenSection = document.getElementById('adminForbidden');
const adminContent = document.getElementById('adminContent');
const form = document.getElementById('adminProductForm');
const formTitle = document.getElementById('productFormTitle');
const formMessage = document.getElementById('productFormMessage');
const productList = document.getElementById('adminProductList');

let editingProduct = null;
let busy = false;

function showState(target) {
  for (const el of [unauthSection, forbiddenSection, adminContent]) {
    el.hidden = el !== target;
  }
}

function resetForm() {
  editingProduct = null;
  form.reset();
  form.elements.disponivel.value = 'true';
  formTitle.textContent = 'Novo produto';
  formMessage.textContent = '';
}

document.getElementById('cancelProductButton').addEventListener('click', resetForm);

async function loadProducts() {
  const products = await api('/api/admin/produtos');
  productList.replaceChildren();
  if (!products.length) {
    productList.textContent = 'Nenhum produto cadastrado.';
    return;
  }
  for (const product of products) {
    const article = document.createElement('article');
    const title = document.createElement('h3');
    title.textContent = `#${product.id} — ${product.nome}`;

    const details = document.createElement('p');
    details.textContent = `Preço: ${formatCurrencyBRL(product.preco)} | Status: ${product.disponivel ? 'Disponível' : 'Indisponível'} | Versão: ${product.versao}`;

    const desc = document.createElement('p');
    desc.textContent = product.descricao;

    const editBtn = document.createElement('button');
    editBtn.type = 'button';
    editBtn.textContent = 'Editar';
    editBtn.addEventListener('click', async () => {
      try {
        const current = await api(`/api/admin/produtos/${product.id}`);
        editingProduct = current;
        form.elements.nome.value = current.nome;
        form.elements.descricao.value = current.descricao;
        form.elements.preco.value = current.preco;
        form.elements.imagemCaminho.value = current.imagemCaminho || '';
        form.elements.disponivel.value = current.disponivel ? 'true' : 'false';
        formTitle.textContent = `Editar produto #${current.id}`;
        formMessage.textContent = '';
        form.elements.nome.focus();
      } catch (error) {
        formMessage.textContent = error.message;
      }
    });

    article.append(title, details, desc, editBtn);
    productList.append(article);
  }
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy) return;
  busy = true;
  formMessage.textContent = '';
  const saveButton = document.getElementById('saveProductButton');
  saveButton.disabled = true;

  const rawPrice = form.elements.preco.value.trim().replace(',', '.');
  const payload = {
    nome: form.elements.nome.value,
    descricao: form.elements.descricao.value,
    preco: rawPrice,
    imagemCaminho: form.elements.imagemCaminho.value || null,
    disponivel: form.elements.disponivel.value === 'true'
  };
  if (editingProduct) {
    payload.versao = editingProduct.versao;
  }

  try {
    await api(editingProduct ? `/api/admin/produtos/${editingProduct.id}` : '/api/admin/produtos', {
      method: editingProduct ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const wasEdit = Boolean(editingProduct);
    resetForm();
    formMessage.textContent = wasEdit ? 'Produto atualizado com sucesso.' : 'Produto cadastrado com sucesso.';
    await loadProducts();
  } catch (error) {
    formMessage.textContent = error.message;
    if (error.status === 409) {
      formMessage.textContent += ' Seu texto foi preservado no formulário. Copie o que precisar e use Editar na lista para carregar a versão atual.';
      await loadProducts().catch(() => {});
    }
  } finally {
    saveButton.disabled = false;
    busy = false;
  }
});

async function initAdminProducts() {
  unauthSection.hidden = true;
  forbiddenSection.hidden = true;
  adminContent.hidden = true;
  pageMessage.textContent = '';
  try {
    const user = await api('/api/autenticacao/sessao');
    if (user.perfil !== 'ADMIN') {
      showState(forbiddenSection);
      return;
    }
    await loadProducts();
    showState(adminContent);
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

window.addEventListener('pageshow', initAdminProducts);
window.addEventListener('pagehide', () => {
  adminContent.hidden = true;
  productList.replaceChildren();
});

