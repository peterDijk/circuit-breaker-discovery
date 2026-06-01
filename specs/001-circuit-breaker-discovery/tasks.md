# Tasks: Circuit Breaker Weather Service

**Input**: Design documents from `specs/001-circuit-breaker-discovery/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in the feature specification. Tests omitted from task list.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization — sbt project, dependencies, directory structure

- [x] T001 Create sbt project structure: build.sbt with Scala 3.3, cats-effect 3.5, http4s 0.23, circe 0.14 dependencies, project/build.properties with sbt 1.10
- [x] T002 [P] Create directory structure per plan: src/main/scala/circuitbreaker/{model,weather,cli,metrics}/ and src/test/scala/circuitbreaker/
- [x] T003 [P] Create project/plugins.sbt with sbt-revolver for hot-reload during development

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain model and circuit breaker state machine that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 [P] Define State ADT (Closed, Open, HalfOpen) in src/main/scala/circuitbreaker/model/State.scala
- [x] T005 [P] Define CircuitBreakerConfig case class with failureThreshold, resetTimeout, callTimeout in src/main/scala/circuitbreaker/CircuitBreakerConfig.scala
- [x] T006 [P] Define StateTransition case class (from, to, timestamp, reason) in src/main/scala/circuitbreaker/model/StateTransition.scala
- [x] T007 [P] Define WeatherResponse case class with circe decoder in src/main/scala/circuitbreaker/model/WeatherResponse.scala
- [x] T008 [P] Define FallbackResponse case class in src/main/scala/circuitbreaker/model/FallbackResponse.scala
- [x] T009 Implement CircuitBreaker core state machine using cats-effect Ref in src/main/scala/circuitbreaker/CircuitBreaker.scala — must support: protect[A](call: IO[A]): IO[Either[FallbackResponse, A]], state transitions based on failure count and timeouts, atomic state updates
- [x] T010 Implement WeatherClient using http4s Ember client to call Open-Meteo API in src/main/scala/circuitbreaker/weather/WeatherClient.scala — accepts city name, returns IO[WeatherResponse], respects callTimeout from config

**Checkpoint**: Foundation ready — circuit breaker can protect calls and weather client can fetch data

---

## Phase 3: User Story 1 — Fetch Weather Through Circuit Breaker (Priority: P1) 🎯 MVP

**Goal**: User can call the weather service through the circuit breaker and observe it transition from closed to open after consecutive failures

**Independent Test**: Run the app, make weather requests for valid cities (succeed), then invalid/unreachable endpoints (fail) — observe circuit open after 5 failures and subsequent requests rejected immediately

### Implementation for User Story 1

- [x] T011 [US1] Implement Repl command loop in src/main/scala/circuitbreaker/cli/Repl.scala — parse commands: "weather <city>", "status", "config", "help", "quit"; dispatch to appropriate handler
- [x] T012 [US1] Implement weather command handler in Repl that calls CircuitBreaker.protect(weatherClient.fetch(city)) and formats output per CLI contract (show state, temperature, failure count)
- [x] T013 [US1] Implement Main.scala entry point in src/main/scala/circuitbreaker/Main.scala — wire CircuitBreakerConfig (from env vars or defaults), create CircuitBreaker, create WeatherClient, launch Repl
- [x] T014 [US1] Implement "status" and "config" commands in Repl showing current CircuitBreaker state/metrics and configuration values
- [x] T015 [US1] Add console logging of state transitions — each transition prints timestamp, from-state, to-state, and reason to stdout

**Checkpoint**: User Story 1 fully functional — user can make weather requests, see circuit open after failures, and see requests rejected when open

---

## Phase 4: User Story 2 — Observe Circuit Recovery (Priority: P2)

**Goal**: User can observe the circuit automatically attempt recovery after the timeout period — half-open state allows a trial request through

**Independent Test**: Trigger failures to open circuit, wait for reset timeout (30s default), make another request — observe half-open trial and either recovery (closed) or re-open

### Implementation for User Story 2

- [x] T016 [US2] Enhance CircuitBreaker.protect to implement half-open logic: after resetTimeout elapses, allow exactly one trial request through; if it succeeds → transition to Closed; if it fails → transition back to Open with reset timer restarted
- [x] T017 [US2] Update CLI output formatting to show half-open state transitions: "[HALF-OPEN] Attempting recovery..." followed by result
- [x] T018 [US2] Add "time until recovery" display when circuit is open — show remaining seconds until half-open transition in the rejection message

**Checkpoint**: Full circuit breaker lifecycle observable — closed → open → half-open → closed/open

---

## Phase 5: User Story 3 — View State and Metrics (Priority: P3)

**Goal**: User can see current state, metrics (counts), and a history of state transitions for deeper understanding

**Independent Test**: Make a series of requests, run "status" and "history" commands — verify counts match expectations and transitions are logged with timestamps

### Implementation for User Story 3

- [x] T019 [US3] Implement CircuitBreakerMetrics in src/main/scala/circuitbreaker/CircuitBreakerMetrics.scala — track successCount, totalFailures, lastStateChange timestamp; expose via getMetrics: IO[Metrics]
- [x] T020 [US3] Implement transition history storage (bounded list of last 20 StateTransitions) in CircuitBreaker using a second Ref
- [x] T021 [US3] Implement "history" command in Repl that displays recent state transitions with timestamps and reasons per CLI contract
- [x] T022 [P] [US3] Implement optional MetricsServer in src/main/scala/circuitbreaker/metrics/MetricsServer.scala — http4s Ember server on localhost:8080 with GET /status and GET /history endpoints returning JSON per metrics contract
- [x] T023 [US3] Add "--with-metrics" flag to Main.scala that optionally starts the MetricsServer alongside the REPL

**Checkpoint**: All user stories functional — full observability into circuit breaker behavior

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that enhance the overall experience

- [x] T024 [P] Add configuration loading from environment variables (CB_FAILURE_THRESHOLD, CB_RESET_TIMEOUT_SECONDS, CB_CALL_TIMEOUT_SECONDS) in CircuitBreakerConfig companion object
- [x] T025 [P] Add "help" command output listing all available commands with brief descriptions
- [x] T026 Validate quickstart.md walkthrough end-to-end — ensure all documented commands work as described

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase completion
- **User Story 2 (Phase 4)**: Depends on Foundational phase; enhances CircuitBreaker from US1 but core logic is in Phase 2
- **User Story 3 (Phase 5)**: Depends on Foundational phase; adds observability layer
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2 — delivers working CLI + circuit breaker + weather calls
- **User Story 2 (P2)**: Can start after Phase 2 — enhances the half-open recovery logic (could technically start in parallel with US1 since it modifies CircuitBreaker.scala)
- **User Story 3 (P3)**: Can start after Phase 2 — adds metrics/history layer (independent files, parallelizable with US1/US2 except for Repl commands)

### Within Each User Story

- Models/config before core logic
- Core logic before CLI integration
- CLI integration before polish

### Parallel Opportunities

- T002, T003 can run in parallel (different files)
- T004, T005, T006, T007, T008 can ALL run in parallel (independent model files)
- T022 (MetricsServer) can run in parallel with T019-T021 (different file)
- T024, T025 can run in parallel (different concerns)

---

## Parallel Example: Foundational Phase

```bash
# Launch all model definitions together:
Task: "Define State ADT in src/main/scala/circuitbreaker/model/State.scala"
Task: "Define CircuitBreakerConfig in src/main/scala/circuitbreaker/CircuitBreakerConfig.scala"
Task: "Define StateTransition in src/main/scala/circuitbreaker/model/StateTransition.scala"
Task: "Define WeatherResponse in src/main/scala/circuitbreaker/model/WeatherResponse.scala"
Task: "Define FallbackResponse in src/main/scala/circuitbreaker/model/FallbackResponse.scala"

# Then sequentially:
Task: "Implement CircuitBreaker core state machine"
Task: "Implement WeatherClient"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (sbt project, dependencies)
2. Complete Phase 2: Foundational (state machine + weather client)
3. Complete Phase 3: User Story 1 (CLI, wiring, basic state transitions)
4. **STOP and VALIDATE**: Can you make weather requests and see the circuit open after failures?
5. Demo-ready at this point

### Incremental Delivery

1. Setup + Foundational → Core ready
2. Add User Story 1 → Test: weather requests work, circuit opens on failures (MVP!)
3. Add User Story 2 → Test: circuit recovers after timeout, half-open visible
4. Add User Story 3 → Test: metrics/history commands work, optional HTTP endpoint
5. Each story adds observability without breaking previous functionality

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently testable after the foundational phase
- The circuit breaker state machine (T009) is the most complex task — it encapsulates the core learning
- Commit after each task or logical group
- Stop at any checkpoint to validate the story independently
