# Metrics HTTP Endpoint Contract

An optional lightweight HTTP endpoint exposing circuit breaker metrics as JSON.

## Endpoint

### `GET /status`

Returns current circuit breaker state and metrics.

**Response 200 OK**:
```json
{
  "state": "closed",
  "failureCount": 2,
  "failureThreshold": 5,
  "successCount": 38,
  "totalFailures": 7,
  "lastStateChange": "2026-06-01T14:23:01Z",
  "lastFailure": "2026-06-01T14:22:58Z",
  "config": {
    "failureThreshold": 5,
    "resetTimeoutSeconds": 30,
    "callTimeoutSeconds": 10
  }
}
```

### `GET /history`

Returns recent state transitions.

**Response 200 OK**:
```json
{
  "transitions": [
    {
      "from": "closed",
      "to": "open",
      "timestamp": "2026-06-01T14:23:01Z",
      "reason": "5 consecutive failures"
    },
    {
      "from": "open",
      "to": "halfOpen",
      "timestamp": "2026-06-01T14:23:32Z",
      "reason": "reset timeout elapsed"
    }
  ]
}
```

## Notes

- Server binds to `localhost:8080` by default
- This endpoint is optional (P3 priority) and serves observability purposes
- No authentication required (local development tool)
