package circuitbreaker.model

import io.circe.Encoder
import io.circe.syntax.*
import io.circe.Json
import java.time.Instant

case class StateTransition(
  from: State,
  to: State,
  timestamp: Instant,
  reason: String
)

object StateTransition {
  given Encoder[StateTransition] = Encoder.instance { transition =>
    Json.obj(
      "from" -> transition.from.display.asJson,
      "to" -> transition.to.display.asJson,
      "timestamp" -> transition.timestamp.toString.asJson,
      "reason" -> transition.reason.asJson
    )
  }
}
