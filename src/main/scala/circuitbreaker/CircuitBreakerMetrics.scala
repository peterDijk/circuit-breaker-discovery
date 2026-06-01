package circuitbreaker

import io.circe.{Encoder, Json}
import io.circe.syntax.*
import circuitbreaker.model.State

import java.time.Instant

case class Metrics(
  state: State,
  failureCount: Int,
  failureThreshold: Int,
  successCount: Int,
  totalFailures: Int,
  lastStateChange: Instant,
  lastFailure: Option[Instant],
  config: CircuitBreakerConfig
)

object Metrics:
  given Encoder[Metrics] = Encoder.instance: m =>
    Json.obj(
      "state" -> m.state.display.asJson,
      "failureCount" -> m.failureCount.asJson,
      "failureThreshold" -> m.failureThreshold.asJson,
      "successCount" -> m.successCount.asJson,
      "totalFailures" -> m.totalFailures.asJson,
      "lastStateChange" -> m.lastStateChange.toString.asJson,
      "lastFailure" -> m.lastFailure.map(_.toString).asJson,
      "config" -> Json.obj(
        "failureThreshold" -> m.config.failureThreshold.asJson,
        "resetTimeoutSeconds" -> m.config.resetTimeout.toSeconds.asJson,
        "callTimeoutSeconds" -> m.config.callTimeout.toSeconds.asJson
      )
    )
