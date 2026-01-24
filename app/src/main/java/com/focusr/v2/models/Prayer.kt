package com.focusr.v2.models

/**
 * Islamic prayer times
 */
enum class Prayer(
    val displayName: String,
    val arabicName: String,
    val emoji: String,
    val overlayMessage: String,
    val overlaySubtitle: String
) {
    FAJR(
        displayName = "Fajr",
        arabicName = "الفجر",
        emoji = "🌅",
        overlayMessage = "Rise for Fajr",
        overlaySubtitle = "The dawn prayer - Start your day with Allah"
    ),
    DHUHR(
        displayName = "Dhuhr",
        arabicName = "الظهر",
        emoji = "☀️",
        overlayMessage = "Time for Dhuhr",
        overlaySubtitle = "The midday prayer - Pause and reconnect"
    ),
    ASR(
        displayName = "Asr",
        arabicName = "العصر",
        emoji = "🌤️",
        overlayMessage = "Asr Awaits",
        overlaySubtitle = "The afternoon prayer - Seek tranquility"
    ),
    MAGHRIB(
        displayName = "Maghrib",
        arabicName = "المغرب",
        emoji = "🌅",
        overlayMessage = "Maghrib Time",
        overlaySubtitle = "The sunset prayer - Be grateful for the day"
    ),
    ISHA(
        displayName = "Isha",
        arabicName = "العشاء",
        emoji = "🌙",
        overlayMessage = "Time for Isha",
        overlaySubtitle = "The night prayer - End your day in peace"
    );
    
    companion object {
        fun fromApiName(name: String): Prayer? {
            return when (name.lowercase()) {
                "fajr" -> FAJR
                "dhuhr", "zuhr" -> DHUHR
                "asr" -> ASR
                "maghrib" -> MAGHRIB
                "isha" -> ISHA
                else -> null
            }
        }
    }
}
