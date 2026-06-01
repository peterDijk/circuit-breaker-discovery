# CLI Interface Contract

The application exposes a command-line interface for interacting with the circuit breaker demo.

## Commands

### `weather <city>`

Fetch weather for the given city through the circuit breaker.

**Input**: City name as positional argument (e.g., `weather Amsterdam`)

**Output on success** (circuit closed, request succeeds):
```
[CLOSED] Weather for Amsterdam: 18.5°C, Wind: 12.3 km/h
         Failures: 0/5 | Successes: 42
```

**Output on circuit open** (request rejected):
```
[OPEN] Service temporarily unavailable. Retry in 24s.
       Failures: 5/5 | Last failure: 2s ago
```

**Output on half-open trial** (testing recovery):
```
[HALF-OPEN] Attempting recovery...
[CLOSED] Circuit recovered! Weather for Amsterdam: 19.1°C, Wind: 8.7 km/h
```

### `status`

Show current circuit breaker state and metrics.

**Output**:
```
Circuit Breaker Status:
  State:          Closed
  Failure Count:  2/5
  Success Count:  38
  Total Failures: 7
  Last Change:    2026-06-01T14:23:01Z (Closed → Closed)
```

### `config`

Show current configuration.

**Output**:
```
Configuration:
  Failure Threshold: 5
  Reset Timeout:     30s
  Call Timeout:       10s
```

### `history`

Show recent state transitions.

**Output**:
```
State Transitions:
  14:23:01 Closed → Open    (5 consecutive failures)
  14:23:32 Open → HalfOpen  (reset timeout elapsed)
  14:23:32 HalfOpen → Closed (trial request succeeded)
```

## Interactive Mode

The application runs in a REPL loop, accepting commands until the user types `quit` or `exit`.

```
circuit-breaker> weather London
[CLOSED] Weather for London: 14.2°C, Wind: 22.1 km/h

circuit-breaker> status
Circuit Breaker Status:
  State: Closed
  ...

circuit-breaker> quit
Goodbye!
```

## Error Display

Errors from the external service are shown with context:

```
[CLOSED] Request failed: Connection timeout after 10s
         Failures: 3/5 | 2 more before circuit opens
```
