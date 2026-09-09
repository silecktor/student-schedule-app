package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingLocation>?
)

@JsonClass(generateAdapter = true)
data class GeocodingLocation(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null
)

@JsonClass(generateAdapter = true)
data class WeatherForecastResponse(
    val daily: DailyWeatherData?
)

@JsonClass(generateAdapter = true)
data class DailyWeatherData(
    val time: List<String>?,
    @Json(name = "weathercode") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val tempMax: List<Double>?,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>?,
    @Json(name = "precipitation_probability_max") val rainProbability: List<Int>?
)

data class WeatherInfo(
    val cityName: String,
    val tempMax: Double,
    val tempMin: Double,
    val rainProb: Int,
    val description: String,
    val isRainExpected: Boolean
)

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") cityName: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "ru",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto"
    ): WeatherForecastResponse
}

object WeatherRepository {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val geocodingApi = geocodingRetrofit.create(GeocodingApi::class.java)
    private val weatherApi = weatherRetrofit.create(WeatherApi::class.java)

    suspend fun getTomorrowWeather(cityName: String): Result<WeatherInfo> {
        return try {
            val geoRes = geocodingApi.searchCity(cityName)
            val location = geoRes.results?.firstOrNull()
                ?: return Result.failure(Exception("Город '$cityName' не найден"))

            val forecastRes = weatherApi.getDailyForecast(location.latitude, location.longitude)
            val daily = forecastRes.daily
                ?: return Result.failure(Exception("Нет данных о погоде"))

            // Index 1 is tomorrow
            val idx = if ((daily.time?.size ?: 0) > 1) 1 else 0
            val code = daily.weatherCode?.getOrNull(idx) ?: 0
            val maxT = daily.tempMax?.getOrNull(idx) ?: 20.0
            val minT = daily.tempMin?.getOrNull(idx) ?: 10.0
            val rainProb = daily.rainProbability?.getOrNull(idx) ?: 0

            val isRain = rainProb >= 40 || code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
            val desc = getWeatherDescription(code)

            Result.success(
                WeatherInfo(
                    cityName = location.name,
                    tempMax = maxT,
                    tempMin = minT,
                    rainProb = rainProb,
                    description = desc,
                    isRainExpected = isRain
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Ясно"
            1, 2, 3 -> "Переменная облачность"
            45, 48 -> "Туман"
            51, 53, 55 -> "Небольшой дождь"
            61, 63, 65 -> "Дождь"
            71, 73, 75 -> "Снег"
            80, 81, 82 -> "Ливень"
            95, 96, 99 -> "Гроза"
            else -> "Облачно"
        }
    }
}
