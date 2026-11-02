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

