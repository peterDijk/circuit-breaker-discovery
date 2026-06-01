package circuitbreaker

import cats.effect.{IO, IOApp}
import org.http4s.ember.client.EmberClientBuilder
import circuitbreaker.cli.Repl
import circuitbreaker.weather.WeatherClient
import circuitbreaker.metrics.MetricsServer

object Main extends IOApp.Simple:

  def run: IO[Unit] =
    val config = CircuitBreakerConfig.fromEnv
    val withMetrics = sys.env.get("CB_WITH_METRICS").contains("true") ||
      sys.props.get("cb.withMetrics").contains("true")

    EmberClientBuilder.default[IO].build.use: client =>
      for
        cb <- CircuitBreaker.make(config)
        weatherClient = WeatherClient(client)
        repl = Repl(cb, weatherClient, config)
        _ <- if withMetrics then
          MetricsServer.run(cb, config).background.use: _ =>
            IO.println("Metrics server started on http://localhost:8080") >>
              repl.run
        else
          repl.run
      yield ()
