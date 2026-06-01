package circuitbreaker.cli

import cats.effect.IO
import cats.effect.std.Console
import cats.syntax.all.*
import circuitbreaker.{CircuitBreaker, CircuitBreakerConfig}
import circuitbreaker.model.*
import circuitbreaker.weather.WeatherClient

import java.time.{Duration, Instant, ZoneId}
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*

class Repl(
  cb: CircuitBreaker,
  weatherClient: WeatherClient,
  config: CircuitBreakerConfig
):
  private val console = Console[IO]
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

  def run: IO[Unit] =
    console.println("=== Circuit Breaker Discovery ===") >>
      console.println("Type 'help' for available commands.\n") >>
      loop

  private def loop: IO[Unit] =
    console.print("circuit-breaker> ") >>
      console.readLine.flatMap:
        case line if line.trim.isEmpty => loop
        case line =>
          val parts = line.trim.split("\\s+", 2).toList
          val command = parts.head.toLowerCase
          val args = parts.drop(1).headOption.getOrElse("")
          handleCommand(command, args).flatMap:
            case true  => loop
            case false => console.println("Goodbye!")

  private def handleCommand(command: String, args: String): IO[Boolean] =
    command match
      case "weather" if args.nonEmpty => weatherCommand(args).as(true)
      case "weather"                  => console.println("Usage: weather <city>").as(true)
      case "status"                   => statusCommand.as(true)
      case "config"                   => configCommand.as(true)
      case "history"                  => historyCommand.as(true)
      case "help"                     => helpCommand.as(true)
      case "quit" | "exit"            => IO.pure(false)
      case other                      => console.println(s"Unknown command: $other. Type 'help' for available commands.").as(true)

  private def weatherCommand(city: String): IO[Unit] =
    for
      stateBefore <- cb.getState
      _ <- if stateBefore.status == State.HalfOpen then
        console.println(s"[HALF-OPEN] Attempting recovery...")
      else IO.unit
      result <- cb.protect(weatherClient.fetch(city))
      stateAfter <- cb.getState
      _ <- result match
        case Right(weather) =>
          val stateDisplay = stateAfter.status.display
          console.println(s"[$stateDisplay] Weather for $city: $weather") >>
            console.println(s"         Failures: ${stateAfter.failureCount}/${config.failureThreshold} | Successes: ${stateAfter.successCount}")
        case Left(fallback) if stateAfter.status == State.Open =>
          val retryStr = if fallback.retryIn.toSeconds > 0 then s"Retry in ${fallback.retryIn.toSeconds}s." else ""
          console.println(s"[OPEN] ${fallback.message}. $retryStr") >>
            console.println(s"       Failures: ${stateAfter.totalFailures} total | Last failure: ${formatAgo(stateAfter.lastFailure)}")
        case Left(fallback) =>
          val remaining = config.failureThreshold - stateAfter.failureCount
          console.println(s"[${stateAfter.status.display}] ${fallback.message}") >>
            console.println(s"         Failures: ${stateAfter.failureCount}/${config.failureThreshold} | $remaining more before circuit opens")
      history <- cb.getHistory
      _ <- history.headOption match
        case Some(t) if t.timestamp.isAfter(stateBefore.lastStateChange) =>
          console.println(s"  >> State transition: ${t.from.display} → ${t.to.display} (${t.reason})")
        case _ => IO.unit
    yield ()

  private def statusCommand: IO[Unit] =
    for
      state <- cb.getState
      history <- cb.getHistory
      lastTransition = history.headOption.map(t => s"${t.from.display} → ${t.to.display}").getOrElse("none")
      _ <- console.println("Circuit Breaker Status:")
      _ <- console.println(s"  State:          ${state.status.display}")
      _ <- console.println(s"  Failure Count:  ${state.failureCount}/${config.failureThreshold}")
      _ <- console.println(s"  Success Count:  ${state.successCount}")
      _ <- console.println(s"  Total Failures: ${state.totalFailures}")
      _ <- console.println(s"  Last Change:    ${timeFormatter.format(state.lastStateChange)} ($lastTransition)")
    yield ()

  private def configCommand: IO[Unit] =
    console.println("Configuration:") >>
      console.println(s"  Failure Threshold: ${config.failureThreshold}") >>
      console.println(s"  Reset Timeout:     ${config.resetTimeout.toSeconds}s") >>
      console.println(s"  Call Timeout:      ${config.callTimeout.toSeconds}s")

  private def historyCommand: IO[Unit] =
    for
      history <- cb.getHistory
      _ <- if history.isEmpty then
        console.println("No state transitions recorded yet.")
      else
        console.println("State Transitions:") >>
          history.reverse.traverse_(t =>
            console.println(s"  ${timeFormatter.format(t.timestamp)} ${t.from.display} → ${t.to.display} (${t.reason})")
          )
    yield ()

  private def helpCommand: IO[Unit] =
    console.println("Available commands:") >>
      console.println("  weather <city>  - Fetch weather through the circuit breaker") >>
      console.println("  status          - Show circuit breaker state and metrics") >>
      console.println("  config          - Show current configuration") >>
      console.println("  history         - Show state transition history") >>
      console.println("  help            - Show this help message") >>
      console.println("  quit            - Exit the application")

  private def formatAgo(instant: Option[Instant]): String =
    instant.map: i =>
      val ago = Duration.between(i, Instant.now()).toSeconds
      s"${ago}s ago"
    .getOrElse("never")
