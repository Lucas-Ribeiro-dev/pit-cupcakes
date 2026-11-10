import { formatCurrencyBRL, FALLBACK_IMAGE_PATH } from "../compartilhado/formatters.js";
import { addToCart } from "./acoes-carrinho.js";

const loadingElement = document.getElementById("loadingState");
const errorElement = document.getElementById("errorState");
const errorMessageElement = document.getElementById("errorMessage");
const notFoundElement = document.getElementById("notFoundState");
const retryButton = document.getElementById("retryButton");
const detailCard = document.getElementById("detailCard");

const detailImage = document.getElementById("detailImage");
const detailTitle = document.getElementById("detailTitle");
const detailDescription = document.getElementById("detailDescription");
const detailPrice = document.getElementById("detailPrice");
const detailBadge = document.getElementById("detailBadge");
const addButton = document.getElementById('addToCart');
const quantityInput = document.getElementById('cartQuantity');
const cartMessage = document.getElementById('cartMessage');
let currentProductId;
addButton.addEventListener('click', () => {
  const quantity = Number(quantityInput.value);
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
    cartMessage.textContent = 'Informe uma quantidade inteira de 1 a 99.';
    return;
  }
  addToCart(currentProductId, quantity, addButton, cartMessage);
});

function showState(activeElement) {
  [loadingElement, errorElement, notFoundElement, detailCard].forEach((el) => {
    if (el) {
      el.classList.add("hidden");
    }
  });

  if (activeElement) {
    activeElement.classList.remove("hidden");
  }
}

function getProductIdFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const rawId = params.get("id");

  if (!rawId) {
    return null;
  }

  const trimmed = rawId.trim();
  // Valida se contem apenas digitos positivos
  if (!/^\d+$/.test(trimmed)) {
    return null;
  }

  const numericId = parseInt(trimmed, 10);
  if (numericId <= 0) {
    return null;
  }

  return numericId;
}

export async function loadProductDetail() {
  const productId = getProductIdFromUrl();

  // Nao realiza requisicao caso o parametro seja invalido ou ausente
  if (productId === null) {
    if (errorMessageElement) {
      errorMessageElement.textContent = "Identificador de produto inválido. Por favor, retorne ao catálogo e selecione um item válido.";
    }
    if (retryButton) {
      retryButton.classList.add("hidden");
    }
    showState(errorElement);
    return;
  }

  if (retryButton) {
    retryButton.classList.remove("hidden");
  }
  showState(loadingElement);

  try {
    const response = await fetch(`/api/produtos/${productId}`);

    if (response.status === 404) {
      showState(notFoundElement);
      return;
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      const message = errorData && errorData.mensagem
        ? errorData.mensagem
        : "Não foi possível carregar os detalhes do produto.";
      showError(message);
      return;
    }

    const product = await response.json();
    renderProductDetail(product);
    showState(detailCard);
  } catch (error) {
    showError("Falha de comunicação com o servidor. Verifique sua conexão e tente novamente.");
  }
}

function renderProductDetail(product) {
  currentProductId = product.id;
  addButton.disabled = !product.disponivel;
  addButton.textContent = product.disponivel ? 'Adicionar ao carrinho' : 'Indisponível';
  if (detailTitle) {
    detailTitle.textContent = product.nome;
    document.title = `${product.nome} - PIT Cupcakes`;
  }

  if (detailDescription) {
    detailDescription.textContent = product.descricao;
  }

  if (detailPrice) {
    detailPrice.textContent = formatCurrencyBRL(product.preco);
  }

  if (detailImage) {
    detailImage.src = product.imagemUrl ? product.imagemUrl : FALLBACK_IMAGE_PATH;
    detailImage.alt = product.nome ? product.nome : "Cupcake";
    detailImage.onerror = () => {
      detailImage.src = FALLBACK_IMAGE_PATH;
    };
  }

  if (detailBadge) {
    if (product.disponivel) {
      detailBadge.className = "badge badge-disponivel";
      detailBadge.textContent = "Disponível";
    } else {
      detailBadge.className = "badge badge-indisponivel";
      detailBadge.textContent = "Indisponível";
    }
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
    loadProductDetail();
  });
}

document.addEventListener("DOMContentLoaded", () => {
  loadProductDetail();
});

