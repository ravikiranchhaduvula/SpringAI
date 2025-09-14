Backend — README.md
AI Router + RAG (Spring Boot)

Spring Boot service that:

Exposes /ai/route — routes user text to either a plain answer or an action (createPaymentHold) using Spring AI (OpenAI) with guardrails (JSON-only + retry).

Optional RAG: /rag/ingest to add snippets to pgvector (Postgres), /rag/ask to answer using top-k retrieved context.

CI: GitHub Actions builds/tests, optional k6 smoke (~2 req/min).

Requirements

Java 21

Gradle 8+

Docker (for Postgres/pgvector)

OpenAI API key

Quick start
1) Configure (properties, not YAML)

src/main/resources/application.properties

# OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.2
spring.ai.openai.chat.options.timeout=15s

# Embeddings for RAG
spring.ai.openai.embedding.options.model=text-embedding-3-small

# pgvector datasource
spring.datasource.url=jdbc:postgresql://localhost:5432/aistore
spring.datasource.username=postgres
spring.datasource.password=postgres

# Create vector schema at startup
spring.ai.vectorstore.pgvector.initialize-schema=true

# Actuator (for CI smoke)
management.endpoints.web.exposure.include=health,info

2) Start pgvector

docker-compose.yml

services:
  db:
    image: pgvector/pgvector:pg16
    container_name: pgvector-db
    environment:
      POSTGRES_DB: aistore
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"


Run:

docker compose up -d

3) Run the app
export OPENAI_API_KEY=sk-...   # PowerShell: $env:OPENAI_API_KEY="sk-..."
./gradlew bootRun

API
1) Router

POST /ai/route
Request:

{ "q": "Place a hold on invoice INV-1001 for $120.50" }


Possible responses:

{ "action":"createPaymentHold", "args":{"invoiceId":"INV-1001","amount":120.5} }


or

{ "answer":"Increase DB pool size, add caching, and profile slow SQL." }

2) RAG: ingest

POST /rag/ingest

curl -s -X POST http://localhost:8080/rag/ingest \
  -H "content-type: application/json" \
  -d '{"id":"runbook-latency","text":"Runbook: If P95 latency > 800ms, check DB pool size, add response caching on product detail, and profile slow SQL queries.","meta":{"tag":"runbook","service":"checkout"}}'

3) RAG: ask

POST /rag/ask

curl -s -X POST http://localhost:8080/rag/ask \
  -H "content-type: application/json" \
  -d '{"q":"How do we reduce p95 latency in checkout?","k":3}'


Response:

{
  "answer": "Check DB pool size, add response caching, and profile slow SQL.",
  "sources": [
    { "preview":"Runbook: If P95 latency > 800ms, check DB pool size, add respon...", "metadata":{"docId":"runbook-latency","tag":"runbook","service":"checkout"} }
  ]
}