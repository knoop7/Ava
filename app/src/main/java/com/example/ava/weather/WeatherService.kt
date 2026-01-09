package com.example.ava.weather

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.util.regex.Pattern

data class WeatherData(
    val temperature: Int = 0,
    val condition: String = "sunny",  
    val humidity: Int = 0,
    val windSpeed: Float = 0f,
    val windDirection: String = "",   
    val windDirectionEn: String = "", 
    val visibility: Float = 0f,
    val aqi: Int = 0,
    val pm25: Int = 0,
    val city: String = ""
)


object WeatherService {
    private const val TAG = "WeatherService"
    
    
    private const val WEATHER_DOMAIN = "weather.com.cn"
    private const val HTTP_REFERER = "http://m.weather.com.cn/"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    
    
    private const val BAIDU_MAP_AK = "nSxiPohfziUaCuONe4ViUP2N"
    
    
    private var cachedWeather: WeatherData? = null
    private var lastFetchTime: Long = 0
    private var lastAreaId: String = ""
    private const val CACHE_DURATION = 10 * 60 * 1000L  
    
    
    private val areaIdCache = mutableMapOf<String, String>()
    
    suspend fun getWeather(city: String): WeatherData {
        val now = System.currentTimeMillis()
        
        
        val pureCity = if (city.contains("】")) {
            city.substringAfter("】")
        } else {
            city
        }
        
        
        val areaId = getAreaId(pureCity)
        if (areaId.isEmpty()) {
            Log.e(TAG, "No area_id for $pureCity")
            return cachedWeather ?: WeatherData(city = pureCity)
        }
        
        
        if (cachedWeather != null && lastAreaId == areaId && 
            now - lastFetchTime < CACHE_DURATION) {
            return cachedWeather!!
        }
        
        return try {
            val weather = fetchWeatherFromChinaWeather(areaId, pureCity)
            cachedWeather = weather
            lastFetchTime = now
            lastAreaId = areaId
            weather
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather: ${e.message}")
            cachedWeather ?: WeatherData(city = pureCity)
        }
    }
    
    
    private suspend fun fetchWeatherFromChinaWeather(areaId: String, city: String): WeatherData = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val url = "http://d1.$WEATHER_DOMAIN/weather_index/$areaId.html?_=$timestamp"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Referer", HTTP_REFERER)
            connection.setRequestProperty("User-Agent", USER_AGENT)
            
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.e(TAG, "Weather API error: $responseCode")
                return@withContext WeatherData(city = city)
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            
            
            val dataSKPattern = Pattern.compile("dataSK\\s*=\\s*(\\{.*?\\})\\s*;", Pattern.DOTALL)
            val dataSKMatcher = dataSKPattern.matcher(response)
            
            if (dataSKMatcher.find()) {
                val dataSKJson = dataSKMatcher.group(1)
                val dataSK = JSONObject(dataSKJson)
                
                val temp = dataSK.optString("temp", "0").toDoubleOrNull()?.toInt() ?: 0
                val humidity = dataSK.optString("sd", "0%").replace("%", "").toIntOrNull() ?: 0
                val windSpeed = dataSK.optString("wse", "0").replace("km/h", "").replace("<", "").trim().toFloatOrNull() ?: 0f
                val windDir = dataSK.optString("WD", "")  
                val windDirEn = dataSK.optString("wde", "")  
                val visibility = dataSK.optString("njd", "0km").replace("km", "").trim().toFloatOrNull() ?: 0f
                val aqi = dataSK.optString("aqi", "0").toIntOrNull() ?: 0
                val pm25 = dataSK.optString("aqi_pm25", "0").toIntOrNull() ?: 0
                val weatherCode = dataSK.optString("weathercode", "d00")
                
                val condition = convertWeatherCode(weatherCode)
                
                return@withContext WeatherData(
                    temperature = temp,
                    condition = condition,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    windDirection = windDir,
                    windDirectionEn = windDirEn,
                    visibility = visibility,
                    aqi = aqi,
                    pm25 = pm25,
                    city = city
                )
            }
            
            Log.e(TAG, "Failed to parse weather data from response")
            WeatherData(city = city)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather: ${e.message}", e)
            WeatherData(city = city)
        }
    }
    
    
    private suspend fun getAreaId(city: String): String = withContext(Dispatchers.IO) {
        
        areaIdCache[city]?.let { return@withContext it }
        
        
        val coords = getCoordinatesFromBaidu(city)
        if (coords == null) {
            Log.e(TAG, "Failed to get coordinates for $city")
            return@withContext ""
        }
        
        
        try {
            val url = "http://d7.$WEATHER_DOMAIN/geong/v1/api?params={\"method\":\"stationinfo\",\"lat\":${coords.second},\"lng\":${coords.first}}"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Referer", HTTP_REFERER)
            connection.setRequestProperty("User-Agent", USER_AGENT)
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.optJSONObject("data")
                val station = data?.optJSONObject("station")
                val areaId = station?.optString("areaid", "") ?: ""
                if (areaId.isNotEmpty()) {
                    areaIdCache[city] = areaId
                    return@withContext areaId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get area_id from coordinates: ${e.message}")
        }
        
        
        try {
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val searchUrl = "http://toy1.$WEATHER_DOMAIN/search?cityname=$encodedCity"
            
            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Referer", HTTP_REFERER)
            connection.setRequestProperty("User-Agent", USER_AGENT)
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val cleanResponse = response.trim().removePrefix("(").removeSuffix(")")
                if (cleanResponse.startsWith("[")) {
                    val jsonArray = org.json.JSONArray(cleanResponse)
                    if (jsonArray.length() > 0) {
                        val ref = jsonArray.getJSONObject(0).optString("ref", "")
                        val areaId = ref.split("~").firstOrNull() ?: ""
                        if (areaId.length in 9..12) {
                            areaIdCache[city] = areaId
                            return@withContext areaId
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback search failed: ${e.message}")
        }
        
        Log.w(TAG, "Could not find area_id for $city")
        ""
    }
    
    
    private suspend fun getCoordinatesFromBaidu(city: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val url = "http://api.map.baidu.com/place/v2/search?query=$encodedCity&region=全国&output=json&ak=$BAIDU_MAP_AK"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optInt("status") == 0) {
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val location = results.getJSONObject(0).optJSONObject("location")
                        if (location != null) {
                            val lng = location.optDouble("lng")
                            val lat = location.optDouble("lat")
                            return@withContext Pair(lng, lat)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get coordinates from Baidu: ${e.message}")
        }
        null
    }
    
    
    private fun convertWeatherCode(code: String): String {
        val numCode = code.replace("d", "").replace("n", "")
        return when (numCode) {
            "00" -> "sunny"           
            "01" -> "cloudy"          
            "02" -> "cloudy"          
            "03" -> "light_rain"      
            "04" -> "light_rain"      
            "05" -> "light_rain"      
            "06" -> "light_rain"      
            "07" -> "light_rain"      
            "08" -> "rainy"           
            "09" -> "heavy_rain"      
            "10" -> "heavy_rain"      
            "11" -> "heavy_rain"      
            "12" -> "heavy_rain"      
            "13" -> "light_snow"      
            "14" -> "light_snow"      
            "15" -> "snowy"           
            "16" -> "heavy_snow"      
            "17" -> "heavy_snow"      
            "18" -> "fog"             
            "19" -> "light_rain"      
            "20" -> "sandstorm"       
            "21" -> "light_rain"      
            "22" -> "rainy"           
            "23" -> "heavy_rain"      
            "24" -> "heavy_rain"      
            "25" -> "heavy_rain"      
            "26" -> "light_snow"      
            "27" -> "snowy"           
            "28" -> "heavy_snow"      
            "29" -> "sandstorm"       
            "30" -> "sandstorm"       
            "31" -> "sandstorm"       
            "32" -> "fog"             
            "49" -> "fog"             
            "53" -> "haze"            
            "54" -> "haze"            
            "55" -> "haze"            
            "56" -> "haze"            
            "57" -> "fog"             
            "58" -> "fog"             
            "301" -> "light_rain"     
            "302" -> "snowy"          
            else -> "sunny"
        }
    }
    
    
    private fun isChineseLocale(): Boolean {
        val locale = java.util.Locale.getDefault()
        val language = locale.language.lowercase()
        val country = locale.country.uppercase()
        val timezone = java.util.TimeZone.getDefault().id
        return language.startsWith("zh") || 
            country in listOf("CN", "TW", "HK", "MO") ||
            timezone.startsWith("Asia/Shanghai") || 
            timezone.startsWith("Asia/Chongqing") ||
            timezone.startsWith("Asia/Hong_Kong") ||
            timezone.startsWith("Asia/Taipei")
    }
    
    fun getWindDirectionText(direction: Int): String {
        val isCN = isChineseLocale()
        return when {
            direction < 22.5 || direction >= 337.5 -> if (isCN) "北风" else "N"
            direction < 67.5 -> if (isCN) "东北风" else "NE"
            direction < 112.5 -> if (isCN) "东风" else "E"
            direction < 157.5 -> if (isCN) "东南风" else "SE"
            direction < 202.5 -> if (isCN) "南风" else "S"
            direction < 247.5 -> if (isCN) "西南风" else "SW"
            direction < 292.5 -> if (isCN) "西风" else "W"
            else -> if (isCN) "西北风" else "NW"
        }
    }
    
    
    fun getWindLevel(speed: Float): String {
        val isCN = isChineseLocale()
        return when {
            speed < 1 -> if (isCN) "0级" else "0"
            speed < 6 -> if (isCN) "1-2级" else "1-2"
            speed < 12 -> if (isCN) "3级" else "3"
            speed < 20 -> if (isCN) "4级" else "4"
            speed < 29 -> if (isCN) "5级" else "5"
            speed < 39 -> if (isCN) "6级" else "6"
            speed < 50 -> if (isCN) "7级" else "7"
            speed < 62 -> if (isCN) "8级" else "8"
            else -> if (isCN) "9级以上" else "9+"
        }
    }
    
    fun clearCache() {
        cachedWeather = null
        lastFetchTime = 0
        lastAreaId = ""
    }
}
