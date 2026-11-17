// Il tema d'atmosfera scelto dal lettore, ricordato per ogni libro (come il segnalibro)
export const TEMA_PREDEFINITO = "notte";

// Stessa regola del servizio immagini: solo lettere minuscole, numeri, - e _
const NOME_TEMA = /^[a-z0-9_-]+$/;

function chiave(libroId) {
  return `eco-atmosfera-${libroId}`;
}

export function leggiAtmosfera(libroId) {
  try {
    const salvato = localStorage.getItem(chiave(libroId));
    if (salvato && NOME_TEMA.test(salvato)) return salvato;
  } catch {
    // storage non disponibile (navigazione privata): si usa il predefinito
  }
  return TEMA_PREDEFINITO;
}

export function salvaAtmosfera(libroId, tema) {
  try {
    localStorage.setItem(chiave(libroId), tema);
  } catch {
    // non salvato: pazienza, la scelta vale solo per questa lettura
  }
}

export function cancellaAtmosfera(libroId) {
  try {
    localStorage.removeItem(chiave(libroId));
  } catch {
    // niente da fare
  }
}
