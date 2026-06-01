# Research: Circuit Breaker Weather Service

## Decision 1: Circuit Breaker Implementation Approach

**Decision**: Hand-rolled circuit breaker using cats-effect `Ref` for state management.

**Rationale**: The user explicitly wants to learn how circuit breakers work. Using a library (e.g., resilience4j-scala or cats-retry) would hide the internals. Building from scratch with `Ref` (atomic, concurrent-safe mutable state) and `Temporal` (for timeouts/delays) exposes every state transition clearly while remaining idiomatic Scala.

**Alternatives considered**:
- resilience4j with Scala wrapper — production-ready but opaque; defeats learning purpose
- Akka circuit breaker — heavyweight dependency, actor-based which adds conceptual overhead
- cats-retry — only handles retries, not the full circuit breaker state machine

## Decision 2: Effect System

**Decision**: cats-effect 3.x with `IO` monad.

**Rationale**: The user works in Scala professionally, and cats-effect is the dominant pure-FP effect system in the Scala ecosystem. It provides `Ref` for atomic state, `Temporal` for time-based operations (sleep, timeouts), and composes naturally. The circuit breaker maps cleanly to a state machine managed by `Ref`.

**Alternatives considered**:
- ZIO — equally capable but different ecosystem; cats-effect is more lightweight for a learning project
- Scala Futures — no built-in atomic state management; would require locks or actors
- Plain imperative with `synchronized` — not idiomatic Scala; misses the educational value of FP patterns

## Decision 3: HTTP Client

**Decision**: http4s Ember client (cats-effect native).

**Rationale**: http4s integrates seamlessly with cats-effect `IO`, supports timeouts natively, and is the standard HTTP library in the Typelevel ecosystem. The Ember client is lightweight and doesn't require external runtime.

**Alternatives considered**:
- sttp — more flexible backend options but adds an abstraction layer
- Scala standard library `java.net.http` — verbose, not `IO`-native
- akka-http — actor-system dependency, overkill for a simple GET call

## Decision 4: Weather API

**Decision**: Open-Meteo (open-meteo.com) — free, no API key required.

**Rationale**: Zero signup friction. Returns JSON with temperature, weather code, etc. for any lat/lon. Perfect for a learning project because you can start immediately without credentials.

**Alternatives considered**:
- OpenWeatherMap — requires API key registration
- WeatherAPI — requires API key
- wttr.in — simple but returns formatted text rather than structured JSON

## Decision 5: Scala Version & Build

**Decision**: Scala 3.3 LTS with sbt 1.10.x.

**Rationale**: Scala 3.3 is the current Long Term Support release, stable and well-supported by all libraries. sbt is the standard Scala build tool.

**Alternatives considered**:
- Scala 2.13 — still common in industry but Scala 3 is the future and offers cleaner syntax
- Mill build tool — less ecosystem support, non-standard for newcomers

## Decision 6: Project Type

**Decision**: CLI application with optional HTTP server for metrics endpoint.

**Rationale**: Lowest friction to run and observe. Console output shows state transitions immediately. An optional lightweight HTTP server (http4s) can expose metrics via a simple endpoint for the P3 user story.

**Alternatives considered**:
- Pure library — harder to demonstrate interactively
- Full web service — overengineered for learning
- Scripted demo — less interactive, can't experiment freely
