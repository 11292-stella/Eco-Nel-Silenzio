# 📖 Eco nel Silenzio

Lettore di PDF con voce narrante, costruito a **microservizi**.

Carichi un PDF (un racconto, un libro) e l'applicazione lo mostra come un **libro aperto**:
- pagina destra: il testo, con la **frase letta evidenziata**
- pagina sinistra: **illustrazioni d'atmosfera** realizzate da me, divise per tema
- intanto una **voce sintetica** legge il testo, frase per frase

> 🚧 Progetto in sviluppo. Il servizio `libri` è completo e testato; gli altri servizi sono in arrivo.

## Architettura

```
Frontend React (libro aperto)
            │
       API Gateway  (Spring Cloud Gateway)
     ┌──────┼──────────┐
   libri   voce     immagini
     │      ▲
     └─► RabbitMQ ─┘   "frase pronta da leggere"
```

Ogni servizio è un'applicazione Spring Boot indipendente, con il proprio database.

| Servizio | Cosa fa | Stato |
|---|---|---|
| `libri` | riceve il PDF, estrae il testo (PDFBox), toglie intestazioni e piè di pagina, lo divide in pagine e frasi | ✅ completo |
| `voce` | genera l'audio di ogni frase con un modello text-to-speech locale (Piper) | ⏳ |
| `immagini` | galleria di illustrazioni divise per tema | ⏳ |
| `gateway` | unico punto di ingresso | ⏳ |
| `frontend` | interfaccia React a libro sfogliabile | ⏳ |

## Stack

Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · Flyway · PDFBox · Swagger/OpenAPI · Docker Compose · RabbitMQ · React

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

## Avviare il servizio `libri`

Requisiti: **Java 21+** e **Docker Desktop** avviato.

```bash
cd libri
./mvnw spring-boot:run      # avvia anche PostgreSQL dal compose.yaml
./mvnw test                 # lancia tutti i test
```

- Swagger: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

| Metodo | URL | Descrizione |
|---|---|---|
| `POST` | `/api/libri` | carica un PDF (multipart: `file`, `titolo` facoltativo) |
| `GET` | `/api/libri` | elenco dei libri, dal più recente |
| `GET` | `/api/libri/{id}/pagine/{numero}` | una pagina con le sue frasi |

## Autrice

**Stella Marucelli** – QA Automation Engineer
Portfolio: https://portfolio-loading.vercel.app/