package circuitbreaker

import scala.concurrent.duration.*

case class CircuitBreakerConfig(
  failureThreshold: Int = 5,
  resetTimeout: FiniteDuration = 30.seconds,
  callTimeout: FiniteDuration = 10.seconds
)

object CircuitBreakerConfig:
  val default: CircuitBreakerConfig = CircuitBreakerConfig()

  def fromEnv: CircuitBreakerConfig =
    val failureThreshold = sys.env
      .get("CB_FAILURE_THRESHOLD")
      .flatMap(_.toIntOption)
      .getOrElse(5)

    val resetTimeout = sys.env
      .get("CB_RESET_TIMEOUT_SECONDS")
      .flatMap(_.toIntOption)
      .map(_.seconds)
      .getOrElse(30.seconds)

    val callTimeout = sys.env
      .get("CB_CALL_TIMEOUT_SECONDS")
      .flatMap(_.toIntOption)
      .map(_.seconds)
      .getOrElse(10.seconds)

    CircuitBreakerConfig(
      failureThreshold = failureThreshold,
      resetTimeout = resetTimeout,
      callTimeout = callTimeout
    )
