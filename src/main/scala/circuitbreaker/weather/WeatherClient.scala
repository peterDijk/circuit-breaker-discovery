package circuitbreaker.weather

import cats.effect.IO
import cats.syntax.all.*
import org.http4s.client.Client
import org.http4s.{Request, Uri}
import org.http4s.Method.GET
import io.circe.parser.parse
import circuitbreaker.model.WeatherResponse

class WeatherClient(client: Client[IO]):

  private val geocodeBaseUri = "https://geocoding-api.open-meteo.com/v1/search"
  private val weatherBaseUri = "https://api.open-meteo.com/v1/forecast"

  def fetch(city: String): IO[WeatherResponse] =
    geocode(city).flatMap: coords =>
      fetchWeather(coords._1, coords._2, city)

  private def geocode(city: String): IO[(Double, Double)] =
    val uri = Uri
      .unsafeFromString(geocodeBaseUri)
      .withQueryParam("name", city)
      .withQueryParam("count", "1")
      .withQueryParam("language", "en")
      .withQueryParam("format", "json")

    client.expect[String](Request[IO](GET, uri)).flatMap: body =>
      IO.fromEither(
        parse(body).flatMap: json =>
          val cursor = json.hcursor
          cursor.downField("results").downArray.as[io.circe.Json].flatMap: firstResult =>
            for
              lat <- firstResult.hcursor.downField("latitude").as[Double]
              lon <- firstResult.hcursor.downField("longitude").as[Double]
            yield (lat, lon)
      ).adaptError { case _ => new RuntimeException(s"City not found: $city") }

  private def fetchWeather(lat: Double, lon: Double, city: String): IO[WeatherResponse] =
    val uri = Uri
      .unsafeFromString(weatherBaseUri)
      .withQueryParam("latitude", lat.toString)
      .withQueryParam("longitude", lon.toString)
      .withQueryParam("current", "temperature_2m,weather_code,wind_speed_10m")

    client.expect[String](Request[IO](GET, uri)).flatMap: body =>
      IO.fromEither(
        parse(body).left.map(e => io.circe.DecodingFailure(e.message, Nil)).flatMap: json =>
          WeatherResponse.fromApiResponse(json, city)
      ).adaptError { case e => new RuntimeException(s"Failed to parse weather data: ${e.getMessage}") }
