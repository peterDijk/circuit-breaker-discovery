package circuitbreaker.model

import io.circe.{Json, Decoder, DecodingFailure, HCursor}

case class WeatherResponse(
  temperature: Double,
  weatherCode: Int,
  windSpeed: Double,
  location: String
) {
  override def toString: String = s"${temperature}°C, Wind: ${windSpeed} km/h"
}

object WeatherResponse {
  def fromApiResponse(json: Json, location: String): Either[DecodingFailure, WeatherResponse] = {
    val cursor = json.hcursor
    for {
      current <- cursor.downField("current").as[Json]
      currentCursor = current.hcursor
      temperature <- currentCursor.downField("temperature_2m").as[Double]
      weatherCode <- currentCursor.downField("weather_code").as[Int]
      windSpeed <- currentCursor.downField("wind_speed_10m").as[Double]
    } yield WeatherResponse(temperature, weatherCode, windSpeed, location)
  }

  implicit val decoder: Decoder[WeatherResponse] = (c: HCursor) => {
    for {
      temperature <- c.downField("current").downField("temperature_2m").as[Double]
      weatherCode <- c.downField("current").downField("weather_code").as[Int]
      windSpeed <- c.downField("current").downField("wind_speed_10m").as[Double]
    } yield WeatherResponse(temperature, weatherCode, windSpeed, "")
  }
}
