
Ogni servizio è un'applicazione Spring Boot indipendente, con il proprio database e le proprie versioni.
Il client parla solo con il **gateway**, che inoltra le richieste al servizio giusto.

| Servizio | Cosa fa | Porta | Stato |
|---|---|---|---|
| `gateway` | unico punto di ingresso, instrada le richieste verso i servizi | 8080 | ✅ |
| `libri` | riceve il PDF, estrae il testo (PDFBox), toglie intestazioni e piè di pagina, lo divide in pagine e frasi | 8081 | ✅ |
| `voce` | genera l'audio di ogni frase con un modello text-to-speech locale (Piper) | – | ⏳ |
| `immagini` | galleria di illustrazioni divise per tema | – | ⏳ |
| `frontend` | interfaccia React a libro sfogliabile | – | ⏳ |

## Stack

Java 21 · Spring Boot 4 · Spring Cloud Gateway · Spring Data JPA · PostgreSQL · Flyway · PDFBox · Swagger/OpenAPI · Docker Compose · RabbitMQ · React

## Test

Il progetto segue la **piramide dei test**:

| Livello | Strumenti | Stato |
|---|---|---|
| Unit | JUnit 6, AssertJ | ✅ |
| Integrazione | Testcontainers (PostgreSQL vero in Docker) | ✅ |
| Controller | MockMvc, Mockito | ✅ |
| API black-box (via gateway) | da definire | ⏳ |
| E2E | Playwright | ⏳ |
| CI | GitHub Actions | ⏳ |

## Avviare il progetto

Requisiti: **Java 21+** e **Docker Desktop** avviato.

**1. Servizio `libri`** (avvia anche PostgreSQL dal `compose.yaml`)
```bash
cd libri
./mvnw spring-boot:run
```

**2. Gateway** (in un altro terminale)
```bash
cd gateway
./mvnw spring-boot:run
```

**Test** del servizio `libri`:
```bash
cd libri
./mvnw test
```

### Indirizzi utili

| Cosa | Indirizzo |
|---|---|
| API (passando dal gateway) | http://localhost:8080/api/libri |
| Swagger di `libri` | http://localhost:8081/swagger-ui.html |
| Health check gateway | http://localhost:8080/actuator/health |
| Health check `libri` | http://localhost:8081/actuator/health |

### Endpoint (via gateway, porta 8080)

| Metodo | URL | Descrizione |
|---|---|---|
| `POST` | `/api/libri` | carica un PDF (multipart: `file`, `titolo` facoltativo) |
| `GET` | `/api/libri` | elenco dei libri, dal più recente |
| `GET` | `/api/libri/{id}/pagine/{numero}` | una pagina con le sue frasi |

## Autrice

**Stella Marucelli** – QA Automation Engineer
Portfolio: https://portfolio-loading.vercel.app/