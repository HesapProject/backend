# ADR-001: Reactive Stack (WebFlux + R2DBC)

## Status: Accepted

## Context
Loyihada ko'p sonli bir vaqtda keluvchi so'rovlar bor (to'lov monitoring, real-time notifications).
Blocking stack (Spring MVC + JDBC) thread pool limitiga tez yetadi.

## Decision
- **Spring WebFlux** — non-blocking HTTP server
- **R2DBC** — non-blocking database client
- **Project Reactor** — `Mono`/`Flux` reactive streams

## Consequences
- Barcha kod `Mono`/`Flux` qaytarishi kerak
- `.block()` chaqirish **taqiqlanadi**
- Test yozishda `StepVerifier` ishlatish kerak
- Debugging murakkabroq — stack trace o'rniga reactive chain
