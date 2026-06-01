# Implementation Plan: Circuit Breaker Weather Service

**Branch**: `001-circuit-breaker-discovery` | **Date**: 2026-06-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-circuit-breaker-discovery/spec.md`

## Summary

Build a hands-on circuit breaker learning project in Scala 3. A hand-rolled circuit breaker (using cats-effect `Ref` for state) wraps calls to the Open-Meteo weather API. An interactive CLI lets the user make requests, observe state transitions (closed → open → half-open → closed), and view metrics. The circuit breaker is built from scratch so every mechanism is visible and understandable.

## Technical Context

**Language/Version**: Scala 3.3 LTS on JDK 17+

**Primary Dependencies**:
- cats-effect 3.5 — IO monad, Ref (atomic state), Temporal (time/sleep)
- http4s 0.23 — Ember HTTP client + optional server for metrics
- circe 0.14 — JSON decoding of weather API responses
- decline 2.4 — CLI argument parsing (optional, for config overrides)

**Storage**: In-memory only (Ref-based); state transitions stored in a bounded list

**Testing**: munit + munit-cats-effect for unit and integration tests; cats-effect TestControl for time-based testing

**Target Platform**: JVM (macOS/Linux/Windows), command-line

**Project Type**: CLI application with optional embedded HTTP server

**Performance Goals**: Not applicable (learning project); circuit breaker should reject open-state requests in <10ms

**Constraints**: External API calls timeout at 10s; project should compile and run with a single `sbt run`

**Scale/Scope**: Single developer learning project; one circuit breaker, one external service

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

No project constitution defined (template only). No gates to check. ✅ PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-circuit-breaker-discovery/
├── plan.md              # This file
├── research.md          # Technology decisions and rationale
├── data-model.md        # Entity definitions and state machine
├── quickstart.md        # Getting started guide
├── contracts/
│   ├── cli-interface.md # CLI command/output contract
│   └── metrics-endpoint.md # Optional HTTP metrics API
└── tasks.md             # Task breakdown (via /speckit-tasks)
```

### Source Code (repository root)

```text
src/main/scala/circuitbreaker/
├── Main.scala              # Entry point, wiring, REPL loop
├── CircuitBreaker.scala    # Core state machine (Ref-based)
├── CircuitBreakerConfig.scala  # Configuration case class
├── CircuitBreakerMetrics.scala # Metrics/state reporting
├── model/
│   ├── State.scala         # ADT: Closed, Open, HalfOpen
│   ├── StateTransition.scala # Transition event record
│   ├── WeatherResponse.scala # Decoded weather data
│   └── FallbackResponse.scala # Open-state response
├── weather/
│   └── WeatherClient.scala # HTTP client for Open-Meteo API
├── cli/
│   └── Repl.scala          # Interactive command loop
└── metrics/
    └── MetricsServer.scala # Optional http4s metrics endpoint

src/test/scala/circuitbreaker/
├── CircuitBreakerSpec.scala    # Core state machine tests
├── CircuitBreakerTimingSpec.scala # TestControl time-based tests
└── WeatherClientSpec.scala     # HTTP client tests (mocked)

project/
├── build.properties    # sbt version
└── plugins.sbt         # sbt plugins

build.sbt              # Project definition, dependencies
```

**Structure Decision**: Single-module sbt project. The project is small enough that splitting into sub-modules would add unnecessary complexity. All source lives under `src/main/scala/circuitbreaker/` with logical package separation.

## Complexity Tracking

No violations to justify — project is intentionally minimal.
