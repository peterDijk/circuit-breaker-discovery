package circuitbreaker.model

import scala.concurrent.duration.FiniteDuration
import io.circe.{Encoder, Json}

case class FallbackResponse(
  message: String,
  state: State,
  retryIn: FiniteDuration
)

object FallbackResponse {
  def serviceUnavailable(retryIn: FiniteDuration): FallbackResponse =
    FallbackResponse(
      message = "Service temporarily unavailable",
      state = State.Open,
      retryIn = retryIn
    )

  implicit val encoder: Encoder[FallbackResponse] = (response: FallbackResponse) =>
    Json.obj(
      "message" -> Json.fromString(response.message),
      "state" -> Json.fromString(response.state.display),
      "retryInSeconds" -> Json.fromLong(response.retryIn.toSeconds)
    )
}
