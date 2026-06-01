package circuitbreaker

import cats.effect.{IO, Ref, Clock}
import cats.effect.std.Console
import cats.syntax.all.*
import circuitbreaker.model.*

import java.time.Instant
import scala.concurrent.duration.*

case class CircuitBreakerState(
  status: State,
  failureCount: Int,
  successCount: Int,
  totalFailures: Int,
  lastStateChange: Instant,
  lastFailure: Option[Instant],
  openedAt: Option[Instant]
)

object CircuitBreaker:

  def make(config: CircuitBreakerConfig): IO[CircuitBreaker] =
    for
      now <- Clock[IO].realTimeInstant
      initialState = CircuitBreakerState(
        status = State.Closed,
        failureCount = 0,
        successCount = 0,
        totalFailures = 0,
        lastStateChange = now,
        lastFailure = None,
        openedAt = None
      )
      stateRef <- Ref.of[IO, CircuitBreakerState](initialState)
      historyRef <- Ref.of[IO, List[StateTransition]](List.empty)
    yield CircuitBreaker(config, stateRef, historyRef)

class CircuitBreaker(
  val config: CircuitBreakerConfig,
  stateRef: Ref[IO, CircuitBreakerState],
  historyRef: Ref[IO, List[StateTransition]]
):

  def protect[A](call: IO[A]): IO[Either[FallbackResponse, A]] =
    for
      now <- Clock[IO].realTimeInstant
      state <- stateRef.get
      result <- state.status match
        case State.Closed =>
          attemptCall(call, now)
        case State.Open =>
          handleOpen(state, now, call)
        case State.HalfOpen =>
          attemptTrialCall(call, now)
    yield result

  private def attemptCall[A](call: IO[A], now: Instant): IO[Either[FallbackResponse, A]] =
    call.timeout(config.callTimeout).attempt.flatMap:
      case Right(value) =>
        onSuccess(now).as(Right(value))
      case Left(error) =>
        onFailure(now, error.getMessage).map(Left(_))

  private def handleOpen[A](
    state: CircuitBreakerState,
    now: Instant,
    call: IO[A]
  ): IO[Either[FallbackResponse, A]] =
    val elapsed = java.time.Duration.between(state.openedAt.getOrElse(now), now)
    if elapsed.toMillis >= config.resetTimeout.toMillis then
      transition(now, State.HalfOpen, "reset timeout elapsed") >>
        attemptTrialCall(call, now)
    else
      val remaining = config.resetTimeout - elapsed.toMillis.millis
      IO.pure(Left(FallbackResponse.serviceUnavailable(remaining)))

  private def attemptTrialCall[A](call: IO[A], now: Instant): IO[Either[FallbackResponse, A]] =
    call.timeout(config.callTimeout).attempt.flatMap:
      case Right(value) =>
        transition(now, State.Closed, "trial request succeeded") >>
          onSuccess(now).as(Right(value))
      case Left(error) =>
        transition(now, State.Open, s"trial request failed: ${error.getMessage}") >>
          stateRef.update(s => s.copy(openedAt = Some(now))) >>
          IO.pure(Left(FallbackResponse.serviceUnavailable(config.resetTimeout)))

  private def onSuccess(now: Instant): IO[Unit] =
    stateRef.update: s =>
      s.copy(
        failureCount = 0,
        successCount = s.successCount + 1
      )

  private def onFailure(now: Instant, reason: String): IO[FallbackResponse] =
    stateRef.modify: s =>
      val newFailureCount = s.failureCount + 1
      val newState =
        if newFailureCount >= config.failureThreshold then
          s.copy(
            status = State.Open,
            failureCount = newFailureCount,
            totalFailures = s.totalFailures + 1,
            lastFailure = Some(now),
            openedAt = Some(now)
          )
        else
          s.copy(
            failureCount = newFailureCount,
            totalFailures = s.totalFailures + 1,
            lastFailure = Some(now)
          )
      val shouldTransition = newFailureCount >= config.failureThreshold
      (newState, (shouldTransition, newFailureCount))
    .flatMap: (shouldTransition, count) =>
      if shouldTransition then
        transition(now, State.Open, s"$count consecutive failures") >>
          IO.pure(FallbackResponse.serviceUnavailable(config.resetTimeout))
      else
        val remaining = config.failureThreshold - count
        IO.pure(FallbackResponse(
          s"Request failed: $reason",
          State.Closed,
          0.seconds
        ))

  private def transition(now: Instant, to: State, reason: String): IO[Unit] =
    stateRef.modify: s =>
      val from = s.status
      val newState = s.copy(status = to, lastStateChange = now, failureCount = 0)
      (newState, StateTransition(from, to, now, reason))
    .flatMap: t =>
      historyRef.update: history =>
        (t :: history).take(20)

  def getState: IO[CircuitBreakerState] = stateRef.get

  def getHistory: IO[List[StateTransition]] = historyRef.get
