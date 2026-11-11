// Il "segnalibro": dove ci si e' fermati in ogni libro, salvato nel browser (localStorage).
// Resta solo in questo browser: niente server, niente account.
// Sempre dentro try/catch: in navigazione privata o con i dati bloccati localStorage puo' mancare.

const INIZIO = { numero: 1, foglio: 0 };

function chiave(libroId) {
  return `eco-lettura-${libroId}`;
}

/** Dove ci si era fermati in questo libro: { numero, foglio }. Se non c'e' niente, l'inizio. */
export function leggiSegnalibro(libroId) {
  try {
    const salvato = JSON.parse(localStorage.getItem(chiave(libroId)));
    if (
      Number.isInteger(salvato?.numero) &&
      salvato.numero >= 1 &&
      Number.isInteger(salvato?.foglio)
    ) {
      return salvato;
    }
  } catch {
    // dato rovinato o storage non disponibile: si riparte dall'inizio
  }
  return INIZIO;
}

/** Salva pagina e foglio. */
export function salvaSegnalibro(libroId, numero, foglio) {
  try {
    localStorage.setItem(chiave(libroId), JSON.stringify({ numero, foglio }));
  } catch {
    // storage non disponibile: pazienza, la lettura funziona lo stesso
  }
}

/** Cancella il segnalibro (es. quando il libro viene eliminato). */
export function cancellaSegnalibro(libroId) {
  try {
    localStorage.removeItem(chiave(libroId));
  } catch {
    // niente da fare
  }
}
