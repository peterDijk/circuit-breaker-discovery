# Quickstart: Circuit Breaker Weather Service

## Prerequisites

- JDK 17+ installed
- sbt 1.10+ installed (or use the sbt launcher wrapper)

## Setup

```bash
# Clone and enter the project
cd circuit-breaker-discovery

# Compile the project
sbt compile

# Run the application
sbt run
```

## First Steps

Once the application starts, you'll see an interactive prompt:

```
=== Circuit Breaker Discovery ===
Type 'help' for available commands.

circuit-breaker>
```

### 1. Make a successful request

```
circuit-breaker> weather Amsterdam
[CLOSED] Weather for Amsterdam: 18.5°C, Wind: 12.3 km/h
         Failures: 0/5 | Successes: 1
```

### 2. Simulate failures

To see the circuit breaker in action, disconnect your network or use an invalid endpoint:

```
circuit-breaker> weather InvalidCity123
[CLOSED] Request failed: Not found
         Failures: 1/5 | 4 more before circuit opens
```

### 3. Watch the circuit open

After 5 consecutive failures:

```
circuit-breaker> weather InvalidCity123
[CLOSED → OPEN] Circuit opened after 5 consecutive failures!
         Requests will be rejected for 30s.
```

### 4. See request rejection

```
circuit-breaker> weather Amsterdam
[OPEN] Service temporarily unavailable. Retry in 28s.
```

### 5. Observe recovery

Wait 30 seconds and try again:

```
circuit-breaker> weather Amsterdam
[HALF-OPEN] Attempting recovery...
[HALF-OPEN → CLOSED] Circuit recovered! Weather for Amsterdam: 18.5°C
```

## Configuration

Override defaults via environment variables or application config:

| Setting           | Default | Environment Variable       |
|-------------------|---------|----------------------------|
| Failure threshold | 5       | `CB_FAILURE_THRESHOLD`     |
| Reset timeout     | 30s     | `CB_RESET_TIMEOUT_SECONDS` |
| Call timeout      | 10s     | `CB_CALL_TIMEOUT_SECONDS`  |

## Running Tests

```bash
sbt test
```

## Optional: Metrics Server

Start with the metrics endpoint enabled:

```bash
sbt "run --with-metrics"
```

Then check status in another terminal:

```bash
curl http://localhost:8080/status
```
