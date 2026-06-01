# Data Model: Circuit Breaker Weather Service

## Core Entities

### CircuitBreakerState (ADT)

The circuit breaker's current operational state.

| State    | Description                                    | Transitions To        |
|----------|------------------------------------------------|-----------------------|
| Closed   | Normal operation; requests pass through        | Open                  |
| Open     | Failing; requests rejected immediately         | HalfOpen              |
| HalfOpen | Testing recovery; one trial request allowed    | Closed, Open          |

### CircuitBreakerConfig

Static configuration controlling circuit breaker behavior.

| Field            | Type     | Default | Description                                    |
|------------------|----------|---------|------------------------------------------------|
| failureThreshold | Int      | 5       | Consecutive failures before opening circuit    |
| resetTimeout     | Duration | 30s     | Time in Open state before transitioning to HalfOpen |
| callTimeout      | Duration | 10s     | Max time to wait for external service response |

### CircuitBreakerMetrics

Runtime counters and status information.

| Field            | Type         | Description                                    |
|------------------|--------------|------------------------------------------------|
| state            | State        | Current state (Closed/Open/HalfOpen)           |
| failureCount     | Int          | Consecutive failures in current Closed period  |
| successCount     | Int          | Total successful calls since start             |
| totalFailures    | Int          | Total failed calls since start                 |
| lastStateChange  | Timestamp    | When the last state transition occurred        |
| lastFailure      | Timestamp?   | When the most recent failure occurred          |

### StateTransition

A recorded state change event.

| Field     | Type      | Description                          |
|-----------|-----------|--------------------------------------|
| from      | State     | Previous state                       |
| to        | State     | New state                            |
| timestamp | Timestamp | When the transition occurred         |
| reason    | String    | Human-readable trigger description   |

### WeatherResponse

Data returned from the external weather service.

| Field       | Type   | Description                     |
|-------------|--------|---------------------------------|
| temperature | Double | Current temperature in Celsius  |
| weatherCode | Int    | WMO weather condition code      |
| windSpeed   | Double | Wind speed in km/h              |
| location    | String | Requested location name         |

### FallbackResponse

Response returned when circuit is open.

| Field   | Type   | Description                           |
|---------|--------|---------------------------------------|
| message | String | "Service temporarily unavailable"     |
| state   | State  | Current circuit breaker state         |
| retryIn | Duration | Time until circuit may attempt recovery |

## State Transition Diagram

```
                    failure count >= threshold
    ┌────────┐  ─────────────────────────────►  ┌────────┐
    │ CLOSED │                                   │  OPEN  │
    └────────┘  ◄───────────────────────────────  └────────┘
         ▲        trial request succeeds              │
         │                                            │
         │         ┌───────────┐                      │
         └─────────│ HALF-OPEN │◄─────────────────────┘
       success     └───────────┘   reset timeout elapsed
                        │
                        │ trial request fails
                        ▼
                   ┌────────┐
                   │  OPEN  │ (reset timeout restarts)
                   └────────┘
```

## Validation Rules

- failureThreshold must be > 0
- resetTimeout must be > 0 seconds
- callTimeout must be > 0 seconds and < resetTimeout
- failureCount resets to 0 when circuit transitions from any state to Closed
- Only one trial request passes through in HalfOpen; others receive fallback
