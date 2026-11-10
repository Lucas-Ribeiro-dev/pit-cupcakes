import { formatCurrencyBRL, FALLBACK_IMAGE_PATH } from "../compartilhado/formatters.js";
import { addToCart } from "./acoes-carrinho.js";

const loadingElement = document.getElementById("loadingState");
const emptyElement = document.getElementById("emptyState");
const errorElement = document.getElementById("errorState");
const errorMessageElement = document.getElementById("errorMessage");
const retryButton = document.getElementById("retryButton");
const catalogGrid = document.getElementById("catalogGrid");

function showState(activeElement) {
  [loadingElement, emptyElement, errorElement, catalogGrid].forEach((el) => {
    if (el) {
      el.classList.add("hidden");
    }
  });

  if (activeElement) {
    activeElement.classList.remove("hidden");
  }
}

function createProductCard(product) {
  const card = document.createElement("article");
  card.className = "product-card";

  const imgWrapper = document.createElement("div");
  imgWrapper.className = "card-image-wrapper";

  const img = document.createElement("img");
  img.className = "card-image";
  img.src = product.imagemUrl ? product.imagemUrl : FALLBACK_IMAGE_PATH;
  img.alt = product.nome ? product.nome : "Cupcake";
  img.onerror = () => {
    img.src = FALLBACK_IMAGE_PATH;
  };
  imgWrapper.appendChild(img);

  const body = document.createElement("div");
  body.className = "card-body";

  const title = document.createElement("h2");
  title.className = "card-title";
  title.textContent = product.nome;

  const price = document.createElement("p");
  price.className = "card-price";
  price.textContent = formatCurrencyBRL(product.preco);

  const availabilityWrapper = document.createElement("div");
  availabilityWrapper.className = "card-availability";

  const badge = document.createElement("span");
  if (product.disponivel) {
    badge.className = "badge badge-disponivel";
    badge.textContent = "Disponível";
  } else {
    badge.className = "badge badge-indisponivel";
    badge.textContent = "Indisponível";
  }
  availabilityWrapper.appendChild(badge);

  const actions = document.createElement("div");
  actions.className = "card-actions";

  const detailsLink = document.createElement("a");
  detailsLink.className = "btn btn-primary btn-card";
  detailsLink.href = `produto.html?id=${encodeURIComponent(product.id)}`;
  detailsLink.textContent = "Ver detalhes";
  actions.appendChild(detailsLink);

  const addButton = document.createElement('button');
  addButton.type = 'button';
  addButton.className = 'btn btn-card';
  addButton.textContent = product.disponivel ? 'Adicionar ao carrinho' : 'Indisponível';
  addButton.disabled = !product.disponivel;
  const addMessage = document.createElement('p');
  addMessage.setAttribute('role', 'status');
  addMessage.setAttribute('aria-live', 'polite');
  addButton.addEventListener('click', () => addToCart(product.id, 1, addButton, addMessage));
  actions.append(addButton, addMessage);

  body.appendChild(title);
  body.appendChild(price);
  body.appendChild(availabilityWrapper);
  body.appendChild(actions);

  card.appendChild(imgWrapper);
  card.appendChild(body);

  return card;
}

export async function loadCatalog() {
  showState(loadingElement);

  try {
    const response = await fetch("/api/produtos");

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      const message = errorData && errorData.mensagem
        ? errorData.mensagem
        : "Não foi possível carregar o catálogo de cupcakes.";
      showError(message);
      return;
    }

    const products = await response.json();

    if (!Array.isArray(products) || products.length === 0) {
      showState(emptyElement);
      return;
    }

    catalogGrid.innerHTML = "";
    products.forEach((product) => {
      catalogGrid.appendChild(createProductCard(product));
    });

    showState(catalogGrid);
  } catch (error) {
    showError("Falha de comunicação com o servidor. Verifique sua conexão e tente novamente.");
  }
}

function showError(message) {
  if (errorMessageElement) {
    errorMessageElement.textContent = message;
  }
  showState(errorElement);
}

if (retryButton) {
  retryButton.addEventListener("click", () => {
    loadCatalog();
  });
}

document.addEventListener("DOMContentLoaded", () => {
  loadCatalog();
});

