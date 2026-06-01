package circuitbreaker.model

import io.circe.{Encoder, Decoder}

enum State:
  case Closed
  case Open
  case HalfOpen

  def display: String = this match
    case Closed   => "CLOSED"
    case Open     => "OPEN"
    case HalfOpen => "HALF-OPEN"

object State:
  given Encoder[State] = Encoder.encodeString.contramap(_.display)
  given Decoder[State] = Decoder.decodeString.emap {
    case "CLOSED"     => Right(Closed)
    case "OPEN"       => Right(Open)
    case "HALF-OPEN"  => Right(HalfOpen)
    case other        => Left(s"Invalid state: $other")
  }
