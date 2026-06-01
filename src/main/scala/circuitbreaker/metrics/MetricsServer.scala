package circuitbreaker.metrics

import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import com.comcast.ip4s.*
import io.circe.syntax.*
import io.circe.Json
import org.http4s.circe.*
import circuitbreaker.{CircuitBreaker, CircuitBreakerConfig, Metrics}
import circuitbreaker.model.StateTransition

object MetricsServer:

  def routes(cb: CircuitBreaker, config: CircuitBreakerConfig): HttpRoutes[IO] =
    HttpRoutes.of[IO]:
      case GET -> Root / "status" =>
        for
          state <- cb.getState
          metrics = Metrics(
            state = state.status,
            failureCount = state.failureCount,
            failureThreshold = config.failureThreshold,
            successCount = state.successCount,
            totalFailures = state.totalFailures,
            lastStateChange = state.lastStateChange,
            lastFailure = state.lastFailure,
            config = config
          )
          resp <- Ok(metrics.asJson)
        yield resp

      case GET -> Root / "history" =>
        for
          history <- cb.getHistory
          json = Json.obj("transitions" -> history.reverse.asJson)
          resp <- Ok(json)
        yield resp

  def run(cb: CircuitBreaker, config: CircuitBreakerConfig): IO[Nothing] =
    val app = Router("/" -> routes(cb, config)).orNotFound
    EmberServerBuilder
      .default[IO]
      .withHost(host"localhost")
      .withPort(port"8080")
      .withHttpApp(app)
      .build
      .useForever
