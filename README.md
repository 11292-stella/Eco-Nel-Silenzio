# 📖 Eco nel Silenzio

Lettore di PDF con voce narrante, costruito a **microservizi**.

Carichi un PDF (un racconto, un libro) e l'applicazione lo mostra come un **libro aperto** su una scrivania a lume di candela:
- pagina sinistra: il testo, con la **frase letta evidenziata**
- pagina destra: **illustrazioni d'atmosfera** in dissolvenza (notte, pioggia, bosco, mare…), scelte dal lettore
- intanto una **voce sintetica** legge il testo, frase per frase, e gira pagina da sola

> 🚧 Progetto in sviluppo. `libri`, `gateway`, `voce`, `immagini`, RabbitMQ e il `frontend` funzionano; il servizio `assistente` (Ollama) è in arrivo.

## Architettura

```
                 Frontend React (libro aperto)               porta 5173
                              │
                         API Gateway                          porta 8080
                    (Spring Cloud Gateway)
          ┌───────────────────┼────────────────────┐
        libri               voce               immagini
     Spring Boot        Spring Boot           Spring Boot
     PostgreSQL              │              catalogo su file
      porta 8081         porta 8082           porta 8083
          │                  │ HTTP
          │            server di Piper                        porta 5050
          │          (Docker, voce italiana)
          │                  ▲
          └──▶ RabbitMQ ─────┘                                porta 5672
           (frasi da preparare in anticipo)
```

Ogni servizio è un'applicazione Spring Boot indipendente, con le proprie dipendenze e (se serve) il proprio database.
Il client parla solo con il **gateway**, che inoltra le richieste al servizio giusto.
Il codice è organizzato in un **monorepo**: un repository, una cartella per servizio.

| Servizio | Cosa fa | Porta | Stato |
|---|---|---|---|
| `gateway` | unico punto di ingresso, instrada `/api/libri/**`, `/api/voce/**` e `/api/immagini/**` | 8080 | ✅ |
| `libri` | riceve il PDF, estrae il testo (PDFBox), toglie intestazioni e piè di pagina ripetuti, lo divide in pagine e frasi (gestisce abbreviazioni, iniziali, dialoghi). Quando si apre una pagina, manda su **RabbitMQ** le frasi della pagina successiva | 8081 | ✅ |
| `voce` | trasforma una frase in mp3: voce sintetica con **Piper** (server sempre acceso in Docker) e tonalità abbassata con **ffmpeg**, con cache su disco. Legge da **RabbitMQ** le frasi da preparare in anticipo | 8082 | ✅ |
| `immagini` | catalogo delle atmosfere: una cartella per tema con le immagini e un `tema.json`, nessun database | 8083 | ✅ |
| `frontend` | libreria con caricamento ed eliminazione dei PDF, pagina di lettura a libro aperto con voce narrante, plancia dei comandi e selettore dell'atmosfera | 5173 | ✅ |
| RabbitMQ | coda delle frasi da preparare in anticipo (`libri` → `voce`) | 5672 | ✅ |
| `assistente` | suggerisce l'atmosfera del libro con Ollama (in locale) | – | ⏳ |

### La voce
- Ogni frase viene letta da **Piper** (text-to-speech neurale, in locale, gratuito), con una voce italiana maschile.
- Piper gira come **server HTTP sempre acceso** in un container Docker: il modello si carica una volta sola.
  Misurato dal log: da circa **3800 ms** per frase (avviando Piper ogni volta) a **100-300 ms**.
- **ffmpeg** abbassa leggermente la tonalità e converte in mp3.
- Gli mp3 restano in **cache** su disco: la stessa frase non viene generata due volte.
- La cache si **pulisce da sola** (all'avvio e ogni notte): via gli audio non ascoltati da 7 giorni e, oltre i 500 MB, i meno usati (LRU).
- Mentre una frase suona, il frontend **scarica già la successiva**: nessuna attesa tra una frase e l'altra.
- Se il servizio voce non risponde, il frontend passa alla **voce del browser** (Web Speech API): il sistema degrada in modo controllato invece di rompersi.

### La preparazione in anticipo (RabbitMQ)
```
GET pagina 3 ──▶ libri ──risponde subito──▶ frontend
                   │ (@Async)
                   └─▶ exchange eco.voce ──prepara──▶ coda voce.preparazione ──▶ voce
                       una frase per messaggio, in JSON          una alla volta → Piper → cache
```
- Quando il lettore apre una pagina, `libri` manda in coda le frasi della **pagina successiva**; `voce` le genera in background. Quando il lettore ci arriva, l'audio è **già in cache**.
- `libri` **non aspetta** (`@Async`) e non sa nemmeno che `voce` esiste: conosce solo l'exchange.
- Se `voce` è spento, i messaggi **aspettano in coda**. Se RabbitMQ è spento, la lettura funziona lo stesso (l'audio si genera al momento).
- `voce` lavora **una frase alla volta** (`prefetch = 1`), per non intasare Piper mentre il lettore ascolta.
- Una frase che fallisce (es. Piper spento) o un messaggio rotto vengono **scartati**: niente giri infiniti.
- Il contratto tra i due servizi è il **JSON**, non una classe Java condivisa.

### Le atmosfere
- Il catalogo è una cartella: `immagini/catalogo/<tema>/` con le immagini (WebP 800×800, sfondo trasparente) e un `tema.json` (titolo, descrizione, ordine).
- Si aggiunge un'immagine e compare **senza ricompilare né riavviare**.
- Protezione dal **path traversal** con due difese indipendenti: nomi ammessi solo con una whitelist di caratteri + percorso normalizzato che deve restare dentro la cartella del tema.
- Il lettore sceglie l'atmosfera dalla plancia; la scelta si ricorda **per ogni libro**.

## Stack

**Backend**: Java 21 · Spring Boot 4 · Spring Cloud Gateway · Spring Data JPA · Spring AMQP · PostgreSQL · Flyway · PDFBox · Swagger/OpenAPI · Docker / Docker Compose

**Messaggi**: RabbitMQ

**Voce**: Piper (text-to-speech) · ffmpeg

**Frontend**: React · Vite · React Router · Bootstrap 5

**In arrivo**: Ollama · Playwright · GitHub Actions

## Grafica
Immagini d'atmosfera e dell'interfaccia generate con l'AI (Gemini) e rielaborate da me (bordi, sfondo trasparente, WebP); logo disegnato da me.

## Test

Il progetto segue la **piramide dei test**. I test nascono **insieme** al codice, e per ognuno si verifica che "sappia fallire".

| Livello | Strumenti | Stato |
|---|---|---|
| Unit | JUnit 6, AssertJ, Mockito | ✅ |
| Integrazione | Testcontainers (PostgreSQL e RabbitMQ veri in Docker) | ✅ |
| Controller | MockMvc | ✅ |
| API black-box (via gateway) | da definire | ⏳ |
| E2E | Playwright (i componenti hanno già i `data-testid`) | ⏳ |
| CI | GitHub Actions | ⏳ |

| Servizio | Test | Dettaglio |
|---|---|---|
| `libri` | **40** | 23 unit, 6 di integrazione (PostgreSQL in Testcontainers), 10 di controller, 1 di avvio |
| `voce` | **30** | 6 di controller, 5 della sintesi con un **server di Piper finto**, 6 della pulizia della cache, 9 del listener RabbitMQ, 3 di integrazione con **RabbitMQ in Testcontainers**, 1 di avvio. Il test che usa ffmpeg viene saltato se ffmpeg non è installato |
| `immagini` | **35** | 22 del catalogo (compreso il path traversal), 12 di controller, 1 di avvio |

## Avviare il progetto

Requisiti:
- **Java 21+**
- **Docker Desktop** avviato
- **Node.js 20.19+** (per il frontend)
- **ffmpeg** nel PATH (per il servizio `voce`; su Windows: `winget install Gyan.FFmpeg`)

**1. Server di Piper e RabbitMQ** (una volta sola: poi ripartono da soli con Docker Desktop)

```bash
cd voce/piper
docker build -t eco-piper .
docker run -d --name eco-piper -p 5050:5050 --restart unless-stopped eco-piper

docker run -d --name eco-rabbitmq -p 5672:5672 -p 15672:15672 --restart unless-stopped rabbitmq:4-management
```

La voce italiana viene scaricata **durante la build**: il modello non è nel repository.
Pannello di RabbitMQ: http://localhost:15672 (utente e password `guest`).

> ⚠️ Su Windows, se è installato anche un RabbitMQ "classico" come servizio, va fermato (`Stop-Service RabbitMQ` da PowerShell come amministratore): altrimenti risponde lui sulla porta 5672 al posto del container.

**2. Servizio `libri`** (avvia anche PostgreSQL dal `compose.yaml`)

```bash
cd libri
./mvnw spring-boot:run
```

**3. Gateway, `voce` e `immagini`** (ognuno in un altro terminale)

```bash
cd gateway
./mvnw spring-boot:run
```
```bash
cd voce
./mvnw spring-boot:run
```
```bash
cd immagini
./mvnw spring-boot:run
```

**4. Frontend** (in un altro terminale)

```bash
cd frontend
npm install
npm run dev
```

Poi apri **http://localhost:5173**.
In sviluppo il frontend chiama solo `/api/...`: il proxy di Vite inoltra le richieste al gateway, quindi non servono configurazioni CORS.

**Test** (con Docker Desktop avviato), in ogni servizio:

```bash
cd libri        # oppure voce, immagini
./mvnw test
```

### Indirizzi utili

| Cosa | Indirizzo |
|---|---|
| Applicazione | http://localhost:5173 |
| API (passando dal gateway) | http://localhost:8080/api/libri |
| Swagger di `libri` | http://localhost:8081/swagger-ui.html |
| Pannello di RabbitMQ | http://localhost:15672 |
| Health check gateway | http://localhost:8080/actuator/health |
| Health check `libri` | http://localhost:8081/actuator/health |
| Health check `voce` | http://localhost:8082/actuator/health |
| Health check `immagini` | http://localhost:8083/actuator/health |

### Endpoint (via gateway, porta 8080)

| Metodo | URL | Descrizione | Risposte |
|---|---|---|---|
| `POST` | `/api/libri` | carica un PDF (multipart: `file`, `titolo` facoltativo) | 201, 400, 413 |
| `GET` | `/api/libri` | elenco dei libri, dal più recente | 200 |
| `GET` | `/api/libri/{id}/pagine/{numero}` | una pagina con le sue frasi (e prepara in anticipo l'audio della successiva) | 200, 400, 404 |
| `DELETE` | `/api/libri/{id}` | elimina un libro con pagine e frasi | 204, 400, 404 |
| `GET` | `/api/voce/sintesi?testo=...` | l'audio della frase (`audio/mpeg`) | 200, 400, 503 |
| `GET` | `/api/immagini/temi` | i temi del catalogo, in ordine | 200 |
| `GET` | `/api/immagini/temi/{tema}` | le immagini di un tema | 200, 404 |
| `GET` | `/api/immagini/file/{tema}/{nome}` | il file dell'immagine | 200, 404 |

Gli errori hanno sempre la stessa forma:

```json
{ "status": 404, "message": "Pagina 9 del libro 5 non trovata", "dataErrore": "2026-11-10T09:30:00" }
```

### Messaggi su RabbitMQ

| Exchange | Routing key | Coda | Da → a | Contenuto |
|---|---|---|---|---|
| `eco.voce` (direct) | `prepara` | `voce.preparazione` | `libri` → `voce` | `{"libroId":5,"pagina":4,"testo":"Era una notte buia."}` |

## Autrice

**Stella Marucelli** – QA Automation Engineer
Portfolio: https://portfolio-loading.vercel.app/
