// Funcoes utilitarias de formatacao e manipulacao de dados

export function formatCurrencyBRL(value) {
  if (value === null || value === undefined || value === "") {
    return "R$ 0,00";
  }

  const numberValue = typeof value === "number" ? value : parseFloat(value);
  if (isNaN(numberValue)) {
    return "R$ 0,00";
  }

  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(numberValue);
}

export const FALLBACK_IMAGE_PATH = "/assets/img/cupcake-placeholder.svg";

const PRODUCT_IMAGE_PATHS = [
  ["baunilha", "/assets/img/cupcake-baunilha.webp"],
  ["chocolate", "/assets/img/cupcake-chocolate.webp"],
  ["frutas vermelhas", "/assets/img/cupcake-frutas-vermelhas.webp"],
  ["frutas-vermelhas", "/assets/img/cupcake-frutas-vermelhas.webp"],
  ["caramelo", "/assets/img/cupcake-caramelo.webp"]
];

export function resolveProductImage(product) {
  const name = String(product?.nome || "").toLowerCase();
  const imagePath = String(product?.imagemUrl || "").toLowerCase();
  const match = PRODUCT_IMAGE_PATHS.find(([term]) => name.includes(term) || imagePath.includes(`cupcake-${term}`));
  return match ? match[1] : (product?.imagemUrl || FALLBACK_IMAGE_PATH);
}

const STATUS_LABELS = {
  RECEBIDO: "Recebido (RECEBIDO)",
  EM_PREPARACAO: "Em preparação (EM_PREPARACAO)",
  PRONTO_PARA_RETIRADA: "Pronto para retirada (PRONTO_PARA_RETIRADA)",
  SAIU_PARA_ENTREGA: "Saiu para entrega (SAIU_PARA_ENTREGA)",
  CONCLUIDO: "Concluído (CONCLUIDO)",
  CANCELADO: "Cancelado (CANCELADO)",
  NAO_AUTORIZADO: "Não autorizado (NAO_AUTORIZADO)"
};

const MODE_LABELS = {
  ENTREGA: "Entrega em endereço (ENTREGA)",
  RETIRADA: "Retirada na loja (RETIRADA)"
};

const PAYMENT_LABELS = {
  APROVADO: "Pagamento simulado aprovado (APROVADO)",
  RECUSADO: "Pagamento simulado recusado (RECUSADO)"
};

export function formatOrderStatus(status) {
  return STATUS_LABELS[status] || status || "";
}

export function formatOrderMode(mode) {
  return MODE_LABELS[mode] || mode || "";
}

export function formatSimulatedPayment(result) {
  return PAYMENT_LABELS[result] || result || "";
}

export function formatDateTimeLocal(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return String(isoString);
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(date);
}


