# 📖 Eco nel Silenzio

Lettore di PDF con voce narrante, costruito a **microservizi**.

Carichi un PDF (un racconto, un libro) e l'applicazione lo mostra come un **libro aperto** su una scrivania a lume di candela:
- pagina sinistra: il testo, con la **frase letta evidenziata**
- pagina destra: **illustrazioni d'atmosfera** divise per tema *(in arrivo)*
- intanto una **voce sintetica** legge il testo, frase per frase, e gira pagina da sola

> 🚧 Progetto in sviluppo. `libri`, `gateway`, `voce` e il `frontend` funzionano; `immagini` e RabbitMQ sono in arrivo.

## Architettura

```
            Frontend React (libro aperto)          porta 5173
                        │
                   API Gateway                     porta 8080
              (Spring Cloud Gateway)
          ┌─────────────┼──────────────┐
        libri          voce         immagini
     Spring Boot    Spring Boot    (in arrivo)
     PostgreSQL         │
      porta 8081   porta 8082
                        │ HTTP
                  server di Piper                  porta 5050
               (Docker, voce italiana)
```

Ogni servizio è un'applicazione Spring Boot indipendente, con le proprie dipendenze e (se serve) il proprio database.
Il client parla solo con il **gateway**, che inoltra le richieste al servizio giusto.
Il codice è organizzato in un **monorepo**: un repository, una cartella per servizio.

| Servizio | Cosa fa | Porta | Stato |
|---|---|---|---|
| `gateway` | unico punto di ingresso, instrada `/api/libri/**` e `/api/voce/**` | 8080 | ✅ |
| `libri` | riceve il PDF, estrae il testo (PDFBox), toglie intestazioni e piè di pagina ripetuti, lo divide in pagine e frasi (gestisce abbreviazioni, iniziali, dialoghi) | 8081 | ✅ |
| `voce` | trasforma una frase in mp3: voce sintetica con **Piper** (server sempre acceso in Docker) e tonalità abbassata con **ffmpeg**, con cache su disco | 8082 | ✅ |
| `frontend` | libreria con caricamento ed eliminazione dei PDF, pagina di lettura a libro aperto con la voce narrante | 5173 | ✅ |
| `immagini` | galleria di illustrazioni divise per tema | – | ⏳ |
| RabbitMQ | preparazione dell'audio delle pagine successive in background | – | ⏳ |

### La voce
- Ogni frase viene letta da **Piper** (text-to-speech neurale, in locale, gratuito), con una voce italiana maschile.
- Piper gira come **server HTTP sempre acceso** in un container Docker: il modello si carica una volta sola.
  Misurato dal log: da circa **3800 ms** per frase (avviando Piper ogni volta) a **100-300 ms**.
- **ffmpeg** abbassa leggermente la tonalità e converte in mp3.
- Gli mp3 restano in **cache** su disco: la stessa frase non viene generata due volte.
- Mentre una frase suona, il frontend **scarica già la successiva**: nessuna attesa tra una frase e l'altra.
- Se il servizio voce non risponde, il frontend passa alla **voce del browser** (Web Speech API): il sistema degrada in modo controllato invece di rompersi.

## Stack

**Backend**: Java 21 · Spring Boot 4 · Spring Cloud Gateway · Spring Data JPA · PostgreSQL · Flyway · PDFBox · Swagger/OpenAPI · Docker / Docker Compose

**Voce**: Piper (text-to-speech) · ffmpeg

**Frontend**: React · Vite · React Router · Bootstrap 5

**In arrivo**: RabbitMQ · Playwright · GitHub Actions

## Test

Il progetto segue la **piramide dei test**.

| Livello | Strumenti | Stato |
|---|---|---|
| Unit | JUnit 6, AssertJ | ✅ |
| Integrazione | Testcontainers (PostgreSQL vero in Docker, migration Flyway incluse) | ✅ |
| Controller | MockMvc, Mockito | ✅ |
| API black-box (via gateway) | da definire | ⏳ |
| E2E | Playwright (i componenti hanno già i `data-testid`) | ⏳ |
| CI | GitHub Actions | ⏳ |

Servizio `libri`: **35 test** (18 unit, 6 di integrazione, 10 di controller, 1 di avvio).

## Avviare il progetto

Requisiti:
- **Java 21+**
- **Docker Desktop** avviato
- **Node.js 20.19+** (per il frontend)
- **ffmpeg** nel PATH (per il servizio `voce`; su Windows: `winget install Gyan.FFmpeg`)

**1. Server di Piper** (una volta sola: poi riparte da solo con Docker Desktop)

```bash
cd voce/piper
docker build -t eco-piper .
docker run -d --name eco-piper -p 5050:5050 --restart unless-stopped eco-piper
```

La voce italiana viene scaricata **durante la build**: il modello non è nel repository.

**2. Servizio `libri`** (avvia anche PostgreSQL dal `compose.yaml`)

```bash
cd libri
./mvnw spring-boot:run
```

**3. Gateway** (in un altro terminale)

```bash
cd gateway
./mvnw spring-boot:run
```

**4. Servizio `voce`** (in un altro terminale)

```bash
cd voce
./mvnw spring-boot:run
```

**5. Frontend** (in un altro terminale)

```bash
cd frontend
npm install
npm run dev
```

Poi apri **http://localhost:5173**.
In sviluppo il frontend chiama solo `/api/...`: il proxy di Vite inoltra le richieste al gateway, quindi non servono configurazioni CORS.

**Test** del servizio `libri` (con Docker Desktop avviato):

```bash
cd libri
./mvnw test
```

### Indirizzi utili

| Cosa | Indirizzo |
|---|---|
| Applicazione | http://localhost:5173 |
| API (passando dal gateway) | http://localhost:8080/api/libri |
| Swagger di `libri` | http://localhost:8081/swagger-ui.html |
| Health check gateway | http://localhost:8080/actuator/health |
| Health check `libri` | http://localhost:8081/actuator/health |
| Health check `voce` | http://localhost:8082/actuator/health |

### Endpoint (via gateway, porta 8080)

| Metodo | URL | Descrizione | Risposte |
|---|---|---|---|
| `POST` | `/api/libri` | carica un PDF (multipart: `file`, `titolo` facoltativo) | 201, 400, 413 |
| `GET` | `/api/libri` | elenco dei libri, dal più recente | 200 |
| `GET` | `/api/libri/{id}/pagine/{numero}` | una pagina con le sue frasi | 200, 400, 404 |
| `DELETE` | `/api/libri/{id}` | elimina un libro con pagine e frasi | 204, 400, 404 |
| `GET` | `/api/voce/sintesi?testo=...` | l'audio della frase (`audio/mpeg`) | 200, 400, 503 |

Gli errori hanno sempre la stessa forma:

```json
{ "status": 404, "message": "Pagina 9 del libro 5 non trovata", "dataErrore": "2026-11-10T09:30:00" }
```

## Autrice

**Stella Marucelli** – QA Automation Engineer
Portfolio: https://portfolio-loading.vercel.app/