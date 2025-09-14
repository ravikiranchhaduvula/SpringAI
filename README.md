# AI Router Backend (Spring Boot + RAG)

Spring Boot service that routes user text to:
- **Plain answers** (via OpenAI)
- **Actions** like `createPaymentHold`
- (Optional) **RAG** using pgvector (Postgres)

---

## Key Configuration

**`src/main/resources/application.properties`**
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.2
spring.ai.openai.chat.options.timeout=15s

spring.ai.openai.embedding.options.model=text-embedding-3-small

spring.datasource.url=jdbc:postgresql://localhost:5432/aistore
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.ai.vectorstore.pgvector.initialize-schema=true
Important Files
AiController.java — routes /ai/ask and /ai/route requests.

RagController.java — optional /rag/ingest and /rag/ask endpoints.

build.gradle — Gradle build file, dependencies (Spring Boot, Spring AI, pgvector).

docker-compose.yml — starts Postgres with pgvector extension.

.github/workflows/ci.yml — CI: builds, runs tests, uploads JAR.

Running Locally
bash
Copy code
# Start Postgres
docker compose up -d

# Run backend
./gradlew bootRun
Backend runs on http://localhost:8080