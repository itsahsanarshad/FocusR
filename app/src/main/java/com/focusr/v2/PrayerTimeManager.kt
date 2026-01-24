package com.focusr.v2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.focusr.v2.models.Prayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages Islamic prayer times using the Aladhan API.
 * Caches prayer times daily to minimize API calls.
 */
class PrayerTimeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PrayerTimeManager"
        private const val PREFS_NAME = "prayer_times_prefs"
        private const val KEY_CACHED_DATE = "cached_date"
        private const val KEY_PRAYER_TIMES = "prayer_times_json"
        private const val KEY_CITY = "prayer_city"
        private const val KEY_COUNTRY = "prayer_country"
        
        // Aladhan API
        private const val API_BASE = "https://api.aladhan.com/v1/timingsByCity"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Cached prayer times for today (prayer -> time in minutes from midnight)
    private var cachedTimes: Map<Prayer, Int>? = null
    private var cachedDate: String? = null
    
    // In-memory cache for current session
    private var inMemoryTimes: Map<Prayer, PrayerTime>? = null
    
    data class PrayerTime(
        val prayer: Prayer,
        val hour: Int,
        val minute: Int
    ) {
        val timeInMinutes: Int get() = hour * 60 + minute
        
        fun formatTime(): String {
            val period = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%d:%02d %s", displayHour, minute, period)
        }
    }
    
    /**
     * Fetches prayer times for today. Uses cache if available.
     * @param school 0 = Shafi (standard), 1 = Hanafi
     */
    suspend fun getPrayerTimes(city: String, country: String, method: Int = 1, school: Int = 0): Map<Prayer, PrayerTime>? {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
        
        // Check cache
        val cachedCity = prefs.getString(KEY_CITY, null)
        val cachedCountry = prefs.getString(KEY_COUNTRY, null)
        val cachedDateStr = prefs.getString(KEY_CACHED_DATE, null)
        
        if (cachedCity == city && cachedCountry == country && cachedDateStr == today) {
            val cachedJson = prefs.getString(KEY_PRAYER_TIMES, null)
            if (cachedJson != null) {
                Log.d(TAG, "Using cached prayer times for $today")
                val times = parseCachedTimes(cachedJson)
                times?.let { 
                    inMemoryTimes = it
                    logPrayerTimes(it)
                }
                return times
            }
        }
        
        // Fetch from API
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$API_BASE?city=$city&country=$country&method=$method&school=$school")
                Log.d(TAG, "Fetching prayer times from: $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    
                    if (json.getInt("code") == 200) {
                        val timings = json.getJSONObject("data").getJSONObject("timings")
                        val times = parsePrayerTimes(timings)
                        
                        // Cache the results
                        cachePrayerTimes(city, country, today, timings.toString())
                        inMemoryTimes = times
                        
                        Log.d(TAG, "Prayer times fetched successfully")
                        logPrayerTimes(times)
                        times
                    } else {
                        Log.e(TAG, "API error: ${json.getString("status")}")
                        null
                    }
                } else {
                    Log.e(TAG, "HTTP error: ${connection.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching prayer times: ${e.message}")
                null
            }
        }
    }
    
    private fun parsePrayerTimes(timings: JSONObject): Map<Prayer, PrayerTime> {
        val times = mutableMapOf<Prayer, PrayerTime>()
        
        Prayer.values().forEach { prayer ->
            val apiName = when (prayer) {
                Prayer.FAJR -> "Fajr"
                Prayer.DHUHR -> "Dhuhr"
                Prayer.ASR -> "Asr"
                Prayer.MAGHRIB -> "Maghrib"
                Prayer.ISHA -> "Isha"
            }
            
            val timeStr = timings.optString(apiName, null) ?: return@forEach
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].trim().toIntOrNull() ?: return@forEach
                val minute = parts[1].trim().take(2).toIntOrNull() ?: return@forEach
                times[prayer] = PrayerTime(prayer, hour, minute)
            }
        }
        
        return times
    }
    
    private fun cachePrayerTimes(city: String, country: String, date: String, json: String) {
        prefs.edit()
            .putString(KEY_CITY, city)
            .putString(KEY_COUNTRY, country)
            .putString(KEY_CACHED_DATE, date)
            .putString(KEY_PRAYER_TIMES, json)
            .apply()
    }
    
    private fun parseCachedTimes(json: String): Map<Prayer, PrayerTime>? {
        return try {
            val timings = JSONObject(json)
            parsePrayerTimes(timings)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cached times: ${e.message}")
            null
        }
    }
    
    /**
     * Gets the current prayer based on time of day.
     * Returns the prayer whose time has most recently passed.
     */
    fun getCurrentPrayer(times: Map<Prayer, PrayerTime>): Prayer? {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        // Sort prayers by time
        val sortedPrayers = times.entries.sortedBy { it.value.timeInMinutes }
        
        // Find the most recent prayer that has started
        var currentPrayer: Prayer? = null
        for ((prayer, time) in sortedPrayers) {
            if (currentMinutes >= time.timeInMinutes) {
                currentPrayer = prayer
            }
        }
        
        // If before Fajr, we're still in Isha from previous day
        if (currentPrayer == null && times.containsKey(Prayer.ISHA)) {
            currentPrayer = Prayer.ISHA
        }
        
        return currentPrayer
    }
    
    /**
     * Gets the next prayer after the current time.
     */
    fun getNextPrayer(times: Map<Prayer, PrayerTime>): Prayer? {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        // Sort prayers by time
        val sortedPrayers = times.entries.sortedBy { it.value.timeInMinutes }
        
        // Find the next upcoming prayer
        for ((prayer, time) in sortedPrayers) {
            if (time.timeInMinutes > currentMinutes) {
                return prayer
            }
        }
        
        // If after Isha, next prayer is Fajr (tomorrow)
        return Prayer.FAJR
    }
    
    /**
     * Checks if currently in a prayer window (from prayer time until next prayer).
     */
    fun isInPrayerWindow(
        times: Map<Prayer, PrayerTime>,
        enabledPrayers: Set<Prayer>
    ): Pair<Boolean, Prayer?> {
        val currentPrayer = getCurrentPrayer(times)
        
        if (currentPrayer != null && enabledPrayers.contains(currentPrayer)) {
            return Pair(true, currentPrayer)
        }
        
        return Pair(false, null)
    }
    
    /**
     * Gets the time (in millis) when the current prayer started.
     */
    fun getPrayerStartTime(times: Map<Prayer, PrayerTime>, prayer: Prayer): Long? {
        val prayerTime = times[prayer] ?: return null
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, prayerTime.hour)
        calendar.set(Calendar.MINUTE, prayerTime.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis
    }
    
    /**
     * Gets time until next prayer in minutes.
     */
    fun getMinutesUntilNextPrayer(times: Map<Prayer, PrayerTime>): Int? {
        val nextPrayer = getNextPrayer(times) ?: return null
        val nextTime = times[nextPrayer] ?: return null
        
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        return if (nextTime.timeInMinutes > currentMinutes) {
            nextTime.timeInMinutes - currentMinutes
        } else {
            // Next prayer is tomorrow (Fajr)
            (24 * 60 - currentMinutes) + nextTime.timeInMinutes
        }
    }
    
    // ========== Convenience methods using in-memory cache ==========
    
    /**
     * Gets current prayer using cached times. Returns null if no times cached.
     */
    fun getCurrentPrayer(): Prayer? {
        val times = inMemoryTimes ?: return null
        return getCurrentPrayer(times)
    }
    
    /**
     * Gets next prayer using cached times.
     */
    fun getNextPrayer(): Prayer? {
        val times = inMemoryTimes ?: return null
        return getNextPrayer(times)
    }
    
    /**
     * Gets prayer start time in millis using cached times.
     */
    fun getPrayerStartTime(prayer: Prayer): Long? {
        val times = inMemoryTimes ?: return null
        return getPrayerStartTime(times, prayer)
    }
    
    /**
     * Checks if in prayer window using cached times.
     */
    fun isInPrayerWindow(enabledPrayers: Set<Prayer>): Pair<Boolean, Prayer?> {
        val times = inMemoryTimes ?: return Pair(false, null)
        return isInPrayerWindow(times, enabledPrayers)
    }
    
    /**
     * Check if times are cached and available.
     */
    fun hasCachedTimes(): Boolean = inMemoryTimes != null
    
    /**
     * Log prayer times for verification.
     */
    private fun logPrayerTimes(times: Map<Prayer, PrayerTime>) {
        Log.i(TAG, "========== PRAYER TIMES ==========")
        times.entries.sortedBy { it.value.timeInMinutes }.forEach { (prayer, time) ->
            Log.i(TAG, "${prayer.emoji} ${prayer.displayName}: ${time.formatTime()}")
        }
        Log.i(TAG, "===================================")
    }
    
    /**
     * Get formatted prayer times for display (public API).
     */
    fun getFormattedPrayerTimes(): String? {
        val times = inMemoryTimes ?: return null
        return times.entries
            .sortedBy { it.value.timeInMinutes }
            .joinToString("\n") { (prayer, time) ->
                "${prayer.emoji} ${prayer.displayName}: ${time.formatTime()}"
            }
    }
}
