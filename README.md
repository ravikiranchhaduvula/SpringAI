Spring Boot + Spring AI + RAG



\# AI Router + RAG (Spring Boot)



Spring Boot service that:

\- Exposes \*\*`/ai/route`\*\* — routes user text to either a plain \*\*answer\*\* or an \*\*action\*\* (`createPaymentHold`) using Spring AI (OpenAI).

\- Optional \*\*RAG\*\* with \*\*pgvector\*\*:

&nbsp; - \*\*`/rag/ingest`\*\* to store snippets.

&nbsp; - \*\*`/rag/ask`\*\* to answer using top-k retrieved context.

\- CI: GitHub Actions (build + test), optional \*\*k6\*\* smoke (~2 req/min).



---



\## Requirements



\- Java 21

\- Gradle 8+

\- Docker (for Postgres/pgvector)

\- `OPENAI\_API\_KEY`



---



\## Configuration



`src/main/resources/application.properties`

```properties

\# OpenAI

spring.ai.openai.api-key=${OPENAI\_API\_KEY}

spring.ai.openai.chat.options.model=gpt-4o-mini

spring.ai.openai.chat.options.temperature=0.2

spring.ai.openai.chat.options.timeout=15s



\# Embeddings (RAG)

spring.ai.openai.embedding.options.model=text-embedding-3-small



\# Postgres / pgvector

spring.datasource.url=jdbc:postgresql://localhost:5432/aistore

spring.datasource.username=postgres

spring.datasource.password=postgres

spring.ai.vectorstore.pgvector.initialize-schema=true



\# Actuator (for CI smoke)

management.endpoints.web.exposure.include=health,info



Run locally



Start pgvector



docker compose up -d

\# image: pgvector/pgvector:pg16, DB: aistore/postgres/postgres





Run the app



export OPENAI\_API\_KEY=sk-xxxx

./gradlew bootRun





App listens on http://localhost:8080

.



API

1\) Router



POST /ai/route

Request



{ "q": "Place a hold on invoice INV-1001 for $120.50" }





Response (action)



{ "action": "createPaymentHold", "args": { "invoiceId": "INV-1001", "amount": 120.5 } }





Response (answer)



{ "answer": "Increase DB pool size, add caching, and profile slow SQL." }



2\) RAG: Ingest



POST /rag/ingest



curl -s -X POST http://localhost:8080/rag/ingest \\

&nbsp; -H "content-type: application/json" \\

&nbsp; -d '{"id":"runbook-latency","text":"Runbook: If P95 latency > 800ms, check DB pool size, add response caching, and profile slow SQL.","meta":{"tag":"runbook","service":"checkout"}}'



3\) RAG: Ask



POST /rag/ask



curl -s -X POST http://localhost:8080/rag/ask \\

&nbsp; -H "content-type: application/json" \\

&nbsp; -d '{"q":"How do we reduce p95 latency in checkout?","k":3}'





Example response



{

&nbsp; "answer": "Check DB pool size, add response caching, and profile slow SQL.",

&nbsp; "sources": \[

&nbsp;   {

&nbsp;     "preview": "Runbook: If P95 latency > 800ms, check DB pool size, add response caching...",

&nbsp;     "metadata": { "docId": "runbook-latency", "tag": "runbook", "service": "checkout" }

&nbsp;   }

&nbsp; ]

}



Minimal CORS (for local React dev)

@Configuration

public class CorsConfig {

&nbsp; @Bean WebMvcConfigurer cors() {

&nbsp;   return new WebMvcConfigurer() {

&nbsp;     @Override public void addCorsMappings(CorsRegistry r) {

&nbsp;       r.addMapping("/\*\*").allowedOrigins("http://localhost:5173").allowedMethods("\*");

&nbsp;     }

&nbsp;   };

&nbsp; }

}



k6 Light Smoke (~2 requests/min)



perf/k6-light.js



import http from "k6/http";

import { check, sleep } from "k6";



export const options = { vus: 1, duration: "60s" };



const payloads = \[

&nbsp; { q: "How can I reduce p95 latency?" },

&nbsp; { q: "Place a hold on invoice INV-2002 for $45.00" }

];



export default function () {

&nbsp; const idx = \_\_ITER % payloads.length;

&nbsp; const res = http.post("http://localhost:8080/ai/route",

&nbsp;   JSON.stringify(payloads\[idx]),

&nbsp;   { headers: { "Content-Type": "application/json" } });

&nbsp; check(res, { "200": r => r.status === 200 });

&nbsp; sleep(30);

}





Run:



k6 run perf/k6-light.js



CI (GitHub Actions) – Summary



Build with Gradle (ensure chmod +x gradlew step).



Start pgvector service for RAG.



Run unit tests (plain JUnit contextLoads() or mocked beans).



Upload JAR as artifact.



Optional job: boot app + run k6-light when ENABLE\_K6=true and OPENAI\_API\_KEY secret is set.



Check runs at Repo → Actions.





---



\## Frontend `README.md` (React + Vite)



```markdown

\# AI Router UI (React + Vite + TypeScript)



Tiny UI that posts to the backend \*\*`/ai/route`\*\* and displays either an \*\*answer\*\* or an \*\*action\*\* (`createPaymentHold` + args).  

Inline CSS only, no UI libraries required.



---



\## Requirements



\- Node.js 20+

\- NPM



---



\## Install \& Run



```bash

npm ci

npm run dev





Open http://localhost:5173

.

The input is rendered under the title “Ask a question or request an action:”.



If your backend isn’t on http://localhost:8080, change the URL in:



src/components/HelpBar.tsx  // fetch("http://localhost:8080/ai/route", ...)



Build

npm run build

npm run preview





Build output: dist/



